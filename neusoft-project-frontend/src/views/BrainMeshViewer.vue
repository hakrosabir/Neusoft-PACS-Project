<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'

const props = defineProps({
  url: { type: String, required: true },
})

const container = ref(null)
const loading = ref(false)
const loadError = ref('')

let renderer, scene, camera, controls, frameId, ro
let initialized = false

function cleanup() {
  if (frameId) cancelAnimationFrame(frameId)
  if (ro) ro.disconnect()
  if (controls) controls.dispose()
  if (renderer) { renderer.dispose(); renderer.domElement?.remove(); }
  renderer = scene = camera = null; initialized = false
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
  let cameraZ = Math.abs(maxDim / (2 * Math.tan(fov / 2))) * 1.5
  cam.position.set(center.x, center.y, center.z + cameraZ)
  cam.near = maxDim / 100
  cam.far = maxDim * 100
  cam.updateProjectionMatrix()
  ctrls.target.copy(center)
  ctrls.update()
}

async function ensureInitialized() {
  if (initialized) return
  await nextTick()
  if (!container.value) return
  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x0f172a)
  const w = container.value.clientWidth || 1
  const h = container.value.clientHeight || 1
  camera = new THREE.PerspectiveCamera(45, w / h, 0.1, 1000)
  camera.position.set(0, 0, 200)
  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setSize(w, h)
  container.value.appendChild(renderer.domElement)
  scene.add(new THREE.AmbientLight(0xffffff, 0.5))
  const dirLight = new THREE.DirectionalLight(0xffffff, 1)
  dirLight.position.set(5, 10, 7)
  scene.add(dirLight)
  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  ro = new ResizeObserver(() => resize())
  ro.observe(container.value)
  const animate = () => { frameId = requestAnimationFrame(animate); controls.update(); renderer.render(scene, camera) }
  animate()
  initialized = true
}

async function loadMesh(url) {
  if (!url) return
  await ensureInitialized()
  scene.children = scene.children.filter(c => c.isLight) 
  loadError.value = ''
  loading.value = true
  try {
    const loader = new GLTFLoader()
    const gltf = await new Promise((resolve, reject) => loader.load(url, resolve, undefined, reject))
    const model = gltf.scene
    model.traverse((child) => {
      if (child.isMesh) {
        child.material = new THREE.MeshStandardMaterial({
          color: 0xe2e8f0, roughness: 0.4, metalness: 0.1, side: THREE.DoubleSide
        })
      }
    })
    scene.add(model)
    fitCameraToObject(camera, model, controls)
  } catch (e) {
    loadError.value = "Could not load 3D model."
  } finally {
    loading.value = false
  }
}

onMounted(() => { if (props.url) loadMesh(props.url) })
watch(() => props.url, (u) => loadMesh(u))
onBeforeUnmount(() => cleanup())
</script>

<template>
  <div class="card">
    <div class="top"><h3>3D Surface View</h3></div>
    <div ref="container" class="viewport"></div>
    <div v-if="loading" class="overlay">Loading 3D...</div>
    <div v-if="loadError" class="overlay error">{{ loadError }}</div>
  </div>
</template>

<style scoped>
.card { border: 1px solid #334155; border-radius: 12px; background: #0f172a; padding: 16px; height: 100%; display: flex; flex-direction: column; box-sizing: border-box; }
.top { margin-bottom: 10px; color: #f1f5f9; font-weight: 600; }
.viewport { width: 100%; flex: 1; min-height: 0; border-radius: 8px; overflow: hidden; background: #000; }
.overlay { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.7); color: white; }
.error { color: #fca5a5; }
</style>