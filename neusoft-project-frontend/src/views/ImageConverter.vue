<template>
  <div class="layout-container">

    <AppSidebar
      active-id="converter"
      :username="user.username || 'User'"
      @go-home="router.push('/doctor-dashboard')"
      @navigate="handleNav"
      @logout="logout"
    />

    <main class="content-area">
      <div class="tool-container">

        <header class="page-header">
          <div>
            <h2 class="title">Image Converter</h2>
            <p class="subtitle">Convert medical scans (DICOM, NIfTI) to standard formats.</p>
          </div>
        </header>

        <div class="card-wrapper">
          <div class="conversion-card">
            <h1 class="card-title">Medical Image Format Conversion</h1>
            <p class="card-subtitle">Upload a DICOM folder or single file, then select a target format.</p>

            <div class="top-bar">
              <button class="upload-btn" @click="triggerUploadFile">📄 Upload File</button>
              <button class="upload-btn" @click="triggerUploadFolder">📂 Upload Folder</button>
              <button class="refresh-btn" @click="fetchItems" :disabled="isLoadingList">
                {{ isLoadingList ? '...' : '🔄 Refresh List' }}
              </button>
              <input ref="fileInput"   type="file" class="hidden-input" @change="handleFileSelected" />
              <input ref="folderInput" type="file" webkitdirectory multiple class="hidden-input" @change="handleFolderSelected" />
            </div>

            <div class="field-group">
              <label class="field-label">Source Item</label>
              <select v-model="selectedPath" class="select-input">
                <option disabled value="">-- Choose file/folder --</option>
                <option v-for="item in items" :key="item.path" :value="item.path">
                  {{ item.type === 'folder' ? '📂' : '📄' }} {{ item.path }}
                </option>
              </select>
            </div>

            <div class="field-group">
              <label class="field-label">Target Format</label>
              <select v-model="selectedFormat" class="select-input">
                <option value="png">PNG Image</option>
                <option value="jpg">JPEG Image</option>
                <option value="nii">NIfTI Volume</option>
                <option value="nrrd">NRRD Volume</option>
              </select>
            </div>

            <div class="actions">
              <button class="primary-btn" @click="convertFormat" :disabled="!selectedPath || isConverting">
                {{ isConverting ? 'Converting...' : 'Start Conversion' }}
              </button>
            </div>

            <div v-if="convertMessage" :class="['status', isError ? 'error' : 'success']">
              {{ convertMessage }}
            </div>
          </div>
        </div>

      </div>
    </main>
  </div>
</template>

<script setup>
import { PYTHON_API_URL } from '../config.js'
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import AppSidebar from '@/components/AppSidebar.vue'

const router = useRouter()
const user   = ref({})

function handleNav(tab) {
  if (tab.route === '/image-converter') return
  router.push(tab.route)
}
function logout() { localStorage.removeItem('user'); router.push('/') }

const items          = ref([])
const selectedPath   = ref('')
const selectedFormat = ref('png')
const convertMessage = ref('')
const isLoadingList  = ref(false)
const isConverting   = ref(false)
const isError        = ref(false)
const fileInput      = ref(null)
const folderInput    = ref(null)

async function fetchItems() {
  isLoadingList.value = true
  try {
    const res = await axios.get(`${PYTHON_API_URL}/list-items`)
    items.value = res.data.items || []
    if (!selectedPath.value && items.value.length)
      selectedPath.value = items.value[0].path
  } catch { /* silent */ }
  finally { isLoadingList.value = false }
}

function triggerUploadFile()   { fileInput.value.click() }
function triggerUploadFolder() { folderInput.value.click() }

async function handleFileSelected(e) {
  const f = e.target.files[0]; if (!f) return
  const fd = new FormData(); fd.append('file', f)
  try {
    await axios.post(`${PYTHON_API_URL}/upload`, fd)
    convertMessage.value = 'File uploaded successfully!'
    isError.value = false
    await fetchItems()
  } catch { convertMessage.value = 'Upload failed.'; isError.value = true }
}

