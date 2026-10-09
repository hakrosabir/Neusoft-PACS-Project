<template>
  <div class="user-management">

    <!-- Top bar: role tabs + search + add button -->
    <div class="um-topbar">
      <div class="role-tabs">
        <button
          v-for="r in roleOptions" :key="r.key"
          :class="['role-tab', { active: userViewMode === r.key }]"
          @click="userViewMode = r.key; searchQuery = ''">
          <span>{{ r.icon }}</span>
          <span>{{ r.label }}</span>
          <span class="role-count">{{ roleCount(r.key) }}</span>
        </button>
      </div>
      <div class="um-right">
        <div class="um-search-wrap">
          <span class="search-icon">🔍</span>
          <input
            v-model="searchQuery" type="text"
            :placeholder="`Search ${currentRoleLabel.toLowerCase()}s...`"
            class="search-input">
        </div>
        <button class="add-user-btn" @click="openAddUser">➕ Add User</button>
      </div>
    </div>

    <!-- Pending doctor approval banner -->
    <div v-if="userViewMode === 'DOCTOR' && pendingCount > 0" class="pending-banner">
      ⚠️ {{ pendingCount }} doctor{{ pendingCount > 1 ? 's' : '' }} awaiting license verification
    </div>

    <!-- User cards grid -->
    <div class="cards-grid">
      <div
        v-for="u in filteredUsers" :key="u.id"
        class="user-card" :class="{ locked: !u.enabled }">

        <!-- Card header -->
        <div class="uc-header">
          <div class="uc-avatar" :class="u.role.toLowerCase()">
            {{ u.username.charAt(0).toUpperCase() }}
          </div>
          <div class="uc-info">
            <h4>{{ u.username }}</h4>
            <span class="uc-id">ID #{{ u.id }}</span>
          </div>
          <div class="uc-badges">
            <span :class="['badge-status', u.enabled ? 'active' : 'locked']">
              {{ u.enabled ? '● Active' : '● Locked' }}
            </span>
            <span v-if="u.role === 'DOCTOR'"
              :class="['badge-verify', u.verified ? 'verified' : 'pending']">
              {{ u.verified ? '✔ Verified' : '⏳ Pending' }}
            </span>
          </div>
        </div>

        <!-- Card details -->
        <div class="uc-details">
          <div class="uc-row"><span class="uc-label">Email</span><span class="uc-val">{{ u.email || '—' }}</span></div>
          <div class="uc-row"><span class="uc-label">Phone</span><span class="uc-val">{{ u.phoneNumber || '—' }}</span></div>
          <template v-if="u.role === 'DOCTOR'">
            <div class="uc-row"><span class="uc-label">License</span><span class="uc-val highlight">{{ u.licenseNumber || '—' }}</span></div>
            <div class="uc-row"><span class="uc-label">Specialty</span><span class="uc-val">{{ u.specialization || '—' }}</span></div>
          </template>
          <template v-if="u.role === 'PATIENT'">
            <div class="uc-row"><span class="uc-label">Insurance ID</span><span class="uc-val highlight">{{ u.insuranceId || '—' }}</span></div>
          </template>
          <template v-if="u.role === 'TECHNICIAN' || u.role === 'NURSE'">
            <div class="uc-row"><span class="uc-label">Role Type</span><span class="uc-val">Nurse / Technician</span></div>
          </template>
          <div class="uc-row"><span class="uc-label">Joined</span><span class="uc-val">{{ formatDate(u.createdAt) }}</span></div>
        </div>

        <!-- Card action buttons -->
        <div class="uc-actions">
          <button v-if="u.role === 'DOCTOR' && !u.verified"
            @click="$emit('approve', u.id)" class="ua-btn approve">✔ Approve</button>
          <button @click="openEdit(u)" class="ua-btn edit">✏️ Edit</button>
          <button @click="$emit('toggle-lock', u)" :class="['ua-btn', u.enabled ? 'lock' : 'unlock']">
            {{ u.enabled ? '🔒 Lock' : '🔓 Unlock' }}
          </button>
          <button @click="openDelete(u)" class="ua-btn delete">🗑 Delete</button>
        </div>

      </div>

      <!-- Empty state -->
      <div v-if="filteredUsers.length === 0" class="empty-state">
        <div class="empty-icon">👤</div>
        <p>No {{ currentRoleLabel.toLowerCase() }}s found{{ searchQuery ? ` matching "${searchQuery}"` : '' }}.</p>
      </div>
    </div>

    <!-- ═══════════ EDIT USER MODAL ═══════════ -->
    <div v-if="editModal.show" class="modal-overlay" @click.self="closeEdit">
      <div class="modal-box">
        <div class="modal-header">
          <h3>✏️ Edit — <span class="modal-username">{{ editModal.user.username }}</span></h3>
          <button class="modal-close" @click="closeEdit">✕</button>
        </div>
        <div class="modal-body">
          <div class="modal-row">
            <label>Username</label>
            <input v-model="editModal.user.username" class="modal-input" placeholder="Username">
          </div>
          <div class="modal-row">
            <label>Email</label>
            <input v-model="editModal.user.email" class="modal-input" type="email" placeholder="Email address">
          </div>
          <div class="modal-row">
            <label>Phone Number</label>
            <input v-model="editModal.user.phoneNumber" class="modal-input" placeholder="Phone number">
          </div>
          <template v-if="editModal.user.role === 'DOCTOR'">
            <div class="modal-row">
              <label>Specialization</label>
              <input v-model="editModal.user.specialization" class="modal-input" placeholder="e.g. Radiology">
            </div>
            <div class="modal-row">
              <label>License Number</label>
              <input v-model="editModal.user.licenseNumber" class="modal-input" placeholder="Medical license">
            </div>
          </template>
          <template v-if="editModal.user.role === 'PATIENT'">
            <div class="modal-row">
              <label>Insurance ID</label>
              <input v-model="editModal.user.insuranceId" class="modal-input" placeholder="Insurance ID">
            </div>
          </template>
          <template v-if="editModal.user.role === 'TECHNICIAN' || editModal.user.role === 'NURSE'">
            <div class="modal-note">🔬 Nurse / Technician — edit email, phone and account status as needed.</div>
          </template>
          <div class="modal-row toggle-row">
            <label>Account Status</label>
            <button :class="['toggle-btn', editModal.user.enabled ? 'on' : 'off']"
              @click="editModal.user.enabled = !editModal.user.enabled">
              {{ editModal.user.enabled ? '● Active' : '● Locked' }}
            </button>
          </div>
        </div>
        <div class="modal-footer">
          <button class="mf-btn cancel" @click="closeEdit">Cancel</button>
          <button class="mf-btn save" @click="submitEdit" :disabled="editModal.saving">
            {{ editModal.saving ? 'Saving...' : '💾 Save Changes' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ═══════════ ADD USER MODAL ═══════════ -->
    <div v-if="addModal.show" class="modal-overlay" @click.self="closeAddUser">
      <div class="modal-box">
        <div class="modal-header">
          <h3>➕ Add New User</h3>
          <button class="modal-close" @click="closeAddUser">✕</button>
        </div>
        <div class="modal-body">

          <!-- Role selector -->
          <div class="modal-row">
            <label>Role</label>
            <div class="role-selector">
              <button
                v-for="r in roleOptions" :key="r.key"
                :class="['role-sel-btn', { active: addModal.user.role === r.key }]"
                @click="addModal.user.role = r.key">
                {{ r.icon }} {{ r.label }}
              </button>
            </div>
          </div>

          <div class="modal-row">
            <label>Username <span class="required">*</span></label>
            <input v-model="addModal.user.username" class="modal-input" placeholder="Enter username">
          </div>
          <div class="modal-row">
            <label>Password <span class="required">*</span></label>
            <input v-model="addModal.user.password" class="modal-input" type="password" placeholder="Enter password">
          </div>
          <div class="modal-row">
            <label>Email</label>
            <input v-model="addModal.user.email" class="modal-input" type="email" placeholder="Email address">
          </div>
          <div class="modal-row">
            <label>Phone Number</label>
            <input v-model="addModal.user.phoneNumber" class="modal-input" placeholder="Phone number">
          </div>

          <template v-if="addModal.user.role === 'DOCTOR'">
            <div class="modal-row">
              <label>Specialization</label>
              <input v-model="addModal.user.specialization" class="modal-input" placeholder="e.g. Radiology">
            </div>
            <div class="modal-row">
              <label>License Number</label>
              <input v-model="addModal.user.licenseNumber" class="modal-input" placeholder="Medical license number">
            </div>
            <div class="modal-note warn">⚠️ Doctor will be set to Pending by default. Use Approve after creation.</div>
          </template>

          <template v-if="addModal.user.role === 'PATIENT'">
            <div class="modal-row">
              <label>Insurance ID</label>
              <input v-model="addModal.user.insuranceId" class="modal-input" placeholder="Insurance ID">
            </div>
          </template>

          <template v-if="addModal.user.role === 'TECHNICIAN'">
            <div class="modal-note">🔬 This user will be registered as a Nurse / Technician with standard access.</div>
          </template>

          <div v-if="addModal.error" class="modal-error">⚠ {{ addModal.error }}</div>

        </div>
        <div class="modal-footer">
          <button class="mf-btn cancel" @click="closeAddUser">Cancel</button>
          <button class="mf-btn save" @click="submitAddUser" :disabled="addModal.saving">
            {{ addModal.saving ? 'Creating...' : '➕ Create User' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ═══════════ DELETE CONFIRM MODAL ═══════════ -->
    <div v-if="deleteModal.show" class="modal-overlay" @click.self="deleteModal.show = false">
      <div class="modal-box confirm-box">
        <div class="modal-header danger">
          <h3>🗑 Delete User</h3>
          <button class="modal-close" @click="deleteModal.show = false">✕</button>
        </div>
        <div class="modal-body">
          <p class="confirm-text">
            Are you sure you want to <strong>permanently delete</strong>
            <span class="modal-username">{{ deleteModal.user?.username }}</span>?
          </p>
          <p class="confirm-warn">⚠️ This action cannot be undone.</p>
        </div>
        <div class="modal-footer">
          <button class="mf-btn cancel" @click="deleteModal.show = false">Cancel</button>
          <button class="mf-btn danger" @click="submitDelete" :disabled="deleteModal.deleting">
            {{ deleteModal.deleting ? 'Deleting...' : '🗑 Confirm Delete' }}
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { JAVA_API_URL } from '../config.js'
import { ref, computed, reactive } from 'vue'
import axios from 'axios'

// ── Props & Emits ──
const props = defineProps({
  users: { type: Array, default: () => [] }
})

// Emits let AdminDashboard.vue know to refresh data after any action
const emit = defineEmits(['approve', 'toggle-lock', 'refresh'])

const API_BASE  = `${JAVA_API_URL}/admin`
const AUTH_BASE = `${JAVA_API_URL}/auth`

// ── Role tab definitions ──
const roleOptions = [
  { key: 'DOCTOR',     label: 'Doctor',           icon: '👨‍⚕️' },
  { key: 'PATIENT',    label: 'Patient',          icon: '🏥'  },
  { key: 'TECHNICIAN', label: 'Nurse/Technician', icon: '🔬'  },
]

// ── Local state ──
const userViewMode = ref('DOCTOR')
const searchQuery  = ref('')

// ── Computed ──
const currentRoleLabel = computed(() =>
  roleOptions.find(r => r.key === userViewMode.value)?.label || userViewMode.value
)

// Counts NURSE + TECHNICIAN together for the tab badge
const roleCount = (role) => {
  if (role === 'TECHNICIAN')
    return props.users.filter(u => u.role === 'TECHNICIAN' || u.role === 'NURSE').length
  return props.users.filter(u => u.role === role).length
}

const pendingCount = computed(() =>
  props.users.filter(u => u.role === 'DOCTOR' && !u.verified).length
)

// Filter users by active tab + search query
const filteredUsers = computed(() =>
  props.users.filter(u => {
    const effectiveRole = u.role === 'NURSE' ? 'TECHNICIAN' : u.role
    const tabMatch = effectiveRole === userViewMode.value
    const searchMatch =
      u.username.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      (u.email || '').toLowerCase().includes(searchQuery.value.toLowerCase())
    return tabMatch && searchMatch
  })
)

// ── Formatters ──
function formatDate(dt) { return dt ? new Date(dt).toLocaleDateString() : '—' }

// ── EDIT MODAL ──
const editModal = reactive({ show: false, saving: false, user: {} })

function openEdit(user) {
  editModal.user  = { ...user }
  editModal.show  = true
  editModal.saving = false
}
function closeEdit() { editModal.show = false; editModal.user = {} }

async function submitEdit() {
  editModal.saving = true
  try {
    await axios.put(`${API_BASE}/users/${editModal.user.id}`, editModal.user)
    emit('refresh')
    closeEdit()
  } catch {
    alert('Failed to save. Ensure PUT /admin/users/{id} exists in AdminController.')
  } finally {
    editModal.saving = false
  }
}

// ── ADD USER MODAL ──
const emptyAddUser = () => ({
  role: 'DOCTOR', username: '', password: '',
  email: '', phoneNumber: '', specialization: '', licenseNumber: '', insuranceId: ''
})
const addModal = reactive({ show: false, saving: false, error: '', user: emptyAddUser() })

function openAddUser()  { addModal.user = emptyAddUser(); addModal.error = ''; addModal.show = true }
function closeAddUser() { addModal.show = false; addModal.error = '' }

async function submitAddUser() {
  if (!addModal.user.username.trim()) { addModal.error = 'Username is required.'; return }
  if (!addModal.user.password.trim()) { addModal.error = 'Password is required.'; return }
  addModal.error  = ''
  addModal.saving = true
  try {
    // Reuses the existing /auth/register endpoint — no new backend needed
    await axios.post(`${AUTH_BASE}/register`, addModal.user)
    emit('refresh')
    closeAddUser()
  } catch (e) {
    addModal.error = e?.response?.data?.msg || 'Failed to create user. Username may already exist.'
  } finally {
    addModal.saving = false
  }
}

// ── DELETE MODAL ──
const deleteModal = reactive({ show: false, deleting: false, user: null })

function openDelete(user) { deleteModal.user = user; deleteModal.show = true; deleteModal.deleting = false }

async function submitDelete() {
  deleteModal.deleting = true
  try {
    await axios.delete(`${API_BASE}/users/${deleteModal.user.id}`)
    emit('refresh')
    deleteModal.show = false
  } catch {
    alert('Failed to delete. Ensure DELETE /admin/users/{id} exists in AdminController.')
  } finally {
    deleteModal.deleting = false
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400&display=swap');

/* ── Top bar ── */
.um-topbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; gap: 16px; flex-wrap: wrap; }
.role-tabs  { display: flex; gap: 8px; flex-wrap: wrap; }
.role-tab   { display: flex; align-items: center; gap: 8px; padding: 10px 18px; background: #1e293b; border: 1px solid #334155; border-radius: 10px; color: #94a3b8; cursor: pointer; font-weight: 600; font-size: 0.85rem; transition: all 0.2s; }
.role-tab:hover  { border-color: #475569; color: white; }
.role-tab.active { background: #1d3a5f; border-color: #3b82f6; color: #60a5fa; }
.role-count { background: #0f172a; border-radius: 20px; padding: 2px 8px; font-size: 0.72rem; font-weight: 700; color: #64748b; }
.role-tab.active .role-count { background: rgba(59,130,246,0.2); color: #60a5fa; }

.um-right       { display: flex; align-items: center; gap: 12px; }
.um-search-wrap { position: relative; }
.search-icon    { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); font-size: 0.85rem; }
.search-input   { background: #1e293b; border: 1px solid #334155; color: white; padding: 10px 10px 10px 36px; border-radius: 8px; width: 260px; outline: none; font-size: 0.9rem; }
.search-input:focus { border-color: #3b82f6; }

.add-user-btn { padding: 10px 20px; background: linear-gradient(135deg, #1d4ed8, #3b82f6); border: none; border-radius: 10px; color: white; font-weight: 700; font-size: 0.9rem; cursor: pointer; transition: all 0.2s; white-space: nowrap; box-shadow: 0 4px 12px rgba(59,130,246,0.3); }
.add-user-btn:hover { background: linear-gradient(135deg, #1e40af, #2563eb); transform: translateY(-1px); }

/* Pending banner */
.pending-banner { background: rgba(234,179,8,0.1); border: 1px solid rgba(234,179,8,0.3); color: #facc15; padding: 10px 16px; border-radius: 8px; font-size: 0.85rem; font-weight: 600; margin-bottom: 16px; }

/* Cards grid */
.cards-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 20px; }

.user-card { background: #1e293b; border: 1px solid #334155; border-radius: 14px; padding: 20px; display: flex; flex-direction: column; gap: 14px; transition: all 0.2s; }
.user-card:hover  { transform: translateY(-2px); border-color: #475569; box-shadow: 0 8px 25px rgba(0,0,0,0.3); }
.user-card.locked { border-color: rgba(239,68,68,0.3); opacity: 0.85; }

/* Card header */
.uc-header  { display: flex; align-items: center; gap: 12px; }
.uc-avatar  { width: 46px; height: 46px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 1.2rem; color: white; flex-shrink: 0; }
.uc-avatar.doctor      { background: linear-gradient(135deg, #1d4ed8, #3b82f6); }
.uc-avatar.patient     { background: linear-gradient(135deg, #065f46, #10b981); }
.uc-avatar.nurse,
.uc-avatar.technician  { background: linear-gradient(135deg, #0e7490, #14b8a6); }
.uc-info    { flex: 1; }
.uc-info h4 { margin: 0 0 2px; color: white; font-size: 1rem; }
.uc-id      { font-size: 0.72rem; color: #64748b; font-family: 'JetBrains Mono', monospace; }

.uc-badges      { display: flex; flex-direction: column; gap: 4px; align-items: flex-end; }
.badge-status   { font-size: 0.68rem; font-weight: 700; padding: 3px 8px; border-radius: 20px; }
.badge-status.active { background: rgba(34,197,94,0.15); color: #4ade80; border: 1px solid rgba(34,197,94,0.3); }
.badge-status.locked { background: rgba(239,68,68,0.15); color: #f87171; border: 1px solid rgba(239,68,68,0.3); }
.badge-verify         { font-size: 0.68rem; font-weight: 700; padding: 3px 8px; border-radius: 20px; }
.badge-verify.verified{ background: rgba(56,189,248,0.15); color: #38bdf8; border: 1px solid rgba(56,189,248,0.3); }
.badge-verify.pending { background: rgba(234,179,8,0.15);  color: #facc15; border: 1px solid rgba(234,179,8,0.3); }

/* Card details */
.uc-details { display: flex; flex-direction: column; gap: 6px; background: #0f172a; border-radius: 8px; padding: 12px; border: 1px solid #1e293b; }
.uc-row     { display: flex; justify-content: space-between; align-items: center; font-size: 0.82rem; }
.uc-label   { color: #64748b; font-weight: 600; }
.uc-val     { color: #cbd5e1; text-align: right; max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.uc-val.highlight { color: #60a5fa; font-family: 'JetBrains Mono', monospace; font-size: 0.78rem; }

/* Card actions */
.uc-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.ua-btn     { flex: 1; min-width: 70px; padding: 8px 10px; border-radius: 8px; border: none; cursor: pointer; font-weight: 600; font-size: 0.78rem; transition: all 0.15s; white-space: nowrap; }
.ua-btn.approve { background: rgba(34,197,94,0.15); color: #4ade80; border: 1px solid rgba(34,197,94,0.3); }
.ua-btn.approve:hover { background: rgba(34,197,94,0.3); }
.ua-btn.edit    { background: rgba(59,130,246,0.15); color: #60a5fa; border: 1px solid rgba(59,130,246,0.3); }
.ua-btn.edit:hover    { background: rgba(59,130,246,0.3); }
.ua-btn.lock    { background: rgba(239,68,68,0.15);  color: #f87171; border: 1px solid rgba(239,68,68,0.3); }
.ua-btn.lock:hover    { background: rgba(239,68,68,0.3); }
.ua-btn.unlock  { background: rgba(234,179,8,0.15);  color: #facc15; border: 1px solid rgba(234,179,8,0.3); }
.ua-btn.unlock:hover  { background: rgba(234,179,8,0.3); }
.ua-btn.delete  { background: rgba(239,68,68,0.1);   color: #f87171; border: 1px solid rgba(239,68,68,0.2); }
.ua-btn.delete:hover  { background: rgba(239,68,68,0.25); }

/* Empty state */
.empty-state { grid-column: 1 / -1; text-align: center; padding: 60px 20px; color: #475569; }
.empty-icon  { font-size: 3rem; margin-bottom: 12px; }

/* ── Shared modal styles ── */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 1000; backdrop-filter: blur(4px); }
.modal-box     { background: #1e293b; border: 1px solid #334155; border-radius: 16px; width: 480px; max-width: 95vw; box-shadow: 0 25px 60px rgba(0,0,0,0.6); }
.confirm-box   { width: 420px; }
.modal-header  { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid #334155; }
.modal-header h3    { margin: 0; font-size: 1.2rem; color: white; font-weight: 700; }
.modal-header.danger h3 { color: #f87171; }
.modal-username { color: #60a5fa; }
.modal-close   { background: none; border: none; color: #64748b; cursor: pointer; font-size: 1.1rem; padding: 4px 8px; border-radius: 4px; }
.modal-close:hover { background: #334155; color: white; }
.modal-body    { padding: 24px; display: flex; flex-direction: column; gap: 16px; }
.modal-row     { display: flex; flex-direction: column; gap: 6px; }
.modal-row label { font-size: 0.78rem; color: #94a3b8; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
.modal-input   { background: #0f172a; border: 1px solid #334155; color: white; padding: 10px 14px; border-radius: 8px; font-size: 0.9rem; outline: none; transition: 0.2s; }
.modal-input:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.15); }
.modal-note    { background: rgba(20,184,166,0.1); border: 1px solid rgba(20,184,166,0.3); color: #14b8a6; padding: 10px 14px; border-radius: 8px; font-size: 0.82rem; }
.modal-note.warn { background: rgba(234,179,8,0.1); border-color: rgba(234,179,8,0.3); color: #facc15; }
.modal-error   { background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.3); color: #f87171; padding: 10px 14px; border-radius: 8px; font-size: 0.82rem; font-weight: 600; }
.toggle-row    { flex-direction: row; justify-content: space-between; align-items: center; }
.toggle-btn    { padding: 8px 20px; border-radius: 20px; border: none; cursor: pointer; font-weight: 700; font-size: 0.82rem; transition: 0.2s; }
.toggle-btn.on { background: rgba(34,197,94,0.2); color: #4ade80; border: 1px solid rgba(34,197,94,0.4); }
.toggle-btn.off{ background: rgba(239,68,68,0.2); color: #f87171; border: 1px solid rgba(239,68,68,0.4); }
.modal-footer  { display: flex; gap: 12px; padding: 16px 24px; border-top: 1px solid #334155; justify-content: flex-end; }
.mf-btn        { padding: 10px 24px; border-radius: 8px; border: none; cursor: pointer; font-weight: 600; font-size: 0.9rem; transition: 0.2s; }
.mf-btn.cancel { background: #334155; color: #94a3b8; }
.mf-btn.cancel:hover { background: #475569; color: white; }
.mf-btn.save   { background: #3b82f6; color: white; }
.mf-btn.save:hover:not(:disabled) { background: #2563eb; }
.mf-btn.danger { background: #ef4444; color: white; }
.mf-btn.danger:hover:not(:disabled) { background: #dc2626; }
.mf-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.confirm-text  { color: #e2e8f0; font-size: 0.95rem; line-height: 1.6; margin: 0; }
.confirm-warn  { color: #facc15; font-size: 0.82rem; margin: 0; }
.required      { color: #f87171; }

/* Role selector in Add User modal */
.role-selector { display: flex; gap: 8px; flex-wrap: wrap; }
.role-sel-btn  { flex: 1; padding: 10px 8px; background: #0f172a; border: 1px solid #334155; border-radius: 8px; color: #94a3b8; cursor: pointer; font-size: 0.8rem; font-weight: 600; transition: all 0.2s; text-align: center; }
.role-sel-btn:hover  { border-color: #475569; color: white; }
.role-sel-btn.active { background: #1d3a5f; border-color: #3b82f6; color: #60a5fa; }
</style>