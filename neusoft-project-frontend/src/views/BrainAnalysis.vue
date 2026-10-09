<template>
  <div class="dashboard-layout">

    <AppSidebar
      active-id="brain"
      :username="user.username || 'Radiologist'"
      @go-home="router.push('/doctor-dashboard')"
      @navigate="handleNav"
      @logout="handleLogout"
    />

    <main class="main-content">
      <header class="studio-header">
        <div>
          <h2>🧠 Brain Analysis Studio</h2>
          <p class="subtitle">AI-Powered Glioblastoma Segmentation</p>
        </div>
        <div class="patient-context">
          <select v-model="selectedPatientId" class="context-select" @change="loadPatientHistory">
            <option value="" disabled>Select Patient</option>
            <option v-for="p in patients" :key="p.userId" :value="p.userId">
              {{ p.fullName }} (ID: {{ p.userId }})
            </option>
          </select>
          <button class="btn-refresh" @click="fetchPatients" title="Refresh">↻</button>
        </div>
      </header>

      <div class="studio-grid">

        <div class="panel-column">
          <div class="tool-card upload-card">
            <h3>New Analysis</h3>
            <div
              class="drop-zone"
              @dragover.prevent
              @drop.prevent="handleDrop"
              @click="triggerFileInput"
              :class="{ 'has-file': selectedFile, 'processing': isProcessing }"
            >
              <input type="file" ref="fileInput" @change="handleFileSelect" accept=".zip" hidden>
              <div v-if="isProcessing">
                <div class="spinner"></div>
                <p class="status-text">{{ statusMessage }}</p>
              </div>
              <div v-else-if="selectedFile">
                <div class="file-icon">📄</div>
                <p class="file-name">{{ selectedFile.name }}</p>
                <p class="file-size">{{ (selectedFile.size / 1024 / 1024).toFixed(2) }} MB</p>
              </div>
              <div v-else>
                <div class="upload-icon">📂</div>
                <p>Drag .zip here or Click</p>
              </div>
            </div>
            <div class="action-footer">
              <button
                class="btn-primary"
                @click="startAnalysis"
                :disabled="!selectedFile || isProcessing || !selectedPatientId"
                :title="!selectedPatientId ? 'Select a patient first' : ''"
              >
                {{ isProcessing ? 'Processing...' : 'Start Analysis' }}
              </button>
            </div>
          </div>

          <div class="tool-card history-card">
            <h3>Patient History</h3>
            <div class="history-list-container">
              <ul class="history-list">
                <li
                  v-for="h in history"
                  :key="h.id"
                  @click="selectHistoryItem(h)"
                  :class="{ active: selectedCaseId === h.caseId }"
                >
                  <div class="h-info">
                    <span class="h-id">Scan #{{ h.id }}</span>
                    <span class="h-date">{{ h.analysisDate ? new Date(h.analysisDate).toLocaleDateString() : 'N/A' }}</span>
                  </div>
                  <span class="status-badge" :class="(h.status || 'completed').toLowerCase()">
                    {{ h.status || 'DONE' }}
                  </span>
                </li>
              </ul>
              <div v-if="history.length === 0" class="empty-state">
                {{ selectedPatientId ? 'No analysis history.' : 'Select a patient to see history.' }}
              </div>
            </div>
          </div>
        </div>

        <div class="viewer-column">
          <BrainSliceViewer
            v-if="selectedCaseId"
            :caseId="selectedCaseId"
            :indexRelpath="previewIndexRelpath"
          />
          <div v-else class="empty-viewer">
            <div class="empty-content">
              <span class="empty-icon">🖼️</span>
              <p>Run analysis to view 2D Slices</p>
            </div>
          </div>
        </div>

        <div class="viewer-column">
          <BrainMeshViewer
            v-if="selectedCaseId"
            :url="meshUrl"
          />
          <div v-else class="empty-viewer">
            <div class="empty-content">
              <span class="empty-icon">🧊</span>
              <p>Run analysis to view 3D Model</p>
            </div>
          </div>
        </div>

      </div>
    </main>
  </div>
</template>

<script setup>
import { JAVA_API_URL, PYTHON_API_URL } from '../config.js'
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import AppSidebar      from '@/components/AppSidebar.vue'
import BrainSliceViewer from './BrainSliceViewer.vue'
import BrainMeshViewer  from './BrainMeshViewer.vue'

