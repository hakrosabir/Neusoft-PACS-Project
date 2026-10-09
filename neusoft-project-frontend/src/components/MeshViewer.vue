<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { PLYLoader } from 'three/examples/jsm/loaders/PLYLoader.js'


const props = defineProps({
  meshUrl: { type: String, required: true },
})

const container = ref(null)
const loading = ref(false)
const loadError = ref('')

let renderer, scene, camera, controls, frameId, ro
let initialized = false

function cleanup() {
  if (frameId) cancelAnimationFrame(frameId)
  frameId = null

  if (ro) ro.disconnect()
  ro = null

  if (controls) controls.dispose()
  controls = null

  if (renderer) {
    renderer.dispose()
    renderer.domElement?.remove()
  }

  renderer = scene = camera = null
  initialized = false
}

function resize() {
  if (!container.value || !renderer || !camera) return
  const w = container.value.clientWidth
  const h = container.value.clientHeight
  if (!w || !h) return
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
}

function fitCameraToObject(cam, obj, ctrls) {
  const box = new THREE.Box3().setFromObject(obj)
  const size = box.getSize(new THREE.Vector3())
  const center = box.getCenter(new THREE.Vector3())
  const maxDim = Math.max(size.x, size.y, size.z) || 1

  const fov = cam.fov * (Math.PI / 180)
  let cameraZ = Math.abs(maxDim / (2 * Math.tan(fov / 2)))
  cameraZ *= 1.6

  cam.position.set(center.x, center.y, center.z + cameraZ)
  cam.near = Math.max(0.001, maxDim / 10000)
  cam.far = maxDim * 10000
  cam.updateProjectionMatrix()

  ctrls.target.copy(center)
  ctrls.update()
}

async function ensureInitialized() {
  if (initialized) return

  await nextTick() // 
  if (!container.value) return

  console.log('[MeshViewer] init')

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x0b1020)

  const w = container.value.clientWidth || 1
  const h = container.value.clientHeight || 1

  camera = new THREE.PerspectiveCamera(45, w / h, 0.1, 100000)
  camera.position.set(0, 0, 200)

  renderer = new THREE.WebGLRenderer({ antialias: true, preserveDrawingBuffer: true })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.setSize(w, h)

  container.value.appendChild(renderer.domElement)
  renderer.domElement.style.width = '100%'
  renderer.domElement.style.height = '100%'
  renderer.domElement.style.display = 'block'

  // lights
  // scene.add(new THREE.AmbientLight(0xffffff, 0.6))
  // const key = new THREE.DirectionalLight(0xffffff, 1.0)
  // key.position.set(2, 2, 2)
  // scene.add(key)
  // lights (nicer depth)
  scene.add(new THREE.AmbientLight(0xffffff, 0.35))

  const key = new THREE.DirectionalLight(0xffffff, 0.9)
  key.position.set(2, 2, 2)
  scene.add(key)

  const fill = new THREE.DirectionalLight(0xffffff, 0.35)
  fill.position.set(-2, 1, -1)
  scene.add(fill)

  const hemi = new THREE.HemisphereLight(0xffffff, 0x223355, 0.25)
  scene.add(hemi)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true

  ro = new ResizeObserver(() => resize())
  ro.observe(container.value)
  requestAnimationFrame(resize)

  const animate = () => {
    frameId = requestAnimationFrame(animate)
    controls.update()
    renderer.render(scene, camera)
  }
  animate()

  initialized = true
}

async function loadMesh(url) {
  await ensureInitialized()
  if (!scene) return

  loadError.value = ''
  loading.value = true
  console.log('[MeshViewer] loading mesh:', url)

  try {
    // clear previous model(s) if any
    const toRemove = []
    scene.traverse((obj) => {
      if (obj.userData.__isLoadedMesh) toRemove.push(obj)
    })
    toRemove.forEach((obj) => {
      obj.parent?.remove(obj)
      obj.geometry?.dispose?.()
      if (obj.material?.dispose) obj.material.dispose()
    })

    const loader = new PLYLoader()
    const geometry = await new Promise((resolve, reject) => {
      loader.load(url, resolve, undefined, reject)
    })

    geometry.computeVertexNormals()

    const material = new THREE.MeshStandardMaterial({
      color: 0x22c55e,
      metalness: 0.1,
      roughness: 0.4,
      side: THREE.DoubleSide,
    })

    const mesh = new THREE.Mesh(geometry, material)
    mesh.userData.__isLoadedMesh = true
    scene.add(mesh)

    fitCameraToObject(camera, mesh, controls)
  } catch (e) {
    console.error('[MeshViewer] mesh load failed:', e)
    loadError.value = e?.message || String(e)
  } finally {
    loading.value = false
  }
}


onMounted(async () => {
  await ensureInitialized()
  // load the initial mesh after mount
  if (props.meshUrl) loadMesh(props.meshUrl)
})

watch(() => props.meshUrl, (u) => {
  if (u) loadMesh(u)
})
onBeforeUnmount(() => cleanup())
</script>

<template>
  <div
    style="position:relative; border:1px solid #1f2937; border-radius:14px; background:#0b1020; padding:16px; height:100%; box-sizing:border-box;">
    <div style="margin-bottom:10px; font-weight:600; color:#e5e7eb;">3D View</div>

    <div ref="container" style="width:100%; height:680px; border-radius:10px; overflow:hidden; background:#000;"></div>

    <div v-if="loading"
      style="position:absolute; inset:16px; display:flex; align-items:center; justify-content:center; background:rgba(11,16,32,0.65); border-radius:10px; color:#e5e7eb;">
      Loading 3D…
    </div>

    <div v-if="loadError"
      style="position:absolute; left:16px; right:16px; bottom:16px; padding:10px; background:rgba(255,0,0,0.12); border:1px solid #ffb3b3; border-radius:10px; color:#fecaca;">
      <b>3D load error:</b> {{ loadError }}
    </div>
  </div>
</template>


<style scoped>
.card {
  position: relative;
  border: 1px solid #1f2937;
  border-radius: 14px;
  background: #0b1020;
  padding: 16px;
  height: 100%;
  box-sizing: border-box;
}

.top {
  margin-bottom: 10px;
  font-weight: 600;
}

.viewport {
  width: 100%;
  height: 680px;
  border-radius: 10px;
  overflow: hidden;
  background: #000;
}

.overlay {
  position: absolute;
  inset: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(11, 16, 32, 0.65);
  border-radius: 10px;
}

.err {
  position: absolute;
  left: 16px;
  right: 16px;
  bottom: 16px;
  padding: 10px;
  background: rgba(255, 0, 0, 0.12);
  border: 1px solid #ffb3b3;
  border-radius: 10px;
}
</style>