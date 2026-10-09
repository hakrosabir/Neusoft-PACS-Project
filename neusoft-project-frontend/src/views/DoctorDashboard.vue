<template>
  <div class="dashboard-layout">

    <AppSidebar
      active-id="appointments"
      :username="user.username || 'Radiologist'"
      :badges="{ appointments: pendingCount || null }"
      @go-home="router.push('/doctor-dashboard')"
      @navigate="handleNav"
      @logout="logout"
    />

    <main class="main-content">
      <header class="top-header">
        <div class="header-left">
          <h1>Doctor Dashboard</h1>
          <p class="date-text">{{ currentDate }}</p>
        </div>
        <div class="header-right">
          <button class="notif-btn" @click="fetchAppointments">↻ Refresh</button>
        </div>
      </header>

      <div class="content-body">

        <!-- TAB: APPOINTMENTS -->
        <div v-if="currentTab === 'appointments'" class="appointments-view">
          <div class="stats-row">
            <div class="stat-card">
              <span class="stat-num">{{ scannedCount }}</span>
              <span class="stat-label">Ready for Review</span>
            </div>
            <div class="stat-card">
              <span class="stat-num">{{ pendingCount }}</span>
              <span class="stat-label">Pending Requests</span>
            </div>
            <div class="stat-card">
              <span class="stat-num">{{ appointments.length }}</span>
              <span class="stat-label">Total Appointments</span>
            </div>
          </div>

          <div v-if="scannedAppointments.length > 0">
            <div class="section-title"><h3>☢️ Radiology Review Queue</h3></div>
            <div class="table-container" style="margin-bottom: 30px; border-left: 4px solid #2563eb;">
              <table>
                <thead>
                  <tr><th>Date</th><th>Patient</th><th>Technician Notes</th><th>Scan Status</th><th>Action</th></tr>
                </thead>
                <tbody>
                  <tr v-for="apt in scannedAppointments" :key="apt.id" class="highlight-row">
                    <td>{{ apt.appointmentDate }}</td>
                    <td>
                      <div class="patient-cell">
                        <div class="p-avatar">{{ (apt.patientName || 'P').charAt(0).toUpperCase() }}</div>
                        <span>{{ apt.patientName }}</span>
                      </div>
                    </td>
                    <td class="notes-cell">{{ apt.nurseNotes || 'No notes provided' }}</td>
                    <td><span class="status-badge scanned">✔ Scan Ready</span></td>
                    <td>
                      <button class="btn-review" @click="reportSystemRef.openEditor(apt)">👁 Review & Report</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="section-title"><h3>All Appointment Requests</h3></div>
          <div class="table-container">
            <table>
              <thead>
                <tr><th>Date & Time</th><th>Patient Name</th><th>Department / Scan</th><th>Status</th><th>Action</th></tr>
              </thead>
              <tbody>
                <tr v-if="filteredAllAppointments.length === 0">
                  <td colspan="5" class="text-center">No appointments found.</td>
                </tr>
                <tr
                  v-for="apt in filteredAllAppointments"
                  :key="apt.id"
                  @click="selectPatientForAI(apt)"
                  :class="{ 'selected-row': selectedAptForAI?.id === apt.id }"
                >
                  <td>
                    <div style="font-weight:600">{{ apt.appointmentDate }}</div>
                    <small style="color:#64748b">{{ apt.timeSlot }}</small>
                  </td>
                  <td>{{ apt.patientName }}</td>
                  <td>{{ apt.department }}</td>
                  <td>
                    <span :class="['status-badge', (apt.status || 'PENDING').toLowerCase()]">
                      {{ apt.status || 'PENDING' }}
                    </span>
                  </td>
                  <td>
                    <div v-if="isPending(apt.status)" class="action-buttons">
                      <button class="btn-accept"  @click.stop="updateStatus(apt.id, 'ACCEPTED')">✅ Accept</button>
                      <button class="btn-decline" @click.stop="updateStatus(apt.id, 'DECLINED')">❌ Decline</button>
                    </div>
                    <div v-else-if="apt.status === 'ACCEPTED'" class="action-buttons">
                      <button class="btn-report" @click.stop="reportSystemRef.openEditor(apt)">📝 Write Report</button>
                    </div>
                    <div v-else-if="apt.status === 'COMPLETED'" class="action-buttons">
                      <button class="btn-view" @click.stop="openReportViewer(apt)">📄 View Report</button>
                    </div>
                    <span v-else class="text-muted">{{ apt.status }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- TAB: REPORTS — FIX: now fetches real report data and shows diagnosis -->
        <div v-if="currentTab === 'reports'" class="appointments-view">
          <div class="section-title">
            <h3>Medical Reports History</h3>
          </div>

          <div v-if="reportsLoading" class="reports-loading">
            <div class="loading-spinner"></div>
            <span>Loading reports...</span>
          </div>

          <div class="table-container" v-else>
            <table>
              <thead>
                <tr>
                  <th>Report Date</th>
                  <th>Patient Name</th>
                  <th>Analysis Type</th>
                  <th>Diagnosis Summary</th>
                  <th>Severity</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="allReports.length === 0">
                  <td colspan="6" class="text-center">No finalized reports found.</td>
                </tr>
                <tr v-for="rep in allReports" :key="rep.id">
                  <td>
                    <div style="font-weight:600">{{ formatDate(rep.reportDate || rep.createdAt) }}</div>
                    <small style="color:#64748b">#{{ rep.id }}</small>
                  </td>
                  <td>
                    <div class="patient-cell">
                      <div class="p-avatar">{{ (rep.patientName || 'P').charAt(0).toUpperCase() }}</div>
                      <span>{{ rep.patientName }}</span>
                    </div>
                  </td>
                  <td>
                    <span class="type-chip" :class="'tc-' + normalizeType(rep.analysisType)">
                      {{ typeIcon(rep.analysisType) }} {{ typeLabel(rep.analysisType) }}
                    </span>
                  </td>
                  <td class="notes-cell">{{ truncate(rep.diagnosis, 70) }}</td>
                  <td>
                    <span class="sev-chip" :class="'sv-' + (rep.severity || 'routine')">
                      {{ { routine: '🟢 Routine', urgent: '🟡 Urgent', critical: '🔴 Critical' }[(rep.severity || 'routine').toLowerCase()] || 'Routine' }}
                    </span>
                  </td>
                  <td>
                    <div class="action-buttons">
                      <button class="btn-view" @click="viewReportById(rep)">👁 View</button>
                      <button class="btn-report" @click="editReportById(rep)">✎ Edit</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- TAB: WORKLIST -->
        <div v-if="currentTab === 'worklist'" class="worklist-view">
          <div class="section-title"><h3>Radiology Worklist</h3></div>
          <div class="table-container">
            <table>
              <thead><tr><th>Priority</th><th>Patient ID</th><th>Status</th></tr></thead>
              <tbody>
                <tr v-for="item in worklist" :key="item.id">
                  <td>High</td><td>{{ item.pid }}</td><td>Pending</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

      </div>
    </main>

    <ReportSystem
      ref="reportSystemRef"
      :user="user"
      @refresh-data="onReportSaved"
    />
    <DoctorChatbot :user="user" :active-patient="selectedAptForAI" />
  </div>
</template>

<script setup>
import { JAVA_API_URL } from '../config.js'
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import AppSidebar    from '@/components/AppSidebar.vue'
import DoctorChatbot from '@/components/DoctorChatbot.vue'
import ReportSystem  from '@/components/ReportSystem.vue'

const router = useRouter()
const user   = ref(JSON.parse(localStorage.getItem('user') || '{}'))

const API_BASE    = JAVA_API_URL
const currentDate = new Date().toLocaleDateString()
const currentTab  = ref('appointments')

const appointments     = ref([])
const allReports       = ref([])    // FIX: separate list for actual report objects
const reportsLoading   = ref(false)
const reportSystemRef  = ref(null)
const selectedAptForAI = ref(null)
const worklist = ref([
  { id: 1, pid: 'P-101', priority: 'High',   modality: 'CT' },
  { id: 2, pid: 'P-102', priority: 'Normal', modality: 'MRI' },
])

// ── Nav ──────────────────────────────────────────────────────────────
function handleNav(tab) {
  if (tab.route !== '/doctor-dashboard') {
    router.push(tab.route)
  } else {
    currentTab.value = tab.id
  }
}
function logout() { localStorage.removeItem('user'); router.push('/') }

// ── Data ──────────────────────────────────────────────────────────────
async function fetchAppointments() {
  try {
    const res = await axios.get(`${API_BASE}/appointments/all`)
    appointments.value = Array.isArray(res.data)
      ? res.data.sort((a, b) => {
          if (a.status === 'SCANNED' && b.status !== 'SCANNED') return -1
          if (a.status !== 'SCANNED' && b.status === 'SCANNED') return  1
          return (b.id || 0) - (a.id || 0)
        })
      : []
  } catch (err) { console.error('fetchAppointments:', err) }
}

// FIX: Fetch actual report objects for the reports tab
async function fetchAllReports() {
  reportsLoading.value = true
  try {
    // Try doctor-specific endpoint first, fall back to general
    const doctorId = user.value.id || 999
    const res = await axios.get(`${API_BASE}/reports/doctor/${doctorId}`)
      .catch(() => axios.get(`${API_BASE}/reports/all`))
    allReports.value = Array.isArray(res.data)
      ? res.data.sort((a, b) => new Date(b.reportDate || 0) - new Date(a.reportDate || 0))
      : []
  } catch (err) {
    console.error('fetchAllReports:', err)
    // Fallback: build from completed appointments
    allReports.value = []
  } finally {
    reportsLoading.value = false
  }
}

async function updateStatus(id, newStatus) {
  try {
    await axios.put(`${API_BASE}/appointments/${id}/status`, { status: newStatus })
    fetchAppointments()
  } catch { alert('Status Update Failed') }
}

// FIX: openReportViewer now fetches actual report data
async function openReportViewer(apt) {
  try {
    const res = await axios.get(`${API_BASE}/reports/appointment/${apt.id}`)
    if (res.data) {
      reportSystemRef.value.openViewer(res.data, apt)
    } else {
      alert('Report not found for this appointment.')
    }
  } catch { alert('Error retrieving report.') }
}

// FIX: View report directly from allReports list (has full data)
async function viewReportById(rep) {
  // Find the matching appointment for context
  const apt = appointments.value.find(a => a.id === rep.appointmentId) || {
    id: rep.appointmentId,
    patientId: rep.patientId,
    patientName: rep.patientName,
    patientGender: rep.patientGender,
    patientBirthDate: rep.patientBirthDate,
    dicomPatientId: rep.dicomPatientId,
    department: rep.department || 'Radiology',
  }
  reportSystemRef.value.openViewer(rep, apt)
}

// FIX: Edit report from the reports tab
async function editReportById(rep) {
  const apt = appointments.value.find(a => a.id === rep.appointmentId) || {
    id: rep.appointmentId,
    patientId: rep.patientId,
    patientName: rep.patientName,
    patientGender: rep.patientGender,
    patientBirthDate: rep.patientBirthDate,
    dicomPatientId: rep.dicomPatientId,
    department: rep.department || 'Radiology',
  }
  reportSystemRef.value.openEditor(apt, rep)
}

// Called after a report is saved — refresh both lists
function onReportSaved() {
  fetchAppointments()
  if (currentTab.value === 'reports') fetchAllReports()
}

function selectPatientForAI(apt) { selectedAptForAI.value = apt }

function isPending(status) {
  const s = (status || '').toUpperCase()
  return s === 'PENDING' || s === ''
}

// ── Helpers ───────────────────────────────────────────────────────────
function normalizeType(t) {
  if (!t) return 'brain'
  const s = String(t).toLowerCase()
  if (s.includes('spleen') || s.includes('ich')) return 'spleen'
  if (s.includes('lung')) return 'lungs'
  return 'brain'
}
function typeIcon(t)  { return { brain: '🧠', spleen: '🫀', lungs: '🫁' }[normalizeType(t)] || '📋' }
function typeLabel(t) { return { brain: 'Brain MRI', spleen: 'CT Spleen', lungs: 'Lung CT' }[normalizeType(t)] || 'General' }
function truncate(s, n) { return s && s.length > n ? s.slice(0, n) + '…' : (s || '—') }
function formatDate(d) {
  if (!d) return '—'
  try { return new Date(d).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) }
  catch { return '—' }
}

