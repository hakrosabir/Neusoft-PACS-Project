<template>
  <div class="dashboard-layout" style="background:#080f1e;color:#e2e8f0;min-height:100vh;">

    <AppSidebar
      active-id="spleen"
      :username="user.username || 'Radiologist'"
      @go-home="router.push('/doctor-dashboard')"
      @navigate="handleNav"
      @logout="logout"
    />

    <div class="main-wrapper">

      <!-- ── HEADER ─────────────────────────────────────────────── -->
      <header class="ct-header">
        <div class="header-inner">
          <div class="title-block">
            <div class="title-row">
              <span class="organ-badge">🫀 CT Spleen / ICH Analysis</span>
              <span class="pipeline-tag">AI Segmentation</span>
            </div>
            <p class="subtitle">Upload NIfTI or DICOM · AI runs segmentation · Save results as PNG</p>
          </div>
          <div class="upload-row">
            <label class="file-label">
              <input type="file" class="hidden-input" @change="onFileChange" />
              <span v-if="!selectedFile" class="file-cta">📂 Choose file (.nii / .nii.gz / .zip)</span>
              <span v-else class="file-chosen">📄 {{ selectedFile.name }}</span>
            </label>
            <button class="upload-btn" :disabled="loading" @click="onUpload">
              <span v-if="loading" class="btn-spinner"></span>
              {{ loading ? 'Analysing...' : '▶ Run Analysis' }}
            </button>
          </div>
        </div>
        <div v-if="error"   class="error-bar">⚠ {{ error }}</div>
        <div v-if="loading" class="progress-bar"><div class="progress-fill"></div></div>
      </header>

      <!-- ── BODY ───────────────────────────────────────────────── -->
      <div class="body-scroll">

        <template v-if="resultsReady">

          <!-- Case info strip -->
          <div class="case-strip">
            <div class="cs-item"><span class="cs-label">Case ID</span><span class="cs-val mono">{{ caseId }}</span></div>
            <div class="cs-item"><span class="cs-label">Scan Date</span><span class="cs-val">{{ scanDate }}</span></div>
            <div class="cs-item"><span class="cs-label">Axial</span><span class="cs-val">{{ sliceCount('axial') }} slices</span></div>
            <div class="cs-item"><span class="cs-label">Coronal</span><span class="cs-val">{{ sliceCount('coronal') }} slices</span></div>
            <div class="cs-item"><span class="cs-label">Sagittal</span><span class="cs-val">{{ sliceCount('sagittal') }} slices</span></div>
            <div class="cs-item">
              <span class="cs-label">3D Mesh</span>
              <span :class="['cs-val', meshUrl ? 'ok' : 'na']">{{ meshUrl ? '✔ Ready' : '✗ N/A' }}</span>
            </div>
          </div>

          <!-- ── VIEWERS ROW ──────────────────────────────────── -->
          <div class="viewers-row">

            <!-- 2D Slice Viewer -->
            <div class="viewer-card">
              <div class="vcard-header">
                <div class="vcard-left">
                  <span class="dot dot-blue"></span>
                  <span class="vcard-title">2D Slice Viewer</span>
                  <span class="vcard-sub">{{ plane.toUpperCase() }} · {{ slice + 1 }}/{{ maxSlice + 1 }}</span>
                </div>
                <div class="vcard-right">
                  <div class="plane-tabs">
                    <button
                      v-for="p in ['axial','coronal','sagittal']"
                      :key="p"
                      :class="['ptab', { active: plane === p }]"
                      @click="switchPlane(p)"
                    >{{ p }}</button>
                  </div>
                  <button class="save-btn" @click="saveSliceImage" title="Download current slice as PNG">
                    💾 Save PNG
                  </button>
                </div>
              </div>

              <div class="slice-frame">
                <img v-if="currentSrc" :src="currentSrc" class="slice-img" />
                <div v-else class="no-preview">No preview for {{ plane }}</div>
                <div class="slice-overlay-badge">{{ plane }} · {{ slice + 1 }} / {{ maxSlice + 1 }}</div>
              </div>

              <div class="slice-controls">
                <button class="cbtn" @click="stepSlice(-10)" title="Back 10">«</button>
                <button class="cbtn" @click="stepSlice(-1)"  title="Back 1">‹</button>
                <input
                  type="range" class="srange"
                  :min="0" :max="maxSlice" :value="slice"
                  @input="slice = Number($event.target.value)"
                />
                <button class="cbtn" @click="stepSlice(1)"   title="Forward 1">›</button>
                <button class="cbtn" @click="stepSlice(10)"  title="Forward 10">»</button>
              </div>

              <!-- Thumbnail strip -->
              <div v-if="previewThumbs.length" class="thumb-strip">
                <div
                  v-for="(url, i) in previewThumbs"
                  :key="i"
                  class="thumb"
                  :class="{ 'thumb-active': currentSrc === url }"
                  @click="jumpToPreview(url)"
                  :title="`Jump to slice`"
                >
                  <img :src="url" />
                </div>
              </div>
            </div>

            <!-- 3D Mesh Viewer -->
            <div class="viewer-card">
              <div class="vcard-header">
                <div class="vcard-left">
                  <span class="dot dot-green"></span>
                  <span class="vcard-title">3D Segmentation Mesh</span>
                  <span class="vcard-sub">PLY · Three.js</span>
                </div>
                <div class="vcard-right">
                  <span class="hint-text">🖱 Drag · Scroll · Right-click</span>
                  <button class="save-btn" @click="save3DSnapshot" title="Download 3D view as PNG">
                    💾 Save PNG
                  </button>
                </div>
              </div>
              <div class="mesh-frame" id="mesh-frame">
                <MeshViewer :mesh-url="meshUrl" />
              </div>
            </div>

          </div><!-- /viewers-row -->

        </template>

        <!-- ── EMPTY STATE ─────────────────────────────────────── -->
        <div v-else class="empty-state">
          <div class="pulse-wrap">
            <div class="pulse-ring"></div>
            <span class="empty-emoji">🫀</span>
          </div>
          <h3>No Scan Loaded</h3>
          <p>Upload a CT scan above to run AI spleen segmentation</p>
          <p class="esub">Supported: <strong>.nii</strong> · <strong>.nii.gz</strong> · <strong>.zip (DICOM)</strong></p>
        </div>

      </div><!-- /body-scroll -->
    </div><!-- /main-wrapper -->
  </div>
