<template>
  <div class="layout-container">

    <AppSidebar
      active-id="diagnosis"
      :username="user.username || 'User'"
      @go-home="goHome"
      @navigate="handleNav"
      @logout="logout"
    />

    <main class="main-content">
      <div class="viewer-page">

        <DiagnosisControls
          :items="items"
          v-model:selectedPath="selectedPath"
          v-model:segPath="segPath"
          :isLoadingList="isLoadingList"
          :isLoadingVolume="isLoadingVolume"
          :isLoadingMesh="isLoadingMesh"
          :statusMessage="statusMessage"
          @upload-ct-file="triggerCtFileSelect"
          @upload-ct-folder="triggerCtFolderSelect"
          @upload-seg-file="triggerSegFileSelect"
          @refresh="fetchItems"
          @load-volume="loadVolume"
          @load-mesh="loadMesh"
        />

        <DiagnosisViewer
          ref="viewer"
          :shape="shape"
          :selectedPath="selectedPath"
          :segPath="segPath"
          :initAxialImg="initAxialImg"
          :initSagittalImg="initSagittalImg"
          :initCoronalImg="initCoronalImg"
          :initAxialSegImg="initAxialSegImg"
          :initSagittalSegImg="initSagittalSegImg"
          :initCoronalSegImg="initCoronalSegImg"
          :initAxialIndex="initAxialIndex"
          :initSagittalIndex="initSagittalIndex"
          :initCoronalIndex="initCoronalIndex"
          @status="statusMessage = $event"
        />

      </div>
    </main>

    <!-- Hidden file inputs -->
    <input ref="ctFileInput"   type="file"                        class="hidden-input" @change="handleCtFileSelected" />
    <input ref="ctFolderInput" type="file" webkitdirectory multiple class="hidden-input" @change="handleCtFolderSelected" />
    <input ref="segFileInput"  type="file"                        class="hidden-input" @change="handleSegFileSelected" />
  </div>
</template>

<script setup>
import { PYTHON_API_URL } from '../config.js'
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import AppSidebar        from '@/components/AppSidebar.vue'
import DiagnosisControls from '@/components/DiagnosisControls.vue'
import DiagnosisViewer   from '@/components/DiagnosisViewer.vue'

const router      = useRouter()
const PYTHON_BASE = PYTHON_API_URL
const user        = ref({})

const items          = ref([])
const isLoadingList  = ref(false)
const selectedPath   = ref('')
const segPath        = ref('')
const isLoadingVolume = ref(false)
const isLoadingMesh   = ref(false)
const statusMessage   = ref('')
const shape           = ref(null)

const initAxialImg    = ref(null)
const initSagittalImg = ref(null)
const initCoronalImg  = ref(null)
const initAxialSegImg    = ref(null)
const initSagittalSegImg = ref(null)
const initCoronalSegImg  = ref(null)
const initAxialIndex    = ref(0)
const initSagittalIndex = ref(0)
const initCoronalIndex  = ref(0)

const ctFileInput   = ref(null)
const ctFolderInput = ref(null)
const segFileInput  = ref(null)
const viewer        = ref(null)

function goHome()  { router.push('/doctor-dashboard') }
function logout()  { localStorage.removeItem('user'); router.push('/') }
function handleNav(tab) {
  if (tab.route === '/diagnosis') return
  router.push(tab.route)
}

async function fetchItems() {
  isLoadingList.value = true
  try {
    const res = await axios.get(`${PYTHON_BASE}/list-items`)
    items.value = Array.isArray(res.data.items) ? res.data.items : []
    if (!selectedPath.value && items.value.length)
      selectedPath.value = items.value[0].path
  } catch (err) {
    statusMessage.value = 'Failed to load list: ' + err.message
  } finally { isLoadingList.value = false }
}

function triggerCtFileSelect()   { ctFileInput.value?.click() }
function triggerCtFolderSelect() { ctFolderInput.value?.click() }
function triggerSegFileSelect()  { segFileInput.value?.click() }

