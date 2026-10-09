<template>
  <div class="header-card">
    <h2 class="title">CT + 3D Segmentation Viewer</h2>
    <p class="subtitle">
      Select an uploaded CT folder or file, load the volume to see axial / coronal / sagittal
      slices, and optionally load a 3D segmentation mesh.
    </p>

    <!-- CT Source -->
    <div class="field-group">
      <label class="field-label">CT Source Path</label>
      <div class="row">
        <select v-model="localSelectedPath" class="select-input" @change="$emit('update:selectedPath', localSelectedPath)">
          <option disabled value="">-- Select uploaded CT folder/file --</option>
          <option v-for="item in items" :key="'ct-' + item.path" :value="item.path">
            {{ item.type === 'folder' ? '[Folder] ' : '[File] ' }}{{ item.path }}
          </option>
        </select>

        <button class="small-btn" @click="$emit('upload-ct-file')">Upload File</button>
        <button class="small-btn" @click="$emit('upload-ct-folder')">Upload Folder</button>
        <button class="small-btn" @click="$emit('refresh')" :disabled="isLoadingList">
          {{ isLoadingList ? 'Refreshing...' : 'Refresh' }}
        </button>
      </div>
    </div>

    <!-- Segmentation Source -->
    <div class="field-group">
      <label class="field-label">Segmentation Path (npy / nii.gz / nrrd / folder)</label>
      <div class="row">
        <select v-model="localSegPath" class="select-input" @change="$emit('update:segPath', localSegPath)">
          <option value="">-- Optional: select segmentation file/folder --</option>
          <option v-for="item in items" :key="'seg-' + item.path" :value="item.path">
            {{ item.path }}
          </option>
        </select>
        <button class="small-btn" @click="$emit('upload-seg-file')">Upload Seg</button>
      </div>
    </div>

    <!-- Load buttons -->
    <div class="row">
      <button
        class="primary-btn"
        @click="$emit('load-volume')"
        :disabled="!selectedPath || isLoadingVolume">
        {{ isLoadingVolume ? 'Loading CT Volume...' : 'Load CT Volume' }}
      </button>
      <button
        class="secondary-btn"
        @click="$emit('load-mesh')"
        :disabled="!segPath || isLoadingMesh">
        {{ isLoadingMesh ? 'Loading 3D Mesh...' : 'Load 3D Mesh' }}
      </button>
    </div>

    <!-- Status message -->
    <p v-if="statusMessage" class="status-text">{{ statusMessage }}</p>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  items:          { type: Array,   default: () => [] },
  selectedPath:   { type: String,  default: '' },
  segPath:        { type: String,  default: '' },
  isLoadingList:  { type: Boolean, default: false },
  isLoadingVolume:{ type: Boolean, default: false },
  isLoadingMesh:  { type: Boolean, default: false },
  statusMessage:  { type: String,  default: '' },
})

// Events sent up to DiagnosisStudio.vue
defineEmits([
  'update:selectedPath',
  'update:segPath',
  'upload-ct-file',
  'upload-ct-folder',
  'upload-seg-file',
  'refresh',
  'load-volume',
  'load-mesh',
])

// Local copies so the selects stay reactive (v-model on props not allowed)
const localSelectedPath = ref(props.selectedPath)
const localSegPath      = ref(props.segPath)

// Keep local in sync if parent changes them (e.g. after refresh auto-selects first item)
watch(() => props.selectedPath, v => { localSelectedPath.value = v })
watch(() => props.segPath,      v => { localSegPath.value = v })
</script>

<style scoped>
.header-card {
  background: #020617;
  border-radius: 18px;
  padding: 18px 20px 16px;
  box-shadow:
    0 18px 45px rgba(15,23,42,0.35),
    0 0 0 1px rgba(148,163,184,0.35);
  margin-bottom: 18px;
  color: #e5e7eb;
}

.title    { margin: 0 0 4px; font-size: 1.2rem; font-weight: 600; }
.subtitle { margin: 0 0 8px; font-size: 0.85rem; color: #9ca3af; }

.field-group  { margin-bottom: 10px; }
.field-label  { display: block; font-size: 0.8rem; font-weight: 600; color: #cbd5f5; margin-bottom: 4px; }
.row          { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }

.select-input {
  flex: 1; padding: 6px 10px;
  border-radius: 10px; border: 1px solid #1f2937;
  background: #020617; color: #e5e7eb;
  font-size: 0.85rem; min-width: 0;
}

.small-btn, .primary-btn, .secondary-btn {
  border-radius: 999px; padding: 7px 14px;
  font-size: 0.8rem; cursor: pointer; border: none;
  white-space: nowrap;
}
.small-btn     { background: #111827; color: #e5e7eb; border: 1px solid #1f2937; }
.small-btn:hover { background: #1f2937; }
.primary-btn   { background: linear-gradient(135deg, #2563eb, #1d4ed8); color: white; }
.primary-btn:hover:not(:disabled) { background: linear-gradient(135deg, #1d4ed8, #1e40af); }
.secondary-btn { background: #111827; color: #e5e7eb; border: 1px solid #1f2937; }
.secondary-btn:hover:not(:disabled) { background: #1f2937; }

.primary-btn:disabled,
.secondary-btn:disabled,
.small-btn:disabled { opacity: 0.55; cursor: default; }

.status-text { margin-top: 6px; font-size: 0.8rem; color: #f97316; }
</style>