</template>

<script setup>
import { PYTHON_API_URL } from '../config.js'
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import AppSidebar from '@/components/AppSidebar.vue'
import MeshViewer from '@/components/MeshViewer.vue'

const router     = useRouter()
const PYTHON_API = PYTHON_API_URL

// ── User ──────────────────────────────────────────────────────────────
const user = ref({})
onMounted(() => {
  const s = localStorage.getItem('user')
  if (s) user.value = JSON.parse(s)
})

function handleNav(tab) { if (tab.route !== '/spleen-analysis') router.push(tab.route) }
function logout()        { localStorage.removeItem('user'); router.push('/') }

// ── Upload state ──────────────────────────────────────────────────────
const selectedFile = ref(null)
const loading      = ref(false)
const error        = ref('')
const resultsReady = ref(false)
const caseId       = ref('')
const scanDate     = ref('')
const previews     = ref(null)
const meshUrl      = ref('')

function onFileChange(e) {
  selectedFile.value = e.target.files?.[0] || null
  error.value = ''
}

function toAbsolute(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return PYTHON_API + (url.startsWith('/') ? '' : '/') + url
}

async function onUpload() {
  error.value        = ''
  previews.value     = null
  meshUrl.value      = ''
  resultsReady.value = false
  caseId.value       = ''

  if (!selectedFile.value) { error.value = 'Please choose a file.'; return }

  loading.value = true
  try {
    const fd = new FormData()
    fd.append('file', selectedFile.value)

    const res     = await axios.post(`${PYTHON_API}/api/ct-spleen/submit`, fd)
    const outputs = res.data.outputs || {}

    caseId.value   = res.data.case_id || ''
    scanDate.value = new Date().toLocaleDateString()
    meshUrl.value  = toAbsolute(outputs.mesh_url || '')

    if (outputs.previews) {
      const fixed = {}
      for (const [p, urls] of Object.entries(outputs.previews)) {
        fixed[p] = Array.isArray(urls) ? urls.map(u => toAbsolute(u)) : []
      }
      previews.value = fixed
    }

    console.log('=== SPLEEN RESPONSE ===')
    console.log('case_id :', caseId.value)
    console.log('meshUrl :', meshUrl.value)
    console.log('previews:', JSON.stringify(previews.value))

    resultsReady.value = true
    plane.value = 'axial'
    slice.value = Math.floor((previews.value?.axial?.length || 1) / 2)

  } catch (e) {
    error.value = e?.response?.data?.error || e.message || 'Upload failed'
    console.error('Spleen error:', e)
  } finally {
    loading.value = false
  }
}