async function handleCtFolderSelected(e) {
  const files = e.target.files; if (!files?.length) return
  try {
    const form = new FormData()
    for (const f of files) form.append('files', f)
    await axios.post(`${PYTHON_BASE}/upload-folder`, form)
    statusMessage.value = 'CT folder uploaded.'
    await fetchItems()
  } catch (err) { statusMessage.value = 'Failed to upload CT folder: ' + err.message }
  finally { e.target.value = '' }
}
async function handleCtFileSelected(e) {
  const file = e.target.files?.[0]; if (!file) return
  try {
    const form = new FormData(); form.append('file', file)
    await axios.post(`${PYTHON_BASE}/upload`, form)
    statusMessage.value = 'CT file uploaded.'
    await fetchItems()
  } catch (err) { statusMessage.value = 'Failed to upload CT file: ' + err.message }
  finally { e.target.value = '' }
}
async function handleSegFileSelected(e) {
  const file = e.target.files?.[0]; if (!file) return
  try {
    const form = new FormData(); form.append('file', file)
    await axios.post(`${PYTHON_BASE}/upload`, form)
    statusMessage.value = 'Segmentation file uploaded.'
    await fetchItems()
  } catch (err) { statusMessage.value = 'Failed to upload seg file: ' + err.message }
  finally { e.target.value = '' }
}

function b64ToDataUrl(b64) { return b64 ? 'data:image/png;base64,' + b64 : null }

async function loadVolume() {
  if (!selectedPath.value) return
  isLoadingVolume.value = true; statusMessage.value = ''
  try {
    const res = await axios.post(`${PYTHON_BASE}/viewer/init`, {
      path: selectedPath.value, seg_path: segPath.value, ww: 400, wl: 40
    })
    if (res.data.error) throw new Error(res.data.error)
    shape.value             = res.data.shape
    initAxialIndex.value    = res.data.mid_indices.z
    initSagittalIndex.value = res.data.mid_indices.x
    initCoronalIndex.value  = res.data.mid_indices.y
    initAxialImg.value      = b64ToDataUrl(res.data.axial_png)
    initSagittalImg.value   = b64ToDataUrl(res.data.sagittal_png)
    initCoronalImg.value    = b64ToDataUrl(res.data.coronal_png)
    initAxialSegImg.value    = b64ToDataUrl(res.data.axial_seg_png)
    initSagittalSegImg.value = b64ToDataUrl(res.data.sagittal_seg_png)
    initCoronalSegImg.value  = b64ToDataUrl(res.data.coronal_seg_png)
    statusMessage.value = 'CT volume loaded.'
  } catch (err) { statusMessage.value = 'Failed to load CT volume: ' + err.message }
  finally { isLoadingVolume.value = false }
}

async function loadMesh() {
  if (!segPath.value) { statusMessage.value = 'Please select a segmentation first.'; return }
  isLoadingMesh.value = true; statusMessage.value = ''
  try {
    const res = await axios.post(`${PYTHON_BASE}/viewer/seg3d`, { seg_path: segPath.value })
    if (res.data.error) throw new Error(res.data.error)
    viewer.value?.applyMeshData(res.data.vertices, res.data.faces)
    statusMessage.value = '3D mesh loaded.'
  } catch (err) { statusMessage.value = 'Failed to load 3D segmentation: ' + err.message }
  finally { isLoadingMesh.value = false }
}

onMounted(async () => {
  const stored = localStorage.getItem('user')
  if (stored) user.value = JSON.parse(stored)
  await fetchItems()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&display=swap');

.layout-container {
  display: flex; height: 100vh; width: 100vw;
  background: #0b1120;
  font-family: 'DM Sans', sans-serif;
  overflow: hidden;
}
.main-content { flex: 1; overflow-y: auto; }
.viewer-page {
  padding: 24px;
  background: #0b1120;
  min-height: 100%;
  box-sizing: border-box;
  color: #e5e7eb;
}
.hidden-input { display: none; }
</style>