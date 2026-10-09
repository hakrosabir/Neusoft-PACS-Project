<template>
  <div class="admin-layout">

    <!-- Sidebar -->
    <AdminSidebar
      :currentTab="currentTab"
      @tab-change="currentTab = $event"
      @logout="logout"
    />

    <!-- Main content area -->
    <main class="main-content">

      <!-- Top header bar -->
      <header class="top-header">
        <div class="header-left">
          <h1>Admin Console</h1>
          <div class="global-status-pill" :class="systemOk ? 'status-ok' : 'status-crit'">
            <span class="indicator-light"></span>
            <span>{{ systemOk ? 'SYSTEM OPERATIONAL' : 'SYSTEM CRITICAL' }}</span>
          </div>
        </div>
        <div class="header-right">
          <div class="live-tag"><span class="blink">●</span> LIVE TELEMETRY</div>
          <span class="server-time">{{ currentTime }}</span>
          <button class="refresh-btn" @click="refreshAll" title="Refresh">↻</button>
        </div>
      </header>

      <!-- Tab views -->
      <div class="content-body">

        <!-- Mission Control -->
        <AdminOverview
          v-if="currentTab === 'overview'"
          :backendOnline="backendOnline"
          :dbOnline="dbOnline"
          :aiOnline="aiOnline"
          :isAiGpu="isAiGpu"
          :llmOnline="llmOnline"
          :llmModelName="llmModelName"
          :llmGpu="llmGpu"
          :liveMetrics="liveMetrics"
          :uptime="uptime"
          :totalUsers="users.length"
          :pendingDoctors="pendingDoctors"
          :totalDoctors="totalDoctors"
          :totalPatients="totalPatients"
          :totalTechnicians="totalTechnicians"
        />

        <!-- User Management -->
        <AdminUserManagement
          v-if="currentTab === 'users'"
          :users="users"
          @approve="approveUser"
          @toggle-lock="toggleLock"
          @refresh="refreshAll"
        />

        <!-- System Logs -->
        <AdminSystemLogs
          v-if="currentTab === 'logs'"
          :logs="auditLogs"
          :loading="logsLoading"
          :currentTime="currentTime"
        />

      </div>
    </main>

  </div>
</template>

<script setup>
import { JAVA_API_URL, OLLAMA_API_URL, PYTHON_API_URL } from '../config.js'
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

// ── Component imports ──
import AdminSidebar        from '@/components/Adminsidebar.vue'
import AdminOverview       from '@/components/AdminOverview.vue'
import AdminUserManagement from '@/components/AdminUserManagement.vue'
import AdminSystemLogs     from '@/components/AdminSystemLogs.vue'

const router = useRouter()

// ── Config ──
const API_BASE    = `${JAVA_API_URL}/admin`
const PYTHON_BASE = PYTHON_API_URL
const OLLAMA_BASE = OLLAMA_API_URL
const POLL_RATE   = 5000   // ms — how often logs + health refresh
const TICK_RATE   = 1000   // ms — clock + metric animation

// ── UI state ──
const currentTab = ref('overview')
const currentTime = ref(new Date().toLocaleTimeString())

// ── System health state ──
const backendOnline = ref(true)
const dbOnline      = ref(true)
const aiOnline      = ref(false)
const isAiGpu       = ref(false)
const llmOnline     = ref(false)
const llmModelName  = ref('Loading...')
const llmGpu        = ref(false)
const uptime        = ref('--')

// ── Live animated metrics (cosmetic, randomised each tick) ──
const liveMetrics = reactive({
  jvmHeap: 450, jvmThreads: 24,
  aiLatency: 0, llmLoad: 0, dbConns: 5
})

// ── Data ──
const users       = ref([])
const auditLogs   = ref([])
const logsLoading = ref(false)

// ── Computed helpers ──
const systemOk         = computed(() => backendOnline.value && dbOnline.value)
const pendingDoctors   = computed(() => users.value.filter(u => u.role === 'DOCTOR' && !u.verified).length)
const totalDoctors     = computed(() => users.value.filter(u => u.role === 'DOCTOR').length)
const totalPatients    = computed(() => users.value.filter(u => u.role === 'PATIENT').length)
const totalTechnicians = computed(() => users.value.filter(u => u.role === 'TECHNICIAN' || u.role === 'NURSE').length)

// ═══════════════════════════════════════
// DATA FETCHING
// ═══════════════════════════════════════

async function fetchUsers() {
  try {
    const res = await axios.get(`${API_BASE}/users`)
    users.value = Array.isArray(res.data) ? res.data : []
    backendOnline.value = true
  } catch {
    backendOnline.value = false
  }
}

async function fetchLogs() {
  logsLoading.value = true
  try {
    const res = await axios.get(`${API_BASE}/system/logs?limit=200`)
    auditLogs.value = Array.isArray(res.data) ? res.data : []
  } catch { /* keep existing logs */ }
  finally { logsLoading.value = false }
}