// ── Slice viewer ──────────────────────────────────────────────────────
const plane = ref('axial')
const slice = ref(0)

const list       = computed(() => previews.value?.[plane.value] ?? [])
const maxSlice   = computed(() => Math.max(0, list.value.length - 1))
const currentSrc = computed(() => list.value[slice.value] ?? '')

watch(plane, () => {
  slice.value = Math.floor((list.value.length || 1) / 2)
})

function switchPlane(p) { plane.value = p }
function stepSlice(n)   { slice.value = Math.max(0, Math.min(maxSlice.value, slice.value + n)) }
function sliceCount(p)  { return previews.value?.[p]?.length ?? 0 }

const previewThumbs = computed(() => {
  if (!previews.value) return []
  const all = [
    ...(previews.value.axial    || []),
    ...(previews.value.coronal  || []),
    ...(previews.value.sagittal || []),
  ]
  const step = Math.max(1, Math.floor(all.length / 8))
  return all.filter((_, i) => i % step === 0).slice(0, 8)
})

function jumpToPreview(url) {
  if (!previews.value) return
  for (const [p, urls] of Object.entries(previews.value)) {
    const idx = urls.indexOf(url)
    if (idx !== -1) { plane.value = p; slice.value = idx; return }
  }
}

// ── Save 2D slice ─────────────────────────────────────────────────────
function saveSliceImage() {
  if (!currentSrc.value) return
  const a = document.createElement('a')
  a.href     = currentSrc.value
  a.download = `spleen_${plane.value}_slice${slice.value + 1}.png`
  a.click()
}

// ── Save 3D snapshot ──────────────────────────────────────────────────
// Works because MeshViewer.vue now uses preserveDrawingBuffer: true
function save3DSnapshot() {
  const canvas = document.querySelector('#mesh-frame canvas')
  if (!canvas) {
    alert('3D mesh not loaded yet — wait for the mesh to appear then try again.')
    return
  }
  try {
    const dataUrl = canvas.toDataURL('image/png')
    const a = document.createElement('a')
    a.href     = dataUrl
    a.download = `spleen_3d_${caseId.value || 'snapshot'}.png`
    a.click()
  } catch (err) {
    console.error('3D snapshot failed:', err)
    alert('Could not save 3D snapshot. Check browser console for details.')
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Syne:wght@600;700;800&family=DM+Mono:wght@400;500&family=DM+Sans:wght@400;500;600&display=swap');

/* ── Layout ─────────────────────────────────────────────────────── */
.dashboard-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  background: #080f1e;
  font-family: 'DM Sans', sans-serif;
  color: #e2e8f0;
  overflow: hidden;
}
.main-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
  background: #080f1e;
}