// ── Computed ──────────────────────────────────────────────────────────
const scannedAppointments     = computed(() => appointments.value.filter(a => a.status === 'SCANNED'))
const filteredAllAppointments = computed(() => appointments.value.filter(a => a.status !== 'SCANNED'))
const scannedCount            = computed(() => scannedAppointments.value.length)
const pendingCount            = computed(() => appointments.value.filter(a => isPending(a.status)).length)

// FIX: Watch for tab change to fetch reports when switching to reports tab
watch(currentTab, (newTab) => {
  if (newTab === 'reports') fetchAllReports()
})

onMounted(() => {
  fetchAppointments()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&display=swap');

.dashboard-layout {
  display: flex; height: 100vh;
  background: #f0f2f5; font-family: 'DM Sans', sans-serif; color: #1f2d3d;
}

.main-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

.top-header {
  background: white; padding: 15px 30px;
  display: flex; justify-content: space-between; align-items: center;
  border-bottom: 1px solid #e0e0e0; flex-shrink: 0;
}
.top-header h1 { margin: 0; font-size: 1.3rem; font-weight: 700; color: #0f172a; }
.date-text { margin: 2px 0 0; font-size: 0.8rem; color: #94a3b8; }
.notif-btn {
  background: white; border: 1px solid #e2e8f0;
  padding: 8px 16px; border-radius: 6px;
  cursor: pointer; font-weight: 600; color: #475569; transition: 0.2s;
}
.notif-btn:hover { background: #f8fafc; }

.content-body { flex: 1; padding: 28px 30px; overflow-y: auto; }

.stats-row { display: flex; gap: 20px; margin-bottom: 28px; }
.stat-card {
  flex: 1; background: white; padding: 20px 24px;
  border-radius: 10px; box-shadow: 0 1px 4px rgba(0,0,0,.06);
  display: flex; flex-direction: column; gap: 4px;
}
.stat-num   { font-size: 2rem; font-weight: 700; color: #2563eb; }
.stat-label { font-size: 0.82rem; color: #64748b; font-weight: 500; }

.section-title { margin-bottom: 12px; }
.section-title h3 { margin: 0; font-size: 1rem; font-weight: 700; color: #1e293b; }

.table-container {
  background: white; border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
  overflow: hidden; margin-bottom: 28px;
}
table { width: 100%; border-collapse: collapse; }
th, td { padding: 14px 18px; text-align: left; border-bottom: 1px solid #f1f5f9; font-size: 0.875rem; }
th { background: #fafafa; font-weight: 600; color: #374151; font-size: 0.8rem; }
tbody tr:hover { background: #f8fafc; }
.text-center { text-align: center; color: #94a3b8; }
.highlight-row { background: #f0fdf4; }
.selected-row  { background: #eff6ff; border-left: 3px solid #2563eb; }
.notes-cell { color: #475569; font-size: 0.82rem; max-width: 260px; }

.patient-cell { display: flex; align-items: center; gap: 8px; }
.p-avatar {
  width: 30px; height: 30px; background: #dbeafe; color: #1d4ed8;
  border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 0.8rem; flex-shrink: 0;
}

.status-badge { padding: 3px 10px; border-radius: 999px; font-size: 0.72rem; font-weight: 700; text-transform: uppercase; }
.status-badge.pending   { background: #fef9c3; color: #a16207; }
.status-badge.accepted  { background: #dbeafe; color: #1d4ed8; }
.status-badge.completed { background: #dcfce7; color: #166534; }
.status-badge.declined  { background: #fee2e2; color: #b91c1c; }
.status-badge.scanned   { background: #d1fae5; color: #065f46; border: 1px solid #6ee7b7; }

/* Type chips for reports table */
.type-chip { padding: 3px 10px; border-radius: 6px; font-size: 0.72rem; font-weight: 700; white-space: nowrap; }
.tc-brain  { background: #dbeafe; color: #1d4ed8; }
.tc-spleen { background: #fce7f3; color: #9d174d; }
.tc-lungs  { background: #d1fae5; color: #065f46; }

.sev-chip { font-size: 0.72rem; font-weight: 700; }
.sv-routine  { color: #15803d; }
.sv-urgent   { color: #b45309; }
.sv-critical { color: #dc2626; }

.action-buttons { display: flex; gap: 6px; }
.btn-accept  { background: #f0fdf4; color: #16a34a; border: 1px solid #86efac; padding: 5px 10px; border-radius: 5px; cursor: pointer; font-weight: 600; font-size: 0.8rem; }
.btn-decline { background: #fef2f2; color: #dc2626; border: 1px solid #fca5a5; padding: 5px 10px; border-radius: 5px; cursor: pointer; font-weight: 600; font-size: 0.8rem; }
.btn-report  { background: #2563eb; color: white; border: none; padding: 5px 12px; border-radius: 5px; cursor: pointer; font-weight: 600; font-size: 0.8rem; }
.btn-view    { background: white; border: 1px solid #cbd5e1; padding: 5px 10px; border-radius: 5px; cursor: pointer; font-size: 0.8rem; font-weight: 500; }
.btn-view:hover { background: #f8fafc; }
.btn-review  { background: #1d4ed8; color: white; border: none; padding: 6px 14px; border-radius: 5px; cursor: pointer; font-weight: 600; font-size: 0.8rem; }
.text-muted  { color: #94a3b8; font-size: 0.8rem; }

.reports-loading {
  display: flex; align-items: center; gap: 12px;
  padding: 40px; justify-content: center; color: #64748b;
}
.loading-spinner {
  width: 20px; height: 20px;
  border: 2px solid #e2e8f0; border-top-color: #2563eb;
  border-radius: 50%; animation: spin .7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>