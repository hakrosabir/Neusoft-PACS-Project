<template>
  <div class="app-layout">
    
    <aside class="sidebar">
      <div class="brand">
        <div class="logo-circle">N</div>
        <span class="brand-text">MyHealth</span>
      </div>

      <nav class="nav-menu">
        <button 
          v-for="tab in tabs" 
          :key="tab.id"
          @click="currentTab = tab.id"
          :class="['nav-item', { active: currentTab === tab.id }]"
        >
          <span class="icon">{{ tab.icon }}</span>
          <span class="label">{{ tab.name }}</span>
          <span v-if="tab.id === 'reports' && reports.length" class="nav-badge">{{ reports.length }}</span>
        </button>
      </nav>

      <div class="user-profile">
        <div class="avatar">{{ userInitials }}</div>
        <div class="user-info">
          <p class="name">{{ user?.username || 'Patient' }}</p>
          <p class="role">ID: {{ user?.id || 'N/A' }}</p>
        </div>
        <button @click="logout" class="logout-icon" title="Logout">➜</button>
      </div>
    </aside>

    <main class="main-content">
      <header class="top-bar">
        <div>
          <h2 class="page-title">{{ activeTabName }}</h2>
          <p class="date-display">{{ currentDate }}</p>
        </div>
        <div class="header-actions">
          <button @click="openBooking" class="action-btn primary-action">+ Book Appointment</button>
        </div>
      </header>

      <div class="content-container">
        
        <!-- ── DASHBOARD ── -->
        <div v-if="currentTab === 'dashboard'" class="dashboard-view animate-fade">
          <div class="welcome-banner">
            <div class="banner-text">
              <h2>Welcome back, {{ user?.username }}! 👋</h2>
              <p>Track your health journey and access your medical records all in one place.</p>
            </div>
          </div>
          <div class="stats-grid">
            <div class="stat-card" @click="openBooking">
               <div class="stat-icon teal">✚</div>
               <div class="stat-data">
                 <h3>Book Visit</h3>
                 <p>Schedule Now</p>
               </div>
            </div>
            <div class="stat-card" @click="currentTab = 'reports'">
               <div class="stat-icon purple">📄</div>
               <div class="stat-data">
                 <h3>My Reports</h3>
                 <p>{{ reports.length }} Records Available</p>
               </div>
            </div>
            <div class="stat-card" @click="currentTab = 'appointments'">
               <div class="stat-icon blue">📅</div>
               <div class="stat-data">
                 <h3>Appointments</h3>
                 <p>{{ upcomingAppointments.length }} Scheduled</p>
               </div>
            </div>
          </div>

          <!-- Quick recent reports preview on dashboard -->
          <div v-if="reports.length > 0" class="recent-reports-section">
            <div class="section-header">
              <h3>Recent Reports</h3>
              <button class="link-btn" @click="currentTab = 'reports'">View all →</button>
            </div>
            <div class="recent-cards">
              <div
                v-for="rep in reports.slice(0, 3)"
                :key="rep.id"
                class="recent-card"
                @click="openViewReport(rep)"
              >
                <div class="rc-type" :class="'rct-' + normalizeType(rep.analysisType)">
                  {{ typeIcon(rep.analysisType) }}
                </div>
                <div class="rc-info">
                  <div class="rc-label">{{ typeLabel(rep.analysisType) }}</div>
                  <div class="rc-date">{{ formatDate(rep.reportDate) }}</div>
                  <div class="rc-doctor">Dr. {{ rep.doctorName }}</div>
                </div>
                <div class="rc-sev" :class="'rcs-' + (rep.severity || 'routine')">
                  {{ severityDot(rep.severity) }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- ── APPOINTMENTS ── -->
        <div v-if="currentTab === 'appointments'" class="animate-fade">
          <div class="panel">
            <div class="panel-header">
              <h3>My Scheduled Visits</h3>
              <button @click="fetchAppointments" class="refresh-btn">↻ Refresh</button>
            </div>
            <table class="data-table">
              <thead>
                <tr>
                  <th>Date & Time</th>
                  <th>Doctor</th>
                  <th>Department</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="appointments.length === 0">
                    <td colspan="5" class="text-center">No appointments found.</td>
                </tr>
                <tr v-for="apt in appointments" :key="apt.id">
                  <td class="time">
                    <div class="fw-600">{{ apt.appointmentDate }}</div>
                    <small class="text-muted">{{ apt.timeSlot }}</small>
                  </td>
                  <td class="fw-500">Dr. {{ apt.doctorName }}</td>
                  <td>{{ apt.department }}</td>
                  <td>
                    <span :class="['status-badge', (apt.status || 'PENDING').toLowerCase()]">
                      {{ apt.status || 'PENDING' }}
                    </span>
                  </td>
                  <td>
                    <button 
                      v-if="(apt.status || 'PENDING').toUpperCase() === 'PENDING'" 
                      @click="cancelAppointment(apt.id)" 
                      class="cancel-btn"
                    >
                      Cancel
                    </button>
                    <button 
                      v-else-if="apt.status === 'COMPLETED'" 
                      @click="viewReport(apt)" 
                      class="report-btn"
                    >
                      📄 View Report
                    </button>
                    <span v-else class="text-muted">—</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- ── REPORTS ── -->
        <div v-if="currentTab === 'reports'" class="animate-fade">
          <div class="panel">
            <div class="panel-header">
              <h3>My Medical Reports</h3>
              <button @click="fetchReports" class="refresh-btn">↻ Refresh</button>
            </div>

            <div v-if="reportsLoading" class="loading-state">
              <div class="spinner"></div>
              <span>Loading your reports...</span>
            </div>

            <div v-else-if="reports.length === 0" class="empty-state">
              <div class="empty-icon">📋</div>
              <p>No medical reports yet.</p>
              <small>Reports will appear here once your doctor finalizes them.</small>
            </div>

            <table v-else class="data-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Scan Type</th>
                  <th>Doctor</th>
                  <th>Diagnosis Summary</th>
                  <th>Severity</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="rep in reports" :key="rep.id">
                  <td>
                    <div class="fw-600">{{ formatDate(rep.reportDate) }}</div>
                    <small class="text-muted">#{{ rep.id }}</small>
                  </td>
                  <td>
                    <span class="type-badge" :class="'tb-' + normalizeType(rep.analysisType)">
                      {{ typeIcon(rep.analysisType) }} {{ typeLabel(rep.analysisType) }}
                    </span>
                  </td>
                  <td class="fw-500">Dr. {{ rep.doctorName }}</td>
                  <td class="diagnosis-cell">{{ truncate(rep.diagnosis, 65) }}</td>
                  <td>
                    <span class="sev-badge" :class="'svb-' + (rep.severity || 'routine')">
                      {{ severityLabel(rep.severity) }}
                    </span>
                  </td>
                  <td>
                    <button @click="openViewReport(rep)" class="report-btn">
                      👁 View Report
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

      </div>
    </main>

    <!-- ── BOOKING MODAL ── -->
    <div v-if="showBookingModal" class="modal-overlay">
      <div class="modal-card">
        <div class="modal-header">
          <h3>New Appointment Request</h3>
          <button @click="showBookingModal = false" class="close-btn">×</button>
        </div>
        <div class="modal-body scrollable">
          <h4 class="form-section-title">Patient Information</h4>
          <div class="form-group"><label>Full Name</label><input v-model="bookingData.patientName" class="input-field" placeholder="Patient's Full Name"></div>
          <div class="form-row">
            <div class="form-group half"><label>Date of Birth</label><input type="date" v-model="bookingData.dob" class="input-field"></div>
            <div class="form-group half">
              <label>Gender</label>
              <select v-model="bookingData.gender" class="input-field">
                <option value="MALE">Male</option>
                <option value="FEMALE">Female</option>
                <option value="OTHER">Other</option>
              </select>
            </div>
          </div>
          <div class="form-group"><label>Patient ID (DICOM / Insurance)</label><input v-model="bookingData.dicomId" class="input-field" placeholder="Optional (e.g. PAT-12345)"></div>
          <h4 class="form-section-title">Visit Details</h4>
          <div class="form-group">
            <label>Select Doctor</label>
            <select v-model="selectedDoctor" class="input-field">
                <option value="" disabled>-- Choose a Verified Doctor --</option>
                <option v-for="doc in verifiedDoctors" :key="doc.id" :value="doc">{{ doc.username }} - {{ doc.specialization || 'General' }}</option>
            </select>
          </div>
          <div class="form-group" v-if="selectedDoctor"><label>Department</label><input :value="selectedDoctor.specialization || 'General Medicine'" disabled class="input-field bg-gray"></div>
          <div class="form-row">
            <div class="form-group half"><label>Date</label><input type="date" v-model="bookingData.date" class="input-field" :min="todayStr"></div>
            <div class="form-group half">
              <label>Time</label>
              <select v-model="bookingData.time" class="input-field">
                <option>09:00 AM</option><option>10:00 AM</option><option>11:30 AM</option><option>02:00 PM</option><option>04:00 PM</option>
              </select>
            </div>
          </div>
          <div class="form-group"><label>Additional Notes (Optional)</label><textarea v-model="bookingData.notes" class="input-field" rows="2" placeholder="Symptoms, allergies..."></textarea></div>
        </div>
        <div class="modal-footer">
          <button @click="showBookingModal = false" class="secondary-btn">Cancel</button>
          <button @click="confirmBooking" class="primary-btn">Confirm Booking</button>
        </div>
      </div>
    </div>

    <!-- ReportSystem handles its own Teleport so z-index is always correct -->
    <ReportSystem 
      ref="reportSystemRef" 
      :user="user"
      role="patient"
    />

    <PatientChatbot :user="user" :report-system-ref="reportSystemRef" />

  </div>
</template>

<script setup>
import { JAVA_API_URL } from '../config.js'
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import ReportSystem  from '@/components/ReportSystem.vue'
import PatientChatbot from '@/components/PatientChatbot.vue'

const router = useRouter()
const user   = ref({})
const currentTab      = ref('dashboard')
const showBookingModal = ref(false)
const appointments    = ref([])
const reports         = ref([])
const reportsLoading  = ref(false)
const reportSystemRef = ref(null)
const API_BASE = JAVA_API_URL

const tabs = [
  { id: 'dashboard',    name: 'Health Overview',  icon: '🏠' },
  { id: 'appointments', name: 'My Appointments',   icon: '📅' },
  { id: 'reports',      name: 'My Reports',        icon: '📄' },
]

const activeTabName       = computed(() => tabs.find(t => t.id === currentTab.value)?.name || 'Dashboard')
const userInitials        = computed(() => user.value.username ? user.value.username.substring(0,2).toUpperCase() : 'PT')
const currentDate         = new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })
const todayStr            = new Date().toISOString().split('T')[0]
const upcomingAppointments = computed(() => appointments.value.filter(a => a.status !== 'COMPLETED' && a.status !== 'CANCELLED'))

