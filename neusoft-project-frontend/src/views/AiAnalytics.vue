<template>
  <div class="layout-container">

    <AppSidebar
      active-id="analytics"
      :username="user.username || 'User'"
      @go-home="router.push('/doctor-dashboard')"
      @navigate="handleNav"
      @logout="logout"
    />

    <main class="content-area">
      <div class="tool-container">

        <header class="page-header">
          <div>
            <h2 class="title">AI Utility Tools</h2>
            <p class="subtitle">Advanced segmentation and processing tools.</p>
          </div>
        </header>

        <div class="tool-card">
          <div class="card-header">
            <h3>🫁 Lung Segmentation (TotalSegmentator)</h3>
            <span class="badge">GPU Accelerated</span>
          </div>

          <div class="tool-body">
            <p class="helper-text">
              Supported formats: <strong>.nii, .nii.gz, .nrrd, .zip (DICOM)</strong>.
              The result will be downloaded automatically as a ZIP file containing the segmentation masks.
            </p>

            <div class="upload-section">
              <label class="file-label">
                <input type="file" @change="onFileChange" class="file-input" />
                <span v-if="!file" class="file-cta">Click to select file</span>
                <span v-else class="file-name">{{ file.name }}</span>
              </label>
              <button
                class="primary-btn"
                @click="startSegmentation"
                :disabled="!file || loading"
              >
                {{ loading ? 'Processing...' : 'Start Lung Segmentation' }}
              </button>
            </div>

            <div class="status-console" v-if="caseId || statusText || errorText">
              <div v-if="caseId" class="console-line">
                <span class="label">Job ID:</span>
                <span class="mono">{{ caseId }}</span>
              </div>
              <div v-if="statusText" class="console-line">
                <span class="label">Status:</span>
                <span class="val">{{ statusText }}</span>
              </div>
              <div v-if="errorText" class="console-line error">
                <span class="label">Error:</span>
                <span>{{ errorText }}</span>
              </div>
            </div>
          </div>
        </div>

      </div>
    </main>
  </div>
</template>

<script setup>
import { PYTHON_API_URL } from '../config.js'
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import AppSidebar from '@/components/AppSidebar.vue'

const router   = useRouter()
const user     = ref({})
const API_BASE = PYTHON_API_URL

function handleNav(tab) {
  if (tab.route === '/analytics') return
  router.push(tab.route)
}
function logout() { localStorage.removeItem('user'); router.push('/') }

// ── Lung segmentation state ───────────────────────────────────────────
const file       = ref(null)
const loading    = ref(false)
const statusText = ref('')
const errorText  = ref('')
const caseId     = ref('')
let pollTimer    = null

const onFileChange = (e) => {
  file.value       = e.target.files[0] || null
  statusText.value = ''
  errorText.value  = ''
  caseId.value     = ''
}

const startSegmentation = async () => {
  if (!file.value) return
  loading.value    = true
  statusText.value = 'Uploading file and starting lung segmentation job...'
  errorText.value  = ''
  caseId.value     = ''
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }

  try {
    const formData = new FormData()
    formData.append('file', file.value)
    const startRes = await axios.post(`${API_BASE}/api/totalseg_start`, formData)
    caseId.value = startRes.data.case_id || ''
    if (!caseId.value) throw new Error('No case_id returned from backend.')
    statusText.value = 'Job started. Waiting for status updates...'
    pollTimer = setInterval(checkStatus, 3000)
  } catch (err) {
    errorText.value  = 'Failed to start lung segmentation: ' + (err.message || 'Unknown error')
    statusText.value = ''
    loading.value    = false
  }
}