/* ── Header ─────────────────────────────────────────────────────── */
.ct-header {
  background: #0d1829;
  border-bottom: 1px solid rgba(255,255,255,.06);
  padding: 14px 22px;
  flex-shrink: 0;
}
.header-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.title-row { display: flex; align-items: center; gap: 10px; margin-bottom: 3px; }
.organ-badge {
  font-family: 'Syne', sans-serif;
  font-weight: 700;
  font-size: 1rem;
  color: #f1f5f9;
}
.pipeline-tag {
  background: rgba(37,99,235,.2);
  color: #93c5fd;
  font-size: 0.66rem;
  font-weight: 700;
  letter-spacing: .7px;
  text-transform: uppercase;
  padding: 3px 8px;
  border-radius: 4px;
  border: 1px solid rgba(37,99,235,.35);
}
.subtitle { margin: 0; font-size: 0.77rem; color: #475569; }

.upload-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.file-label {
  cursor: pointer;
  background: #111f35;
  border: 1px dashed #334155;
  border-radius: 7px;
  padding: 8px 14px;
  font-size: 0.82rem;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: border-color .2s;
}
.file-label:hover { border-color: #2563eb; }
.hidden-input { display: none; }
.file-cta    { color: #60a5fa; font-weight: 500; }
.file-chosen { color: #e2e8f0; font-weight: 500; }

.upload-btn {
  background: #2563eb;
  color: white;
  border: none;
  padding: 9px 20px;
  border-radius: 7px;
  font-family: 'Syne', sans-serif;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 7px;
  transition: all .2s;
  white-space: nowrap;
}
.upload-btn:hover:not(:disabled) { background: #1d4ed8; transform: translateY(-1px); }
.upload-btn:disabled { opacity: .5; cursor: not-allowed; transform: none; }

.btn-spinner {
  width: 12px; height: 12px;
  border: 2px solid rgba(255,255,255,.35);
  border-top-color: white;
  border-radius: 50%;
  animation: spin .8s linear infinite;
  flex-shrink: 0;
}
@keyframes spin { to { transform: rotate(360deg); } }

.error-bar {
  margin-top: 10px;
  padding: 9px 14px;
  border: 1px solid #fca5a5;
  border-radius: 6px;
  background: rgba(239,68,68,.08);
  color: #fca5a5;
  font-size: 0.8rem;
}
.progress-bar {
  margin-top: 10px; height: 2px;
  background: #1e293b; border-radius: 2px; overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #2563eb, #38bdf8);
  animation: pslide 1.5s ease-in-out infinite;
}
@keyframes pslide {
  0%   { width: 0%;  margin-left: 0; }
  50%  { width: 60%; margin-left: 20%; }
  100% { width: 0%;  margin-left: 100%; }
}

/* ── Body scroll ─────────────────────────────────────────────────── */
.body-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: #080f1e;
}
.body-scroll::-webkit-scrollbar { width: 5px; }
.body-scroll::-webkit-scrollbar-track { background: #080f1e; }
.body-scroll::-webkit-scrollbar-thumb { background: #1e3a5f; border-radius: 3px; }

/* ── Case info strip ─────────────────────────────────────────────── */
.case-strip {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.cs-item {
  background: #0d1829;
  border: 1px solid rgba(255,255,255,.06);
  border-radius: 7px;
  padding: 7px 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 90px;
}
.cs-label { color: #475569; font-size: 0.62rem; font-weight: 700; text-transform: uppercase; letter-spacing: .5px; }
.cs-val   { color: #e2e8f0; font-size: 0.8rem; font-weight: 600; }
.mono     { font-family: 'DM Mono', monospace; font-size: 0.68rem; }
.ok       { color: #22c55e; }
.na       { color: #f87171; }

/* ── Viewers row ─────────────────────────────────────────────────── */
.viewers-row {
  display: grid;
  grid-template-columns: 440px 1fr;
  gap: 14px;
  flex: 1;
}
@media (max-width: 1200px) { .viewers-row { grid-template-columns: 1fr; } }

/* ── Viewer card ─────────────────────────────────────────────────── */
.viewer-card {
  background: #0d1829;
  border: 1px solid rgba(255,255,255,.06);
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 500px;
}
.vcard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 9px 13px;
  border-bottom: 1px solid rgba(255,255,255,.05);
  background: rgba(255,255,255,.02);
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 6px;
}
.vcard-left  { display: flex; align-items: center; gap: 7px; }
.vcard-right { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

.dot       { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.dot-blue  { background: #38bdf8; box-shadow: 0 0 6px #38bdf8; }
.dot-green { background: #22c55e; box-shadow: 0 0 6px #22c55e; }

.vcard-title { font-family: 'Syne', sans-serif; font-weight: 700; font-size: 0.85rem; color: #f1f5f9; }
.vcard-sub   { color: #475569; font-size: 0.7rem; letter-spacing: .3px; }
.hint-text   { color: #334155; font-size: 0.68rem; }

/* Plane tabs */
.plane-tabs { display: flex; gap: 3px; }
.ptab {
  padding: 4px 9px;
  border-radius: 5px;
  border: 1px solid transparent;
  background: transparent;
  color: #475569;
  font-size: 0.7rem;
  font-weight: 600;
  cursor: pointer;
  text-transform: capitalize;
  transition: all .15s;
}
.ptab:hover  { background: rgba(255,255,255,.05); color: #cbd5e1; }
.ptab.active { background: rgba(37,99,235,.2); color: #93c5fd; border-color: rgba(37,99,235,.4); }

/* Save button */
.save-btn {
  padding: 5px 12px;
  border-radius: 6px;
  border: 1px solid #334155;
  background: #111f35;
  color: #94a3b8;
  font-size: 0.72rem;
  font-weight: 700;
  cursor: pointer;
  transition: all .15s;
  white-space: nowrap;
}
.save-btn:hover { border-color: #2563eb; color: #93c5fd; background: rgba(37,99,235,.12); }

/* Slice frame */
.slice-frame {
  flex: 1;
  position: relative;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 380px;
}
.slice-img { width: 100%; height: 100%; object-fit: contain; display: block; }
.no-preview { color: #475569; font-style: italic; font-size: 0.85rem; }
.slice-overlay-badge {
  position: absolute;
  bottom: 8px; left: 8px;
  background: rgba(0,0,0,.7);
  color: #94a3b8;
  font-family: 'DM Mono', monospace;
  font-size: 0.68rem;
  padding: 3px 8px;
  border-radius: 4px;
  pointer-events: none;
}

/* Slice controls */
.slice-controls {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 9px 12px;
  border-top: 1px solid rgba(255,255,255,.05);
  flex-shrink: 0;
}
.cbtn {
  width: 28px; height: 28px;
  border-radius: 5px;
  border: 1px solid #334155;
  background: #111f35;
  color: #e2e8f0;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all .15s;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.cbtn:hover { border-color: #2563eb; color: #93c5fd; }
.srange {
  flex: 1;
  accent-color: #2563eb;
  height: 4px;
  cursor: pointer;
}

/* Thumbnail strip */
.thumb-strip {
  display: flex;
  gap: 5px;
  padding: 8px 12px;
  border-top: 1px solid rgba(255,255,255,.05);
  flex-shrink: 0;
  overflow-x: auto;
  scrollbar-width: none;
}
.thumb-strip::-webkit-scrollbar { display: none; }
.thumb {
  width: 52px; height: 52px;
  border-radius: 5px;
  overflow: hidden;
  border: 2px solid transparent;
  cursor: pointer;
  transition: border-color .15s;
  background: #000;
  flex-shrink: 0;
}
.thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
.thumb:hover, .thumb-active { border-color: #2563eb; }

/* Mesh frame */
.mesh-frame { flex: 1; min-height: 460px; }

/* ── Empty state ─────────────────────────────────────────────────── */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #475569;
  gap: 10px;
  padding: 60px;
  text-align: center;
}
.pulse-wrap {
  position: relative;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 14px;
}
.empty-emoji { font-size: 4rem; position: relative; z-index: 1; }
.pulse-ring {
  position: absolute;
  width: 100px; height: 100px;
  border-radius: 50%;
  border: 2px solid rgba(37,99,235,.3);
  animation: pulse-ring 2s ease-out infinite;
}
@keyframes pulse-ring {
  0%   { transform: scale(.8); opacity: .8; }
  100% { transform: scale(1.6); opacity: 0; }
}
.empty-state h3 { margin: 0; font-family: 'Syne', sans-serif; font-size: 1.1rem; color: #94a3b8; }
.empty-state p  { margin: 0; font-size: 0.86rem; }
.esub { font-size: 0.76rem; color: #334155; }
.esub strong { color: #60a5fa; }
</style>