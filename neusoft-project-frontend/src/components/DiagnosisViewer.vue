Here is your complete, fully updated Vue component. I have seamlessly integrated the Canvas Stamping logic into the `save3DViewAsPng` function exactly where it was missing.

You can copy and paste this entire block directly into your Vue file:

```html
<template>
  <div>
    <div v-if="shape" class="grid-2x2">

      <div class="pane">
        <div class="pane-header">
          <span>Axial (z)</span>
          <span class="index-label">z = {{ axialIndex }} / {{ shape.depth - 1 }}</span>
        </div>
        <div class="roi-toolbar">
          <button class="roi-btn" @click="undoRoi('axial')"  :disabled="!canUndo('axial')">Undo</button>
          <button class="roi-btn" @click="redoRoi('axial')"  :disabled="!canRedo('axial')">Redo</button>
          <button class="roi-btn" @click="clearRois('axial')" :disabled="!hasRois('axial')">Clear</button>
          <button class="roi-btn primary" @click="saveViewAsPng('axial')" :disabled="!axialImg">Save PNG</button>
        </div>
        <div class="slice-frame" ref="axialFrame">
          <img v-if="axialImg"    :src="axialImg"    class="slice-img"         :style="{ transform: 'scale(' + zoomAxial + ')' }" />
          <img v-if="axialSegImg" :src="axialSegImg" class="slice-img overlay" :style="{ transform: 'scale(' + zoomAxial + ')' }" />
          <canvas ref="axialCanvas" class="roi-canvas"
            @mousedown="onRoiMouseDown('axial', $event)"
            @mousemove="onRoiMouseMove($event)"
            @mouseup="onRoiMouseUp"
            @mouseleave="onRoiMouseUp"
          ></canvas>
        </div>
        <div class="slider-row">
          <label class="slider-label">Axial (z)</label>
          <input type="range" :min="0" :max="shape.depth - 1" v-model.number="axialIndex" @input="onAxialChange" />
        </div>
        <div class="slider-row">
          <label class="slider-label">Zoom</label>
          <input type="range" min="0.4" max="2" step="0.01" v-model.number="zoomAxial" />
        </div>
      </div>

      <div class="pane">
        <div class="pane-header">
          <span>Sagittal (x)</span>
          <span class="index-label">x = {{ sagittalIndex }} / {{ shape.width - 1 }}</span>
        </div>
        <div class="roi-toolbar">
          <button class="roi-btn" @click="undoRoi('sagittal')"  :disabled="!canUndo('sagittal')">Undo</button>
          <button class="roi-btn" @click="redoRoi('sagittal')"  :disabled="!canRedo('sagittal')">Redo</button>
          <button class="roi-btn" @click="clearRois('sagittal')" :disabled="!hasRois('sagittal')">Clear</button>
          <button class="roi-btn primary" @click="saveViewAsPng('sagittal')" :disabled="!sagittalImg">Save PNG</button>
        </div>
        <div class="slice-frame" ref="sagittalFrame">
          <img v-if="sagittalImg"    :src="sagittalImg"    class="slice-img sagittal-img" :style="{ transform: 'translate(-50%, -50%) scaleY(-1) scale(' + zoomSagittal + ')' }" />
          <img v-if="sagittalSegImg" :src="sagittalSegImg" class="slice-img overlay sagittal-img" :style="{ transform: 'translate(-50%, -50%) scaleY(-1) scale(' + zoomSagittal + ')' }" />
          <canvas ref="sagittalCanvas" class="roi-canvas"
            @mousedown="onRoiMouseDown('sagittal', $event)"
            @mousemove="onRoiMouseMove($event)"
            @mouseup="onRoiMouseUp"
            @mouseleave="onRoiMouseUp"
          ></canvas>
        </div>
        <div class="slider-row">
          <label class="slider-label">Sagittal (x)</label>
          <input type="range" :min="0" :max="shape.width - 1" v-model.number="sagittalIndex" @input="onSagittalChange" />
        </div>
        <div class="slider-row">
          <label class="slider-label">Zoom</label>
          <input type="range" min="0.4" max="2" step="0.01" v-model.number="zoomSagittal" />
        </div>
      </div>

      <div class="pane">
        <div class="pane-header">
          <span>Coronal (y)</span>
          <span class="index-label">y = {{ coronalIndex }} / {{ shape.height - 1 }}</span>
        </div>
        <div class="roi-toolbar">
          <button class="roi-btn" @click="undoRoi('coronal')"  :disabled="!canUndo('coronal')">Undo</button>
          <button class="roi-btn" @click="redoRoi('coronal')"  :disabled="!canRedo('coronal')">Redo</button>
          <button class="roi-btn" @click="clearRois('coronal')" :disabled="!hasRois('coronal')">Clear</button>
          <button class="roi-btn primary" @click="saveViewAsPng('coronal')" :disabled="!coronalImg">Save PNG</button>
        </div>
        <div class="slice-frame" ref="coronalFrame">
          <img v-if="coronalImg"    :src="coronalImg"    class="slice-img coronal-img" :style="{ transform: 'translate(-50%, -50%) scaleY(-1) scale(' + zoomCoronal + ')' }" />
          <img v-if="coronalSegImg" :src="coronalSegImg" class="slice-img overlay coronal-img" :style="{ transform: 'translate(-50%, -50%) scaleY(-1) scale(' + zoomCoronal + ')' }" />
          <canvas ref="coronalCanvas" class="roi-canvas"
            @mousedown="onRoiMouseDown('coronal', $event)"
            @mousemove="onRoiMouseMove($event)"
            @mouseup="onRoiMouseUp"
            @mouseleave="onRoiMouseUp"
          ></canvas>
        </div>
        <div class="slider-row">
          <label class="slider-label">Coronal (y)</label>
          <input type="range" :min="0" :max="shape.height - 1" v-model.number="coronalIndex" @input="onCoronalChange" />
        </div>
        <div class="slider-row">
          <label class="slider-label">Zoom</label>
          <input type="range" min="0.4" max="2" step="0.01" v-model.number="zoomCoronal" />
        </div>
      </div>

      <div class="pane">
        <div class="pane-header">
          <span>3D Segmentation</span>
          <span v-if="!isMeshLoaded" class="index-label">Rotate with mouse, scroll to zoom</span>
          <span v-else class="index-label tumor-stats">
            Vol: {{ tumorVolume }} cm³ | {{ tumorDimensions.w }}×{{ tumorDimensions.h }}×{{ tumorDimensions.d }} mm
          </span>
        </div>
        <div class="roi-toolbar" style="justify-content: flex-end;">
          <button class="roi-btn primary" @click="save3DViewAsPng()" :disabled="!isMeshLoaded">Save 3D PNG</button>
        </div>
        <div ref="threeContainer" class="three-container"></div>
      </div>

    </div>

    <p v-else class="empty-hint">Load a CT volume to see slices and 3D segmentation.</p>
  </div>
</template>

<script setup>
import { PYTHON_API_URL } from '../config.js'
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import axios from 'axios'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'

// ── Props from DiagnosisStudio.vue ──
const props = defineProps({
  shape:        { type: Object, default: null },   // { depth, height, width }
  selectedPath: { type: String, default: '' },
  segPath:      { type: String, default: '' },
  // Initial slice images + indices when volume first loads
  initAxialImg:    { type: String, default: null },
  initSagittalImg: { type: String, default: null },
  initCoronalImg:  { type: String, default: null },
  initAxialSegImg: { type: String, default: null },
  initSagittalSegImg: { type: String, default: null },
  initCoronalSegImg:  { type: String, default: null },
  initAxialIndex:    { type: Number, default: 0 },
  initSagittalIndex: { type: Number, default: 0 },
  initCoronalIndex:  { type: Number, default: 0 },
})

const emit = defineEmits(['status'])
const PYTHON_BASE = PYTHON_API_URL

// ── NEW: 3D Tumor Statistics State ──
const isMeshLoaded = ref(false)
const tumorVolume = ref("0.00")
const tumorDimensions = ref({ w: 0, h: 0, d: 0 })

// ── Slice image state ──
const axialImg    = ref(props.initAxialImg)
const sagittalImg = ref(props.initSagittalImg)
const coronalImg  = ref(props.initCoronalImg)
const axialSegImg    = ref(props.initAxialSegImg)
const sagittalSegImg = ref(props.initSagittalSegImg)
const coronalSegImg  = ref(props.initCoronalSegImg)

// ── Slice indices & zoom ──
const axialIndex    = ref(props.initAxialIndex)
const sagittalIndex = ref(props.initSagittalIndex)
const coronalIndex  = ref(props.initCoronalIndex)
const zoomAxial    = ref(1)
const zoomSagittal = ref(1)
const zoomCoronal  = ref(1)

// ── Canvas & frame refs ──
const axialCanvas    = ref(null)
const sagittalCanvas = ref(null)
const coronalCanvas  = ref(null)
const axialFrame    = ref(null)
const sagittalFrame = ref(null)
const coronalFrame  = ref(null)
const threeContainer = ref(null)

// ── ROI state ──
const axialRois    = ref([]);  const axialRedo    = ref([])
const sagittalRois = ref([]);  const sagittalRedo = ref([])
const coronalRois  = ref([]);  const coronalRedo  = ref([])
const isDrawing  = ref(false)
const currentRoi = ref([])
const activeView = ref(null)

// ── Three.js state ──
let scene = null, camera = null, renderer = null
let mesh = null, controls = null, animationId = null
let axialPlane = null, sagittalPlane = null, coronalPlane = null
let axialTex = null, sagittalTex = null, coronalTex = null

const axialTexCanvas    = document.createElement('canvas')
const sagittalTexCanvas = document.createElement('canvas')
const coronalTexCanvas  = document.createElement('canvas')

const clipAxial    = new THREE.Plane(new THREE.Vector3(0,  0, -1), 0)
const clipCoronal  = new THREE.Plane(new THREE.Vector3(0, -1,  0), 0)
const clipSagittal = new THREE.Plane(new THREE.Vector3(-1, 0,  0), 0)

// ═══════════════════════════════════════
// WATCH
// ═══════════════════════════════════════
watch(() => props.initAxialImg, v => {
  axialImg.value    = v
  axialIndex.value  = props.initAxialIndex
  sagittalImg.value = props.initSagittalImg
  sagittalIndex.value = props.initSagittalIndex
  coronalImg.value  = props.initCoronalImg
  coronalIndex.value = props.initCoronalIndex
  axialSegImg.value    = props.initAxialSegImg
  sagittalSegImg.value = props.initSagittalSegImg
  coronalSegImg.value  = props.initCoronalSegImg
  
  axialRois.value = []; sagittalRois.value = []; coronalRois.value = []
  axialRedo.value = []; sagittalRedo.value = []; coronalRedo.value = []
  
  isMeshLoaded.value = false // Reset 3D mesh stats on new volume
  
  nextTick(() => {
    if (props.shape) {
      resizeAllRoiCanvases()
      initThreeIfNeeded()
      rebuildSlicePlanes()
      updateMeshClippingPlanes()
    }
  })
}, { immediate: true })

// ═══════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════
function b64ToDataUrl(b64) { return b64 ? 'data:image/png;base64,' + b64 : null }

function loadImageDataUrl(dataUrl) {
  return new Promise((resolve, reject) => {
    if (!dataUrl) return resolve(null)
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = reject
    img.src = dataUrl
  })
}

function getVolumeCenters() {
  if (!props.shape) return { cx: 0, cy: 0, cz: 0, W: 0, H: 0, D: 0 }
  const { depth: D, height: H, width: W } = props.shape
  return { cx: (W-1)/2, cy: (H-1)/2, cz: (D-1)/2, W, H, D }
}

// ═══════════════════════════════════════
// SLICE FETCHING
// ═══════════════════════════════════════
async function fetchSlice(axis, index, ctRef, segRef) {
  if (!props.shape || !props.selectedPath) return
  try {
    const res = await axios.post(`${PYTHON_BASE}/viewer/slice`, {
      path: props.selectedPath, seg_path: props.segPath,
      axis, index, ww: 400, wl: 40
    })
    if (res.data.error) throw new Error(res.data.error)
    ctRef.value  = b64ToDataUrl(res.data.png_ct)
    segRef.value = b64ToDataUrl(res.data.png_seg)
    await nextTick()
    resizeAllRoiCanvases()
    redrawRois(axis)
    updatePlanePosition(axis, index)
    await updatePlaneTexture(axis, ctRef.value)
    updateMeshClippingPlanes()
  } catch (err) {
    emit('status', 'Failed to fetch slice: ' + err.message)
  }
}

function onAxialChange()    { fetchSlice('axial',    axialIndex.value,    axialImg,    axialSegImg) }
function onSagittalChange() { fetchSlice('sagittal', sagittalIndex.value, sagittalImg, sagittalSegImg) }
function onCoronalChange()  { fetchSlice('coronal',  coronalIndex.value,  coronalImg,  coronalSegImg) }

// ═══════════════════════════════════════
// THREE.JS
// ═══════════════════════════════════════
function updateMeshClippingPlanes() {
  if (!props.shape || !mesh) return
  const { cx, cy, cz } = getVolumeCenters()
  clipAxial.constant    = axialIndex.value    - cz
  clipCoronal.constant  = coronalIndex.value  - cy
  clipSagittal.constant = sagittalIndex.value - cx
  mesh.material.needsUpdate = true
}

async function updatePlaneTexture(axis, ctDataUrl) {
  const canvas = axis === 'axial' ? axialTexCanvas : axis === 'sagittal' ? sagittalTexCanvas : coronalTexCanvas
  const tex    = axis === 'axial' ? axialTex       : axis === 'sagittal' ? sagittalTex       : coronalTex
  if (!tex) return
  const flipY = axis === 'sagittal' || axis === 'coronal'
  const ctImg = await loadImageDataUrl(ctDataUrl)
  if (!ctImg) return
  canvas.width  = ctImg.naturalWidth  || ctImg.width
  canvas.height = ctImg.naturalHeight || ctImg.height
  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  if (flipY) {
    ctx.save(); ctx.translate(0, canvas.height); ctx.scale(1, -1)
    ctx.drawImage(ctImg, 0, 0, canvas.width, canvas.height)
    ctx.restore()
  } else {
    ctx.drawImage(ctImg, 0, 0, canvas.width, canvas.height)
  }
  tex.needsUpdate = true
}

function updatePlanePosition(axis, index) {
  if (!props.shape) return
  const { cx, cy, cz } = getVolumeCenters()
  if (axis === 'axial'    && axialPlane)    axialPlane.position.set(0, 0, index - cz)
  if (axis === 'coronal'  && coronalPlane)  coronalPlane.position.set(0, index - cy, 0)
  if (axis === 'sagittal' && sagittalPlane) sagittalPlane.position.set(index - cx, 0, 0)
}

function disposePlane(p) {
  if (!p) return
  if (p.geometry) p.geometry.dispose()
  if (p.material?.map) p.material.map.dispose()
  if (p.material) p.material.dispose()
}

function rebuildSlicePlanes() {
  if (!scene || !props.shape) return
  if (axialPlane)    { scene.remove(axialPlane);    disposePlane(axialPlane) }
  if (coronalPlane)  { scene.remove(coronalPlane);  disposePlane(coronalPlane) }
  if (sagittalPlane) { scene.remove(sagittalPlane); disposePlane(sagittalPlane) }
  axialPlane = coronalPlane = sagittalPlane = null

  const { W, H, D } = getVolumeCenters()
  axialTex    = new THREE.CanvasTexture(axialTexCanvas)
  sagittalTex = new THREE.CanvasTexture(sagittalTexCanvas)
  coronalTex  = new THREE.CanvasTexture(coronalTexCanvas)
  for (const t of [axialTex, sagittalTex, coronalTex]) {
    t.minFilter = THREE.LinearFilter; t.magFilter = THREE.LinearFilter; t.generateMipmaps = false
  }

  const mat = (tex) => new THREE.MeshBasicMaterial({
    map: tex, transparent: true, opacity: 0.30,
    depthWrite: false, depthTest: true, side: THREE.DoubleSide
  })

  axialPlane = new THREE.Mesh(new THREE.PlaneGeometry(W, H), mat(axialTex))
  axialPlane.renderOrder = 2; scene.add(axialPlane)

  coronalPlane = new THREE.Mesh(new THREE.PlaneGeometry(W, D), mat(coronalTex))
  coronalPlane.rotation.x = -Math.PI / 2; coronalPlane.renderOrder = 2; scene.add(coronalPlane)

  sagittalPlane = new THREE.Mesh(new THREE.PlaneGeometry(D, H), mat(sagittalTex))
  sagittalPlane.rotation.y = Math.PI / 2; sagittalPlane.renderOrder = 2; scene.add(sagittalPlane)

  updatePlanePosition('axial',    axialIndex.value)
  updatePlanePosition('coronal',  coronalIndex.value)
  updatePlanePosition('sagittal', sagittalIndex.value)
  updatePlaneTexture('axial',    axialImg.value)
  updatePlaneTexture('coronal',  coronalImg.value)
  updatePlaneTexture('sagittal', sagittalImg.value)
}

function initThreeIfNeeded() {
  if (!threeContainer.value || renderer) return
  const container = threeContainer.value
  const { clientWidth: W, clientHeight: H } = container
  
  if (W === 0 || H === 0) return 
  
  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x020617)

  camera = new THREE.PerspectiveCamera(40, W / H, 0.1, 5000)
  camera.position.set(0, 0, 350)

  // CRITICAL UPDATE: preserveDrawingBuffer must be true to allow 3D screenshots!
  renderer = new THREE.WebGLRenderer({ antialias: true, preserveDrawingBuffer: true })
  renderer.setSize(W, H)
  renderer.localClippingEnabled = true
  container.appendChild(renderer.domElement)

  const dirLight1 = new THREE.DirectionalLight(0xffffff, 0.9);
  dirLight1.position.set(1, 1, 1);
  const dirLight2 = new THREE.DirectionalLight(0xffffff, 0.9);
  scene.add(dirLight1, dirLight2);
  scene.add(new THREE.AmbientLight(0xffffff, 0.3))

  const grid = new THREE.GridHelper(240, 12, 0x4b5563, 0x1f2937)
  grid.position.y = -120; scene.add(grid)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true; controls.dampingFactor = 0.1

  const animate = () => {
    animationId = requestAnimationFrame(animate)
    controls?.update()
    renderer.render(scene, camera)
  }
  animate()
}

// ── NEW: 3D SCREENSHOT LOGIC ──
function save3DViewAsPng() {
  if (!renderer) return
  
  // 1. Force a fresh render right before taking the picture
  renderer.render(scene, camera)
  
  // 2. Create a temporary hidden 2D canvas
  const w = renderer.domElement.width
  const h = renderer.domElement.height
  const outCanvas = document.createElement('canvas')
  outCanvas.width = w
  outCanvas.height = h
  const ctx = outCanvas.getContext('2d')
  
  // 3. Draw the raw 3D WebGL image onto our new 2D canvas
  ctx.drawImage(renderer.domElement, 0, 0, w, h)
  
  // 4. Draw a semi-transparent dark background box for text readability
  ctx.fillStyle = 'rgba(2, 6, 23, 0.85)' // Dark slate background
  ctx.beginPath()
  if (ctx.roundRect) {
    ctx.roundRect(15, 15, 420, 80, 8) // Modern browsers
  } else {
    ctx.fillRect(15, 15, 420, 80) // Fallback
  }
  ctx.fill()
  
  // 5. Draw the Neusoft PACS Branding / Timestamp
  ctx.fillStyle = '#94a3b8' // Light Gray text
  ctx.font = '600 14px "Inter", Arial, sans-serif'
  const dateStr = new Date().toLocaleString()
  ctx.fillText(`NEUSOFT PACS: 3D TUMOR ANALYSIS  [${dateStr}]`, 30, 40)
  
  // 6. Draw the actual calculated Tumor Statistics in Medical Green
  ctx.fillStyle = '#4ade80' // Neon Green text
  ctx.font = 'bold 20px "Inter", Arial, sans-serif'
  const statsText = `Vol: ${tumorVolume.value} cm³ | ${tumorDimensions.value.w}×${tumorDimensions.value.h}×${tumorDimensions.value.d} mm`
  ctx.fillText(statsText, 30, 70)
  
  // 7. Extract the final stamped image and trigger the download
  const dataUrl = outCanvas.toDataURL('image/png')
  const a = document.createElement('a')
  a.href = dataUrl
  // Dynamically name the file to include the volume!
  a.download = `Neusoft_3D_Report_${tumorVolume.value}cm3.png`
  a.click()
}


// ── EXPOSED MESH LOGIC (UPDATED WITH VOLUME CALCULATION) ──
function applyMeshData(vertices, faces) {
  if (!scene) initThreeIfNeeded()
  const geometry = new THREE.BufferGeometry()
  const positions = new Float32Array(faces.length * 9)
  let idx = 0
  for (const [a, b, c] of faces) {
    for (const v of [vertices[a], vertices[b], vertices[c]]) {
      positions[idx++] = v[2]; positions[idx++] = v[1]; positions[idx++] = v[0]
    }
  }
  geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  geometry.computeVertexNormals()
  
  
  // 1. Calculate Bounding Box Dimensions (Width x Height x Depth)
  geometry.computeBoundingBox()
  const box = geometry.boundingBox
  const width = Math.abs(box.max.x - box.min.x).toFixed(1)
  const height = Math.abs(box.max.y - box.min.y).toFixed(1)
  const depth = Math.abs(box.max.z - box.min.z).toFixed(1)
  tumorDimensions.value = { w: width, h: height, d: depth }

  // 2. Calculate Exact Mesh Volume using the Divergence Theorem (Scalar Triple Product)
  let rawVolume = 0
  const posArr = geometry.attributes.position.array
  for (let i = 0; i < posArr.length; i += 9) {
    const v1 = new THREE.Vector3(posArr[i], posArr[i+1], posArr[i+2])
    const v2 = new THREE.Vector3(posArr[i+3], posArr[i+4], posArr[i+5])
    const v3 = new THREE.Vector3(posArr[i+6], posArr[i+7], posArr[i+8])
    // The signed volume of the tetrahedron formed by the triangle and the origin
    rawVolume += v1.dot(v2.cross(v3)) / 6.0
  }
  // Convert from mm³ to cm³ (1 cm³ = 1000 mm³)
  tumorVolume.value = (Math.abs(rawVolume) / 1000).toFixed(2)

  if (mesh) { scene.remove(mesh); mesh.geometry.dispose(); mesh.material.dispose() }

  const { cx, cy, cz } = getVolumeCenters()
  mesh = new THREE.Mesh(geometry, new THREE.MeshStandardMaterial({
    color: 0x22c55e, metalness: 0.1, roughness: 0.5,
    transparent: true, opacity: 0.85,
    clippingPlanes: [clipAxial, clipCoronal, clipSagittal],
    clipIntersection: true
  }))
  mesh.position.set(-cx, -cy, -cz)
  scene.add(mesh)
  
  isMeshLoaded.value = true // Triggers UI update
  
  rebuildSlicePlanes()
  updateMeshClippingPlanes()
}

defineExpose({ applyMeshData })

// ═══════════════════════════════════════
// ROI HELPERS
// ═══════════════════════════════════════
function getRoisRef(v) { return v === 'axial' ? axialRois : v === 'sagittal' ? sagittalRois : coronalRois }
function getRedoRef(v) { return v === 'axial' ? axialRedo : v === 'sagittal' ? sagittalRedo : coronalRedo }
function getCanvasRef(v) { return v === 'axial' ? axialCanvas : v === 'sagittal' ? sagittalCanvas : coronalCanvas }

function hasRois(v)  { return getRoisRef(v).value.length > 0 }
function canUndo(v)  { return getRoisRef(v).value.length > 0 }
function canRedo(v)  { return getRedoRef(v).value.length > 0 }

function undoRoi(v) {
  const r = getRoisRef(v), d = getRedoRef(v)
  if (!r.value.length) return
  d.value = [...d.value, r.value[r.value.length - 1]]
  r.value = r.value.slice(0, -1)
  redrawRois(v)
}
function redoRoi(v) {
  const r = getRoisRef(v), d = getRedoRef(v)
  if (!d.value.length) return
  r.value = [...r.value, d.value[d.value.length - 1]]
  d.value = d.value.slice(0, -1)
  redrawRois(v)
}
function clearRois(v) {
  getRoisRef(v).value = []; getRedoRef(v).value = []
  redrawRois(v)
}

function redrawRois(view) {
  const canvas = getCanvasRef(view).value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  const rois = getRoisRef(view).value
  ctx.lineWidth = 2; ctx.strokeStyle = '#22c55e'; ctx.fillStyle = 'rgba(34,197,94,0.18)'
  for (const path of rois) {
    if (path.length < 2) continue
    ctx.beginPath(); ctx.moveTo(path[0].x, path[0].y)
    for (let i = 1; i < path.length; i++) ctx.lineTo(path[i].x, path[i].y)
    ctx.closePath(); ctx.stroke(); ctx.fill()
  }
  if (isDrawing.value && activeView.value === view && currentRoi.value.length > 1) {
    const path = currentRoi.value
    ctx.beginPath(); ctx.moveTo(path[0].x, path[0].y)
    for (let i = 1; i < path.length; i++) ctx.lineTo(path[i].x, path[i].y)
    ctx.stroke()
  }
}

function resizeOneCanvas(canvasRef) {
  const canvas = canvasRef.value
  if (!canvas?.parentElement) return
  canvas.width  = canvas.parentElement.clientWidth
  canvas.height = canvas.parentElement.clientHeight
}

function resizeAllRoiCanvases() {
  resizeOneCanvas(axialCanvas); resizeOneCanvas(sagittalCanvas); resizeOneCanvas(coronalCanvas)
  redrawRois('axial'); redrawRois('sagittal'); redrawRois('coronal')
}

function onRoiMouseDown(view, event) {
  if (event.button !== 0) return
  const canvas = getCanvasRef(view).value
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  isDrawing.value  = true
  activeView.value = view
  currentRoi.value = [{ x: event.clientX - rect.left, y: event.clientY - rect.top }]
  redrawRois(view)
}

function onRoiMouseMove(event) {
  if (!isDrawing.value || !activeView.value) return
  const canvas = getCanvasRef(activeView.value).value
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  currentRoi.value.push({ x: event.clientX - rect.left, y: event.clientY - rect.top })
  redrawRois(activeView.value)
}

function onRoiMouseUp() {
  if (!isDrawing.value || !activeView.value) return
  if (currentRoi.value.length > 2) {
    const r = getRoisRef(activeView.value)
    r.value = [...r.value, [...currentRoi.value]]
    getRedoRef(activeView.value).value = []
  }
  isDrawing.value = false
  redrawRois(activeView.value)
  activeView.value = null; currentRoi.value = []
}

async function saveViewAsPng(view) {
  const canvas = getCanvasRef(view).value
  if (!canvas) return
  const { width: w, height: h } = canvas
  if (w === 0 || h === 0) return 

  const out = document.createElement('canvas'); out.width = w; out.height = h
  const ctx = out.getContext('2d')

  const zoom = view === 'axial' ? zoomAxial.value : view === 'sagittal' ? zoomSagittal.value : zoomCoronal.value
  const flipY = view !== 'axial'
  const ctUrl  = view === 'axial' ? axialImg.value    : view === 'sagittal' ? sagittalImg.value    : coronalImg.value
  const segUrl = view === 'axial' ? axialSegImg.value : view === 'sagittal' ? sagittalSegImg.value : coronalSegImg.value

  const [ctImg, segImg] = await Promise.all([loadImageDataUrl(ctUrl), loadImageDataUrl(segUrl)])

  ctx.save()
  ctx.translate(w / 2, h / 2)
  ctx.scale(zoom, flipY ? -zoom : zoom)
  ctx.translate(-w / 2, -h / 2)
  if (ctImg)  ctx.drawImage(ctImg,  0, 0, w, h)
  if (segImg) ctx.drawImage(segImg, 0, 0, w, h)
  ctx.restore()
  ctx.drawImage(canvas, 0, 0, w, h)

  const a = document.createElement('a')
  a.href = out.toDataURL('image/png')
  a.download = `${view}_slice.png`
  a.click()
}

// ═══════════════════════════════════════
// RESIZE / LIFECYCLE
// ═══════════════════════════════════════
function onWindowResize() {
  resizeAllRoiCanvases()
  if (!renderer || !camera || !threeContainer.value) return
  const { clientWidth, clientHeight } = threeContainer.value
  camera.aspect = clientWidth / clientHeight
  camera.updateProjectionMatrix()
  renderer.setSize(clientWidth, clientHeight)
}

onMounted(() => {
  window.addEventListener('resize', onWindowResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onWindowResize)
  if (animationId) cancelAnimationFrame(animationId)
  disposePlane(axialPlane); disposePlane(coronalPlane); disposePlane(sagittalPlane)
  if (mesh) { mesh.geometry.dispose(); mesh.material.dispose() }
  if (renderer?.domElement?.parentNode) renderer.domElement.parentNode.removeChild(renderer.domElement)
})
</script>

<style scoped>
.grid-2x2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-auto-rows: auto;
  gap: 14px;
}

.pane {
  background: #020617;
  border-radius: 16px;
  padding: 12px 10px 10px;
  box-shadow:
    0 18px 35px rgba(15,23,42,0.4),
    0 0 0 1px rgba(30,64,175,0.4);
}

.pane-header {
  display: flex; justify-content: space-between;
  font-size: 0.8rem; color: #cbd5f5; margin-bottom: 6px;
}
.index-label { color: #6b7280; }

.tumor-stats {
  color: #4ade80; /* Bright green text for medical stats */
  font-weight: 600;
  background: rgba(74, 222, 128, 0.1);
  padding: 2px 8px;
  border-radius: 6px;
  border: 1px solid rgba(74, 222, 128, 0.2);
}

/* ROI toolbar */
.roi-toolbar { display: flex; gap: 8px; margin-bottom: 8px; flex-wrap: wrap; }
.roi-btn {
  border-radius: 999px; padding: 6px 12px;
  font-size: 0.78rem; cursor: pointer;
  background: #111827; color: #e5e7eb;
  border: 1px solid #1f2937; transition: 0.15s;
}
.roi-btn:hover:not(:disabled) { background: #1f2937; }
.roi-btn.primary { background: linear-gradient(135deg, #2563eb, #1d4ed8); border: none; color: white; }
.roi-btn.primary:hover:not(:disabled) { background: linear-gradient(135deg, #1d4ed8, #1e40af); }
.roi-btn:disabled { opacity: 0.55; cursor: default; }

/* Slice frame */
.slice-frame {
  position: relative; width: 100%; height: 260px;
  border-radius: 10px; overflow: hidden; background: #020617;
}

.slice-img {
  position: absolute; inset: 0;
  width: 100%; height: 100%;
  object-fit: cover; transform-origin: center center;
}
.sagittal-img, .coronal-img {
  top: 50%; left: 50%;
  width: 300%; height: 200%;
  transform-origin: center center;
}
.slice-img.overlay { mix-blend-mode: normal; }

/* Sliders */
.slider-row { margin-top: 6px; display: flex; align-items: center; gap: 8px; }
.slider-label { width: 72px; font-size: 0.75rem; color: #9ca3af; }
.slider-row input[type='range'] { flex: 1; }

/* ROI canvas */
.roi-canvas {
  position: absolute; inset: 0;
  width: 100%; height: 100%;
  pointer-events: auto; cursor: crosshair;
}

/* Three.js container */
.three-container {
  width: 100%; height: 260px;
  border-radius: 10px; overflow: hidden;
  background: #020617;
}

.empty-hint { margin-top: 24px; color: #9ca3af; font-size: 0.9rem; }
</style>