async function handleFolderSelected(e) {
  const fs = e.target.files; if (!fs.length) return
  const fd = new FormData()
  for (const f of fs) fd.append('files', f)
  try {
    await axios.post(`${PYTHON_API_URL}/upload-folder`, fd)
    convertMessage.value = 'Folder uploaded successfully!'
    isError.value = false
    await fetchItems()
  } catch { convertMessage.value = 'Upload failed.'; isError.value = true }
}

async function convertFormat() {
  if (!selectedPath.value) return
  isConverting.value   = true
  convertMessage.value = ''
  isError.value        = false
  try {
    const res = await axios.post(`${PYTHON_API_URL}/convert`, {
      path: selectedPath.value,
      format: selectedFormat.value,
    })
    convertMessage.value = res.data.message + '\n' + res.data.output_path
  } catch (e) {
    convertMessage.value = 'Error: ' + (e.response?.data?.error || e.message)
    isError.value = true
  } finally { isConverting.value = false }
}

onMounted(() => {
  const s = localStorage.getItem('user')
  if (s) user.value = JSON.parse(s)
  fetchItems()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&display=swap');

.layout-container { display: flex; height: 100vh; width: 100vw; background: #0b1120; font-family: 'DM Sans', sans-serif; color: #e5e7eb; }
.content-area { flex: 1; overflow-y: auto; padding: 24px; }
.tool-container { max-width: 800px; margin: 0 auto; }

.page-header { margin-bottom: 24px; }
.title    { font-size: 1.5rem; font-weight: 700; margin: 0; color: white; }
.subtitle { color: #94a3b8; margin: 4px 0 0; font-size: 0.875rem; }

.card-wrapper { display: flex; justify-content: center; }
.conversion-card {
  width: 100%; max-width: 600px;
  background: #ffffff; border-radius: 16px;
  padding: 28px; box-shadow: 0 20px 60px rgba(0,0,0,0.4);
  color: #0f172a;
}
.card-title    { font-size: 1.5rem; margin: 0 0 6px; color: #0f172a; font-weight: 700; }
.card-subtitle { margin: 0 0 20px; font-size: 0.9rem; color: #64748b; line-height: 1.5; }

.top-bar { display: flex; gap: 8px; margin-bottom: 18px; flex-wrap: wrap; }
.upload-btn {
  background: white; border: 1px solid #cbd5e1; color: #334155;
  padding: 7px 14px; border-radius: 6px; cursor: pointer; font-size: 0.85rem; font-weight: 500; transition: all 0.15s;
}
.upload-btn:hover { border-color: #3b82f6; color: #2563eb; background: #eff6ff; }
.refresh-btn {
  padding: 7px 14px; border-radius: 6px; border: 1px solid #cbd5e1;
  background: #f8fafc; font-size: 0.85rem; font-weight: 500; color: #0f172a; cursor: pointer;
}
.hidden-input { display: none; }

.field-group { margin-bottom: 16px; }
.field-label { display: block; font-size: 0.82rem; font-weight: 600; color: #475569; margin-bottom: 6px; }
.select-input {
  width: 100%; padding: 10px 12px; border-radius: 8px;
  border: 1px solid #d1d5db; font-size: 0.875rem;
  color: #0f172a; background: #f9fafb; outline: none;
  font-family: 'DM Sans', sans-serif;
}
.select-input:focus { border-color: #3b82f6; }

.actions { margin: 8px 0; }
.primary-btn {
  width: 100%; padding: 12px; border-radius: 999px; border: none;
  font-size: 0.9rem; font-weight: 700; cursor: pointer;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: white; transition: all 0.15s; font-family: 'DM Sans', sans-serif;
}
.primary-btn:hover:not(:disabled) { opacity: 0.9; transform: translateY(-1px); }
.primary-btn:disabled { opacity: 0.55; cursor: default; }

.status { margin-top: 8px; font-size: 0.875rem; font-weight: 500; white-space: pre-line; }
.status.success { color: #15803d; }
.status.error   { color: #b91c1c; }
</style>