const verifiedDoctors = ref([])
const selectedDoctor  = ref(null)
const bookingData = reactive({
  patientName: '', dob: '', gender: 'MALE', dicomId: '', date: '', time: '09:00 AM', notes: ''
})

// ── Helpers ────────────────────────────────────────────────────────
function normalizeType(t) {
  if (!t) return 'brain'
  const s = String(t).toLowerCase()
  if (s.includes('spleen') || s.includes('ich')) return 'spleen'
  if (s.includes('lung'))  return 'lungs'
  return 'brain'
}
function typeIcon(t)  { return { brain:'🧠', spleen:'🫀', lungs:'🫁' }[normalizeType(t)] || '📋' }
function typeLabel(t) { return { brain:'Brain MRI', spleen:'CT Spleen', lungs:'Lung CT' }[normalizeType(t)] || 'Radiology' }
function truncate(s, n) { return s && s.length > n ? s.slice(0, n) + '…' : (s || '—') }
function formatDate(d) {
  if (!d) return '—'
  try { return new Date(d).toLocaleDateString('en-GB', { day:'2-digit', month:'short', year:'numeric' }) }
  catch { return '—' }
}
function severityLabel(s) {
  const map = { routine: '● Routine', urgent: '▲ Urgent', critical: '■ Critical' }
  return map[(s || 'routine').toLowerCase()] || '● Routine'
}
function severityDot(s) {
  return { routine: '🟢', urgent: '🟡', critical: '🔴' }[(s || 'routine').toLowerCase()] || '🟢'
}