const router     = useRouter()
const JAVA_API   = JAVA_API_URL
const PYTHON_API = PYTHON_API_URL

const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))

const patients          = ref([])
const selectedPatientId = ref('')
const history           = ref([])
const selectedCaseId    = ref(null)
const previewIndexRelpath = ref('')
const meshUrl           = ref('')
const isProcessing      = ref(false)
const statusMessage     = ref('')
const selectedFile      = ref(null)
const fileInput         = ref(null)

function handleNav(tab)   { router.push(tab.route) }
function handleLogout()   { localStorage.removeItem('user'); router.push('/') }

async function fetchPatients() {
  try {
    const res = await axios.get(`${JAVA_API}/patientProfiles/list`)
    patients.value = res.data
  } catch (e) { patients.value = [] }
}

async function loadPatientHistory() {
  if (!selectedPatientId.value) return
  try {
    const res = await axios.get(`${JAVA_API}/api/brain/patient/${selectedPatientId.value}`)
    history.value = res.data
  } catch { history.value = [] }
}

function triggerFileInput() { fileInput.value.click() }

function handleFileSelect(e) {
  const file = e.target.files[0]
  if (file) validateAndSetFile(file)
}
function handleDrop(e) {
  const file = e.dataTransfer.files[0]
  if (file) validateAndSetFile(file)
}
function validateAndSetFile(file) {
  if (!file.name.endsWith('.zip')) { alert('Please select a .zip file'); return }
  selectedFile.value  = file
  statusMessage.value = 'Ready to start'
}

async function startAnalysis() {
  if (!selectedFile.value) return
  if (!selectedPatientId.value) { alert('Please select a patient first!'); return }

  isProcessing.value  = true
  statusMessage.value = 'Uploading to AI Engine...'
  const formData = new FormData()
  formData.append('file', selectedFile.value)

  try {
    const res    = await axios.post(`${PYTHON_API}/api/brain/start`, formData)
    const caseId = res.data.case_id
    statusMessage.value = 'AI Segmentation Running...'
    pollStatus(caseId)
  } catch (e) {
    statusMessage.value = 'Error: ' + (e.response?.data?.error || e.message)
    isProcessing.value  = false
  }
}

async function pollStatus(caseId) {
  const interval = setInterval(async () => {
    try {
      const res    = await axios.get(`${PYTHON_API}/api/brain/status/${caseId}`)
      const status = res.data.status
      if (status === 'finished') {
        clearInterval(interval)
        await finalizeAnalysis(caseId)
      } else if (status === 'error') {
        clearInterval(interval)
        isProcessing.value  = false
        statusMessage.value = 'AI Analysis Failed: ' + res.data.error
      } else {
        statusMessage.value = `Processing: ${status}...`
      }
    } catch {
      clearInterval(interval)
      isProcessing.value  = false
      statusMessage.value = 'Lost connection to AI Engine'
    }
  }, 2000)
}

async function finalizeAnalysis(caseId) {
  statusMessage.value = 'Saving results...'
  const patient = patients.value.find(p => p.userId === selectedPatientId.value)
  const record  = {
    patientId:    selectedPatientId.value,
    patientName:  patient ? patient.fullName : 'Unknown',
    caseId,
    status:       'COMPLETED',
    analysisDate: new Date().toISOString(),
  }
  try {
    await axios.post(`${JAVA_API}/api/brain/link`, record)
    isProcessing.value  = false
    statusMessage.value = 'Complete!'
    selectedFile.value  = null
    await loadPatientHistory()
    loadCase(caseId)
  } catch {
    statusMessage.value = 'AI Done, but Database Save Failed.'
    isProcessing.value  = false
  }
}

function loadCase(caseId) {
  selectedCaseId.value       = caseId
  previewIndexRelpath.value  = 'output/preview/index.json'
  meshUrl.value              = `${PYTHON_API}/api/case/${caseId}/download/brain_mesh.glb`
}

function selectHistoryItem(item) { loadCase(item.caseId) }