const checkStatus = async () => {
  if (!caseId.value) return
  try {
    const res    = await axios.get(`${API_BASE}/api/totalseg_status/${caseId.value}`)
    const status = res.data.status
    const error  = res.data.error

    if (error) {
      statusText.value = 'Job failed.'
      errorText.value  = error
      stopPolling(); loading.value = false; return
    }
    if (status === 'finished') {
      statusText.value = 'Lung segmentation finished. Downloading result ZIP...'
      await downloadResult()
      stopPolling(); loading.value = false
    } else {
      statusText.value = status
    }
  } catch (err) {
    errorText.value = 'Error while checking status: ' + (err.message || 'Unknown error')
    stopPolling(); loading.value = false
  }
}

const downloadResult = async () => {
  if (!caseId.value) return
  try {
    const res  = await axios.get(`${API_BASE}/api/totalseg_download/${caseId.value}`, { responseType: 'blob' })
    const link = document.createElement('a')
    link.href  = URL.createObjectURL(new Blob([res.data]))
    link.download = `${(file.value?.name || 'result').replace(/\..+$/, '')}_lungs_nrrd.zip`
    document.body.appendChild(link); link.click(); document.body.removeChild(link)
    statusText.value = 'Download started.'
  } catch (err) {
    errorText.value = 'Failed to download result: ' + (err.message || 'Unknown error')
  }
}

const stopPolling = () => { if (pollTimer) { clearInterval(pollTimer); pollTimer = null } }

onMounted(() => {
  const stored = localStorage.getItem('user')
  if (stored) user.value = JSON.parse(stored)
})
onBeforeUnmount(() => stopPolling())
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&display=swap');

.layout-container { display: flex; height: 100vh; width: 100vw; background: #0b1120; font-family: 'DM Sans', sans-serif; color: #e5e7eb; }
.content-area { flex: 1; overflow-y: auto; padding: 24px; }
.tool-container { max-width: 800px; margin: 0 auto; }

.page-header { margin-bottom: 24px; }
.title    { font-size: 1.5rem; font-weight: 700; margin: 0; color: white; }
.subtitle { color: #94a3b8; margin: 4px 0 0; font-size: 0.875rem; }

.tool-card {
  background: #111827; border: 1px solid #1f2937; border-radius: 12px;
  padding: 24px; margin-bottom: 24px;
}
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.card-header h3 { margin: 0; color: white; font-size: 1.1rem; font-weight: 700; }
.badge { background: #064e3b; color: #34d399; font-size: 0.72rem; padding: 4px 10px; border-radius: 999px; font-weight: 600; }

.helper-text { color: #9ca3af; font-size: 0.9rem; margin-bottom: 20px; line-height: 1.6; }

.upload-section {
  display: flex; gap: 12px; align-items: center; margin-bottom: 20px;
  background: #1f2937; padding: 12px; border-radius: 8px; border: 1px dashed #374151;
}
.file-label { flex: 1; cursor: pointer; display: flex; align-items: center; }
.file-input { display: none; }
.file-cta  { color: #60a5fa; font-size: 0.875rem; font-weight: 500; }
.file-name { color: #e5e7eb; font-size: 0.875rem; font-weight: 500; word-break: break-all; }

.primary-btn { background: #2563eb; color: white; border: none; padding: 10px 22px; border-radius: 6px; cursor: pointer; font-weight: 600; font-size: 0.875rem; transition: background 0.2s; white-space: nowrap; }
.primary-btn:hover { background: #1d4ed8; }
.primary-btn:disabled { background: #374151; color: #9ca3af; cursor: not-allowed; }

.status-console {
  background: #000; border-radius: 8px; padding: 16px;
  font-family: 'Courier New', monospace; font-size: 0.875rem;
  border: 1px solid #374151;
}
.console-line { margin-bottom: 6px; display: flex; }
.console-line:last-child { margin-bottom: 0; }
.console-line .label { color: #6b7280; margin-right: 12px; min-width: 58px; }
.console-line .val  { color: #60a5fa; }
.console-line .mono { color: #a78bfa; letter-spacing: 0.5px; }
.console-line.error span { color: #f87171; }
</style>