// ── Data fetching ─────────────────────────────────────────────────
async function fetchVerifiedDoctors() {
  try {
    const res = await axios.get(`${API_BASE}/appointments/verified-doctors`)
    verifiedDoctors.value = Array.isArray(res.data) ? res.data : []
  } catch { verifiedDoctors.value = [] }
}

async function fetchAppointments() {
  if (!user.value.id) return
  try {
    const res = await axios.get(`${API_BASE}/appointments/patient/${user.value.id}`)
    appointments.value = Array.isArray(res.data) ? res.data : []
  } catch (e) { console.error('fetchAppointments:', e) }
}

async function fetchReports() {
  if (!user.value.id) return
  reportsLoading.value = true
  try {
    const res = await axios.get(`${API_BASE}/reports/patient/${user.value.id}`)
    reports.value = Array.isArray(res.data)
      ? res.data.sort((a, b) => new Date(b.reportDate || 0) - new Date(a.reportDate || 0))
      : []
  } catch (e) { console.error('fetchReports:', e) }
  finally { reportsLoading.value = false }
}

// ── Booking ───────────────────────────────────────────────────────
function openBooking() {
  bookingData.patientName = user.value.username || ''
  bookingData.date = ''; bookingData.notes = ''
  selectedDoctor.value = null
  fetchVerifiedDoctors()
  showBookingModal.value = true
}

