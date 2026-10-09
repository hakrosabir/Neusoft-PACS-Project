<script setup>
import { PYTHON_API_URL } from '../config.js'
import { ref, watch, computed } from 'vue'
import axios from 'axios'

const props = defineProps({
  caseId: { type: String, required: true },
  indexRelpath: { type: String, required: true },
})

const index = ref(null)
const plane = ref('axial')
const slice = ref(0)
const opacity = ref(0.5)
const error = ref('')

// CONFIG: Point to your Python Backend
const PYTHON_API = PYTHON_API_URL

function downloadUrl(relpath) {
  const cleanPath = String(relpath).replace(/\\/g, '/')
  return `${PYTHON_API}/api/case/${props.caseId}/download/${cleanPath}`
}

async function loadIndex() {
  error.value = ''
  index.value = null
  try {
    const res = await axios.get(downloadUrl(props.indexRelpath))
    index.value = res.data
    const n = index.value?.planes?.axial?.num_slices ?? 1
    plane.value = 'axial'
    slice.value = Math.floor(n / 2)
  } catch (e) {
    console.error(e)
    error.value = 'Failed to load analysis index.'
  }
}

watch(() => [props.caseId, props.indexRelpath], loadIndex, { immediate: true })

const maxSlice = computed(() => {
  const n = index.value?.planes?.[plane.value]?.num_slices ?? 1
  return Math.max(0, n - 1)
})

watch(plane, () => {
  const n = index.value?.planes?.[plane.value]?.num_slices ?? 1
  slice.value = Math.floor(n / 2)
})

function sliceName(prefix) {
  if (!index.value) return ''
  const z = String(slice.value).padStart(index.value.zero_pad, '0')
  return `${prefix}${z}${index.value.ext}`
}

const baseSrc = computed(() => {
  if (!index.value) return ''
  return downloadUrl(`output/preview/${plane.value}/${sliceName(index.value.img_prefix)}`)
})

const overlaySrc = computed(() => {
  if (!index.value) return ''
  return downloadUrl(`output/preview/${plane.value}/${sliceName(index.value.overlay_prefix)}`)
})

// --- SAVE SNAPSHOT FUNCTION ---
function saveSnapshot() {
  if (!index.value) return
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  const img1 = new Image()
  const img2 = new Image()
  
  img1.crossOrigin = "anonymous"
  img2.crossOrigin = "anonymous"
  img1.src = baseSrc.value
  img2.src = overlaySrc.value
  
  img1.onload = () => {
    canvas.width = img1.width
    canvas.height = img1.height
    ctx.drawImage(img1, 0, 0)
    img2.onload = () => {
      ctx.globalAlpha = opacity.value
      ctx.drawImage(img2, 0, 0)
      const link = document.createElement('a')
      link.download = `brain_scan_${plane.value}_slice${slice.value}.png`
      link.href = canvas.toDataURL('image/png')
      link.click()
    }
  }
}
</script>

<template>
  <div class="card">
    <div class="top">
      <h3>2D Slice View</h3>
      <div class="actions">
        <button class="icon-btn" @click="saveSnapshot" title="Save Snapshot">📸 Save</button>
        <div class="btns">
          <button v-for="p in ['axial','coronal','sagittal']" :key="p" :class="{active: plane===p}" @click="plane=p">
            {{ p }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="error" class="err">{{ error }}</div>

    <div v-if="index" class="frame-container">
      <div class="frame">
        <img :src="baseSrc" class="img" crossorigin="anonymous" />
        <img :src="overlaySrc" class="img overlay" :style="{ opacity }" crossorigin="anonymous" />
      </div>
    </div>
    
    <div v-else class="loading-state">Loading Slices...</div>

    <div v-if="index" class="controls">
      <div class="row">
        <span>Plane: <b style="text-transform: capitalize">{{ plane }}</b></span>
        <span>Slice: <b>{{ slice }}</b> / {{ maxSlice }}</span>
      </div>
      <input type="range" :min="0" :max="maxSlice" v-model.number="slice" class="slider" />
      <div class="label">Tumor Opacity: {{ Math.round(opacity * 100) }}%</div>
      <input type="range" min="0" max="1" step="0.05" v-model.number="opacity" class="slider" />
    </div>
  </div>
</template>

<style scoped>
.card { 
  border:1px solid #334155; 
  border-radius:12px; 
  background:#0f172a; 
  padding:16px; 
  height:100%; 
  display: flex; 
  flex-direction: column; /* Key to fixing layout */
  color: white; 
  box-sizing: border-box;
}

.top { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; flex-shrink: 0; }
h3 { margin:0; font-size: 1.1rem; color: #f1f5f9; }

.actions { display: flex; gap: 8px; }
.btns { display:flex; gap:5px; background: #1e293b; padding: 4px; border-radius: 8px; }
button { padding:6px 12px; border-radius:6px; border:none; background:transparent; color:#94a3b8; cursor:pointer; font-size: 0.85rem; font-weight: 500; transition: 0.2s; }
button:hover { color: white; }
button.active { background:#2563eb; color:white; }
.icon-btn { background: #1e293b; color: #e2e8f0; font-size: 0.9rem; padding: 6px 10px; }
.icon-btn:hover { background: #334155; }

.err { color:#fca5a5; background: rgba(127, 29, 29, 0.2); padding: 10px; border-radius: 6px; font-size: 0.9rem; margin-bottom: 10px; }

/* The container for the image. flex: 1 makes it fill remaining height */
.frame-container { 
  flex: 1; 
  min-height: 0; /* CRITICAL fix for nested flex containers to allow shrinking */
  position: relative; 
  background: #000;
  border-radius: 8px;
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.frame { position: relative; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
.img { width:100%; height:100%; object-fit:contain; display:block; }
.overlay { position:absolute; inset:0; mix-blend-mode: normal; pointer-events: none; }

.loading-state { flex: 1; display: flex; align-items: center; justify-content: center; color: #64748b; font-style: italic; }

.controls { flex-shrink: 0; padding-top: 10px; border-top: 1px solid #1e293b; }
.row { display:flex; justify-content:space-between; font-size:0.9rem; margin-bottom:8px; color: #cbd5e1; }
.label { margin-top:8px; font-size:0.85rem; color: #94a3b8; margin-bottom: 5px; }
.slider { width:100%; cursor: pointer; accent-color: #2563eb; }
</style>