onMounted(async () => {
  const stored = localStorage.getItem('user')
  if (stored) user.value = JSON.parse(stored)
  await fetchPatients()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&display=swap');

.dashboard-layout { display: flex; height: 100vh; width: 100vw; background: #0b1020; color: #e2e8f0; font-family: 'DM Sans', sans-serif; overflow: hidden; }
.main-content { flex: 1; display: flex; flex-direction: column; height: 100vh; overflow: hidden; }

.studio-header {
  padding: 18px 28px;
  background: #0f172a;
  border-bottom: 1px solid #1e293b;
  display: flex; justify-content: space-between; align-items: center; flex-shrink: 0;
}
.studio-header h2 { margin: 0; font-size: 1.2rem; font-weight: 700; }
.subtitle { color: #64748b; font-size: 0.85rem; margin: 3px 0 0; }

.patient-context { display: flex; gap: 8px; }
.context-select {
  background: #1e293b; border: 1px solid #334155; color: white;
  padding: 8px 12px; border-radius: 6px; min-width: 200px; font-size: 0.875rem;
}
.btn-refresh {
  background: #334155; color: white; border: none;
  padding: 0 14px; border-radius: 6px; cursor: pointer; font-size: 1.1rem;
}

.studio-grid { flex: 1; display: grid; grid-template-columns: 290px 1fr 1fr; gap: 18px; padding: 18px; overflow: hidden; }
.panel-column { display: flex; flex-direction: column; gap: 18px; min-height: 0; }
.viewer-column { height: 100%; min-height: 0; display: flex; flex-direction: column; }

.tool-card { background: #1e293b; border-radius: 10px; border: 1px solid #334155; padding: 14px; display: flex; flex-direction: column; }
.tool-card h3 { margin: 0 0 12px; font-size: 0.9rem; font-weight: 700; color: #f1f5f9; }
.upload-card { flex-shrink: 0; }
.history-card { flex: 1; min-height: 0; }

.drop-zone {
  background: #0f172a; border: 2px dashed #475569; border-radius: 8px;
  padding: 20px 10px; text-align: center; cursor: pointer; transition: 0.2s; margin-bottom: 12px;
  min-height: 100px; display: flex; flex-direction: column; justify-content: center; align-items: center;
}
.drop-zone:hover { border-color: #3b82f6; background: rgba(59,130,246,0.05); }
.drop-zone.has-file { border-color: #10b981; border-style: solid; }
.drop-zone.processing { border-color: #eab308; cursor: wait; }
.upload-icon { font-size: 2rem; margin-bottom: 5px; opacity: 0.6; }
.file-name { font-weight: 600; color: #f1f5f9; word-break: break-all; margin: 0; font-size: 0.85rem; }
.file-size { color: #94a3b8; font-size: 0.78rem; }
.status-text { color: #eab308; font-size: 0.85rem; margin-top: 5px; }

.action-footer { display: flex; }
.btn-primary { width: 100%; background: #2563eb; color: white; border: none; padding: 10px; border-radius: 6px; font-weight: 600; cursor: pointer; transition: 0.2s; font-size: 0.875rem; }
.btn-primary:hover:not(:disabled) { background: #1d4ed8; }
.btn-primary:disabled { background: #475569; cursor: not-allowed; opacity: 0.7; }

.spinner { width: 22px; height: 22px; border: 3px solid #eab308; border-top-color: transparent; border-radius: 50%; animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.history-list-container { flex: 1; overflow-y: auto; }
.history-list { list-style: none; padding: 0; margin: 0; }
.history-list li { padding: 10px 12px; background: #0f172a; border-radius: 7px; margin-bottom: 6px; cursor: pointer; border: 1px solid transparent; display: flex; justify-content: space-between; align-items: center; }
.history-list li:hover { border-color: #475569; }
.history-list li.active { border-color: #3b82f6; background: #172554; }
.h-info { display: flex; flex-direction: column; }
.h-id { font-weight: 600; font-size: 0.82rem; color: #f1f5f9; }
.h-date { font-size: 0.72rem; color: #94a3b8; }
.status-badge { font-size: 0.68rem; padding: 2px 8px; border-radius: 4px; background: #334155; font-weight: 700; }
.empty-state { color: #64748b; font-style: italic; text-align: center; margin-top: 20px; font-size: 0.85rem; }

.empty-viewer { flex: 1; background: #111827; border: 1px dashed #374151; border-radius: 10px; display: flex; align-items: center; justify-content: center; }
.empty-content { text-align: center; color: #6b7280; }
.empty-icon { font-size: 2.5rem; display: block; margin-bottom: 8px; opacity: 0.5; }
</style>