async function confirmBooking() {
  if (!bookingData.date)        return alert('Select a date.')
  if (!selectedDoctor.value)    return alert('Select a doctor.')
  if (!bookingData.patientName) return alert('Enter patient name.')
  const payload = {
    patientId:       user.value.id,
    patientName:     bookingData.patientName,
    doctorId:        selectedDoctor.value.id,
    doctorName:      selectedDoctor.value.username,
    department:      selectedDoctor.value.specialization || 'General',
    appointmentDate: bookingData.date,
    timeSlot:        bookingData.time,
    status:          'PENDING',
    dicomPatientId:  bookingData.dicomId || `PAT-${user.value.id}`,
    patientGender:   bookingData.gender,
    patientBirthDate: bookingData.dob || null,
    metaData:        JSON.stringify({ notes: bookingData.notes }),
  }
  try {
    await axios.post(`${API_BASE}/appointments/book`, payload)
    alert('Appointment Request Sent Successfully!')
    showBookingModal.value = false
    currentTab.value = 'appointments'
    fetchAppointments()
  } catch { alert('Failed to book appointment.') }
}

async function cancelAppointment(id) {
  if (!confirm('Cancel this appointment?')) return
  try {
    await axios.put(`${API_BASE}/appointments/${id}/status`, { status: 'CANCELLED' })
    fetchAppointments()
  } catch { alert('Failed to cancel.') }
}

// ── Report viewing ────────────────────────────────────────────────
// Called from Appointments tab → "View Report" button
async function viewReport(apt) {
  try {
    const res = await axios.get(`${API_BASE}/reports/appointment/${apt.id}`)
    if (res.data) {
      reportSystemRef.value.openViewer(res.data, apt)
    } else {
      alert('Report not available yet. The doctor may still be working on it.')
    }
  } catch (e) {
    console.error(e)
    alert('Error fetching report.')
  }
}

// Called from Reports tab → "View Report" button (already has full report object)
function openViewReport(rep) {
  // Build appointment context from what we have in the report itself
  const contextApt = appointments.value.find(a => a.id === rep.appointmentId) || {
    id:              rep.appointmentId,
    patientId:       rep.patientId,
    patientName:     rep.patientName,
    patientGender:   rep.patientGender,
    patientBirthDate: rep.patientBirthDate,
    dicomPatientId:  rep.dicomPatientId,
    department:      rep.analysisType ? typeLabel(rep.analysisType) : 'Radiology',
    doctorName:      rep.doctorName,
  }
  reportSystemRef.value.openViewer(rep, contextApt)
}