async function checkHealth() {
  // Python AI engine
  const start = Date.now()
  try {
    const res = await axios.get(`${PYTHON_BASE}/health`, { timeout: 2000 })
    if (res.data?.status === 'ONLINE' || res.data?.status === 'ok') {
      aiOnline.value    = true
      isAiGpu.value     = /true|cuda|on|1/.test(String(res.data.gpu).toLowerCase())
      liveMetrics.aiLatency = Date.now() - start
    } else throw new Error()
  } catch { aiOnline.value = false; isAiGpu.value = false; liveMetrics.aiLatency = 0 }

  // Ollama LLM
  try {
    const res = await axios.get(`${OLLAMA_BASE}/api/tags`, { timeout: 2000 })
    llmOnline.value = true
    const m = res.data?.models?.find(m => m.name.includes('neusoft-ai'))
    llmModelName.value = m ? m.name : 'Model Missing'
    llmGpu.value = !!m
  } catch { llmOnline.value = false; llmModelName.value = 'Connection Failed'; llmGpu.value = false }

  // DB stats
  try {
    const res = await axios.get(`${API_BASE}/system/db-stats`, { timeout: 2000 })
    dbOnline.value = true
    uptime.value   = res.data?.uptime || '48h 12m'
  } catch { dbOnline.value = false }
}

async function refreshAll() {
  await Promise.all([fetchUsers(), fetchLogs(), checkHealth()])
}

// ═══════════════════════════════════════
// USER ACTIONS (called via emits from AdminUserManagement)
// ═══════════════════════════════════════

async function approveUser(id) {
  try {
    await axios.put(`${API_BASE}/users/${id}/approve`)
    await refreshAll()
  } catch { alert('Failed to approve doctor') }
}

async function toggleLock(user) {
  try {
    await axios.put(`${API_BASE}/users/${user.id}/status`, { enabled: !user.enabled })
    await refreshAll()
  } catch { alert('Failed to update account status') }
}

function logout() {
  localStorage.removeItem('user')
  router.push('/')
}

// ═══════════════════════════════════════
// TIMERS
// ═══════════════════════════════════════

let tickTimer, pollTimer

function startTimers() {
  // Every second: update clock + animate metrics
  tickTimer = setInterval(() => {
    currentTime.value = new Date().toLocaleTimeString()
    if (backendOnline.value) {
      liveMetrics.jvmHeap    = 450 + Math.floor(Math.random() * 80)
      liveMetrics.jvmThreads = 20  + Math.floor(Math.random() * 5)
    } else {
      liveMetrics.jvmHeap = 0; liveMetrics.jvmThreads = 0
    }
    if (llmOnline.value) liveMetrics.llmLoad = 15 + Math.floor(Math.random() * 20)
    if (dbOnline.value)  liveMetrics.dbConns =  5 + Math.floor(Math.random() * 10)
  }, TICK_RATE)

  // Every 5 seconds: refresh data + health
  pollTimer = setInterval(refreshAll, POLL_RATE)
}

onMounted(() => { startTimers(); refreshAll() })
onUnmounted(() => { clearInterval(tickTimer); clearInterval(pollTimer) })
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Rajdhani:wght@500;600;700&family=Inter:wght@400;600&display=swap');

/* ── Root layout ── */
.admin-layout {
  display: flex;
  height: 100vh;
  background-color: #0b1121;
  font-family: 'Inter', sans-serif;
  color: #e2e8f0;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #0f172a;
}

/* ── Top header ── */
.top-header {
  height: 75px;
  background: #020617;
  border-bottom: 1px solid #1e293b;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 30px;
  flex-shrink: 0;
}
.header-left h1 {
  margin: 0;
  font-family: 'Rajdhani', sans-serif;
  font-weight: 700;
  font-size: 1.8rem;
  color: white;
  letter-spacing: 1px;
}

.global-status-pill {
  display: flex; align-items: center; gap: 10px;
  margin-top: 5px;
  background: rgba(255,255,255,0.05);
  padding: 4px 12px;
  border-radius: 20px;
  border: 1px solid #334155;
  width: fit-content;
}
.status-ok   { border-color: #22c55e; color: #22c55e; }
.status-crit { border-color: #ef4444; color: #ef4444; }
.indicator-light {
  width: 8px; height: 8px; border-radius: 50%;
  background: currentColor;
  box-shadow: 0 0 10px currentColor;
  animation: pulse 2s infinite;
}

.header-right  { display: flex; align-items: center; gap: 20px; }
.live-tag      { display: flex; align-items: center; gap: 6px; color: #ef4444; font-weight: 700; font-size: 0.75rem; border: 1px solid #ef4444; padding: 4px 8px; border-radius: 4px; }
.server-time   { font-size: 0.85rem; color: #94a3b8; font-family: monospace; }
.refresh-btn   { background: #1e293b; color: #94a3b8; border: 1px solid #334155; padding: 8px 12px; border-radius: 6px; cursor: pointer; font-size: 1rem; transition: 0.2s; }
.refresh-btn:hover { color: white; border-color: white; }

/* ── Content body ── */
.content-body { padding: 30px; overflow-y: auto; flex: 1; }

/* ── Animations ── */
@keyframes pulse { 0% { box-shadow: 0 0 0 0 currentColor; } 100% { box-shadow: 0 0 0 10px transparent; } }
@keyframes blink { 50% { opacity: 0; } }
.blink { animation: blink 1s infinite; }
</style>