// ── Watchers ──────────────────────────────────────────────────────
watch(currentTab, (newTab) => {
  if (newTab === 'reports')      fetchReports()
  if (newTab === 'appointments') fetchAppointments()
})

function logout() { localStorage.removeItem('user'); router.push('/') }

onMounted(() => {
  const stored = localStorage.getItem('user')
  if (!stored) { router.push('/'); return }
  user.value = JSON.parse(stored)
  fetchAppointments()
  fetchReports()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&display=swap');

.app-layout { display: flex; height: 100vh; background-color: #f0fdfa; font-family: 'DM Sans', sans-serif; }

/* ── Sidebar ── */
.sidebar { width: 260px; background-color: #0d9488; color: white; display: flex; flex-direction: column; padding: 20px; flex-shrink: 0; }
.brand { font-size: 1.2rem; font-weight: bold; margin-bottom: 30px; display: flex; align-items: center; gap: 10px; }
.logo-circle { background: white; color: #0d9488; width: 34px; height: 34px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 800; }
.brand-text { font-weight: 700; }
.nav-menu { display: flex; flex-direction: column; gap: 4px; }
.nav-item {
  padding: 12px 14px; color: #ccfbf1; background: transparent; border: none;
  width: 100%; text-align: left; cursor: pointer; display: flex; align-items: center;
  gap: 10px; border-radius: 8px; transition: all 0.15s; font-size: 0.9rem;
}
.nav-item:hover { background: rgba(255,255,255,.12); }
.nav-item.active { background: white; color: #0f766e; font-weight: 700; }
.nav-badge {
  margin-left: auto; background: rgba(255,255,255,.25); color: white;
  font-size: 0.65rem; font-weight: 700; min-width: 18px; height: 18px;
  border-radius: 99px; display: flex; align-items: center; justify-content: center; padding: 0 5px;
}
.nav-item.active .nav-badge { background: #0d9488; color: white; }

.user-profile { margin-top: auto; display: flex; align-items: center; gap: 10px; padding-top: 18px; border-top: 1px solid rgba(255,255,255,.2); }
.avatar { width: 38px; height: 38px; background: rgba(255,255,255,.2); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 0.85rem; }
.user-info .name { margin: 0; font-weight: 600; font-size: 0.85rem; }
.user-info .role { margin: 2px 0 0; font-size: 0.72rem; opacity: .7; }
.logout-icon { background: none; border: none; color: white; cursor: pointer; font-size: 1.1rem; margin-left: auto; opacity: .7; transition: opacity .15s; }
.logout-icon:hover { opacity: 1; }

/* ── Main ── */
.main-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.top-bar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 18px 28px; background: white;
  border-bottom: 1px solid #e5e7eb; flex-shrink: 0;
}
.page-title { margin: 0; font-size: 1.2rem; font-weight: 700; color: #0f172a; }
.date-display { margin: 2px 0 0; font-size: 0.8rem; color: #94a3b8; }
.primary-action {
  background: #0d9488; color: white; border: none;
  padding: 9px 20px; border-radius: 20px; cursor: pointer; font-weight: 600;
  font-size: 0.85rem; transition: background .15s;
}
.primary-action:hover { background: #0f766e; }

.content-container { flex: 1; padding: 24px 28px; overflow-y: auto; }

/* ── Welcome banner ── */
.welcome-banner {
  background: linear-gradient(135deg, #0d9488, #0891b2);
  border-radius: 14px; padding: 24px 28px; margin-bottom: 24px; color: white;
}
.welcome-banner h2 { margin: 0 0 6px; font-size: 1.3rem; font-weight: 700; }
.welcome-banner p  { margin: 0; opacity: .85; font-size: 0.88rem; }

/* ── Stats grid ── */
.stats-grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 16px; margin-bottom: 28px; }
.stat-card {
  background: white; padding: 18px 20px; border-radius: 12px;
  display: flex; gap: 14px; align-items: center; cursor: pointer;
  transition: transform .2s, box-shadow .2s;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
}
.stat-card:hover { transform: translateY(-3px); box-shadow: 0 6px 18px rgba(0,0,0,.1); }
.stat-icon { width: 48px; height: 48px; background: #ccfbf1; color: #0d9488; display: flex; align-items: center; justify-content: center; border-radius: 12px; font-size: 1.4rem; flex-shrink: 0; }
.stat-icon.purple { background: #ede9fe; color: #7c3aed; }
.stat-icon.blue   { background: #e0f2fe; color: #0284c7; }
.stat-data h3 { margin: 0 0 3px; font-size: 0.95rem; font-weight: 700; color: #0f172a; }
.stat-data p  { margin: 0; font-size: 0.78rem; color: #64748b; }

/* ── Recent reports section ── */
.recent-reports-section { margin-top: 4px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.section-header h3 { margin: 0; font-size: 0.95rem; font-weight: 700; color: #0f172a; }
.link-btn { background: none; border: none; color: #0d9488; cursor: pointer; font-size: 0.82rem; font-weight: 600; }
.recent-cards { display: flex; flex-direction: column; gap: 8px; }
.recent-card {
  background: white; border-radius: 10px; padding: 12px 16px;
  display: flex; align-items: center; gap: 12px; cursor: pointer;
  border: 1px solid #f1f5f9; transition: all .15s;
}
.recent-card:hover { border-color: #0d9488; box-shadow: 0 2px 8px rgba(13,148,136,.1); }
.rc-type { width: 36px; height: 36px; border-radius: 9px; display: flex; align-items: center; justify-content: center; font-size: 1.3rem; flex-shrink: 0; }
.rct-brain  { background: #dbeafe; }
.rct-spleen { background: #fce7f3; }
.rct-lungs  { background: #d1fae5; }
.rc-info { flex: 1; }
.rc-label  { font-size: 0.84rem; font-weight: 700; color: #0f172a; }
.rc-date   { font-size: 0.73rem; color: #94a3b8; margin: 1px 0; }
.rc-doctor { font-size: 0.73rem; color: #64748b; }
.rc-sev { font-size: 1rem; }

/* ── Panel / table ── */
.panel {
  background: white; padding: 20px 22px;
  border-radius: 12px; box-shadow: 0 1px 4px rgba(0,0,0,.06);
}
.panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.panel-header h3 { margin: 0; font-size: 0.95rem; font-weight: 700; color: #0f172a; }
.refresh-btn { background: none; border: 1px solid #e2e8f0; padding: 5px 10px; border-radius: 6px; cursor: pointer; font-size: 0.78rem; color: #64748b; }
.refresh-btn:hover { background: #f8fafc; }

.data-table { width: 100%; border-collapse: collapse; }
.data-table th,
.data-table td { padding: 13px 14px; text-align: left; border-bottom: 1px solid #f1f5f9; vertical-align: middle; font-size: 0.85rem; }
.data-table th { color: #64748b; font-weight: 700; font-size: 0.76rem; text-transform: uppercase; letter-spacing: .4px; background: #fafafa; }
tbody tr:hover { background: #fafffe; }
.text-center { text-align: center; color: #94a3b8; padding: 30px !important; }
.fw-600 { font-weight: 600; }
.fw-500 { font-weight: 500; }
.text-muted { color: #94a3b8; font-size: 0.78rem; }
.diagnosis-cell { color: #374151; font-size: 0.83rem; max-width: 240px; line-height: 1.4; }

/* Status badges */
.status-badge { padding: 3px 10px; border-radius: 99px; font-size: 0.72rem; font-weight: 700; text-transform: uppercase; white-space: nowrap; }
.status-badge.pending   { background: #fef3c7; color: #d97706; }
.status-badge.accepted  { background: #dbeafe; color: #1d4ed8; }
.status-badge.completed { background: #dcfce7; color: #166534; }
.status-badge.cancelled { background: #fee2e2; color: #991b1b; }
.status-badge.scanned   { background: #d1fae5; color: #065f46; }

/* Type badges */
.type-badge { padding: 3px 10px; border-radius: 6px; font-size: 0.72rem; font-weight: 700; white-space: nowrap; }
.tb-brain  { background: #dbeafe; color: #1d4ed8; }
.tb-spleen { background: #fce7f3; color: #9d174d; }
.tb-lungs  { background: #d1fae5; color: #065f46; }

/* Severity badges */
.sev-badge { font-size: 0.78rem; font-weight: 700; }
.svb-routine  { color: #15803d; }
.svb-urgent   { color: #b45309; }
.svb-critical { color: #dc2626; }

/* Buttons */
.report-btn {
  background: #0d9488; color: white; border: none;
  padding: 6px 12px; border-radius: 6px; cursor: pointer;
  font-size: 0.78rem; font-weight: 600; white-space: nowrap; transition: background .15s;
}
.report-btn:hover { background: #0f766e; }
.cancel-btn {
  background: white; border: 1px solid #ef4444; color: #ef4444;
  padding: 5px 10px; border-radius: 6px; cursor: pointer; font-size: 0.78rem; transition: all .15s;
}
.cancel-btn:hover { background: #fef2f2; }

/* Loading / empty states */
.loading-state, .empty-state {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 50px 20px; gap: 10px; color: #94a3b8;
}
.spinner {
  width: 28px; height: 28px;
  border: 3px solid #e2e8f0; border-top-color: #0d9488;
  border-radius: 50%; animation: spin .7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.empty-icon { font-size: 2.5rem; margin-bottom: 6px; }
.empty-state p { margin: 0; font-size: 0.95rem; font-weight: 600; color: #374151; }
.empty-state small { font-size: 0.8rem; }

/* ── Booking modal ── */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; justify-content: center; align-items: center; z-index: 1000; }
.modal-card { background: white; width: 560px; border-radius: 14px; padding: 26px; box-shadow: 0 20px 60px rgba(0,0,0,.2); display: flex; flex-direction: column; max-height: 90vh; }
.modal-body { overflow-y: auto; padding-right: 4px; }
.scrollable { overflow-y: auto; padding-right: 4px; }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.modal-header h3 { margin: 0; font-size: 1.05rem; font-weight: 700; color: #0f172a; }
.close-btn { background: none; border: none; font-size: 1.5rem; cursor: pointer; color: #94a3b8; line-height: 1; }
.form-section-title { font-size: 0.82rem; color: #0d9488; margin: 16px 0 10px; font-weight: 700; text-transform: uppercase; letter-spacing: .5px; border-bottom: 1px solid #e2e8f0; padding-bottom: 6px; }
.form-group { margin-bottom: 14px; }
.form-row { display: flex; gap: 14px; }
.form-group.half { flex: 1; }
.form-group label { display: block; margin-bottom: 5px; font-weight: 600; color: #374151; font-size: 0.83rem; }
.input-field { width: 100%; padding: 9px 11px; border: 1px solid #cbd5e1; border-radius: 7px; font-size: 0.85rem; box-sizing: border-box; transition: border-color .15s; }
.input-field:focus { outline: none; border-color: #0d9488; box-shadow: 0 0 0 3px rgba(13,148,136,.12); }
.bg-gray { background-color: #f3f4f6; cursor: not-allowed; }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; margin-top: 18px; padding-top: 14px; border-top: 1px solid #f1f5f9; }
.secondary-btn { background: none; border: 1px solid #cbd5e1; padding: 9px 18px; border-radius: 7px; cursor: pointer; font-size: 0.85rem; color: #374151; }
.primary-btn { background: #0d9488; color: white; border: none; padding: 9px 20px; border-radius: 7px; cursor: pointer; font-weight: 600; font-size: 0.85rem; }
.primary-btn:hover { background: #0f766e; }

.animate-fade { animation: fadeIn 0.25s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: translateY(0); } }
</style>