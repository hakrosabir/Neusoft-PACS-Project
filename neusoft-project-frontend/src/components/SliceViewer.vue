<script setup>
import { ref, computed, watch } from "vue";

const props = defineProps({
  previews: { type: Object, required: true } // { axial:[], coronal:[], sagittal:[] }
});

const plane = ref("axial");
const slice = ref(0);

const list = computed(() => props.previews?.[plane.value] ?? []);
const maxSlice = computed(() => Math.max(0, list.value.length - 1));
const src = computed(() => list.value[slice.value] ?? "");

watch(plane, () => {
  slice.value = Math.floor((list.value.length || 1) / 2);
}, { immediate: true });

function onSliceInput(e) {
  slice.value = Number(e.target.value);
}
</script>

<template>
  <div class="card">
    <div class="top">
      <h3>2D View</h3>
      <div class="btns">
        <button
          v-for="p in ['axial','coronal','sagittal']"
          :key="p"
          :class="{ active: plane === p }"
          @click="plane = p"
        >
          {{ p }}
        </button>
      </div>
    </div>

    <div v-if="src" class="frame">
      <img :src="src" class="img" />
    </div>
    <div v-else class="err">No preview for {{ plane }}</div>

    <div v-if="list.length" class="controls">
      <div class="row">
        <span>Plane: <b>{{ plane }}</b></span>
        <span>Slice: <b>{{ slice }}</b> / {{ maxSlice }}</span>
      </div>

      <input
        type="range"
        :min="0"
        :max="maxSlice"
        :value="slice"
        @input="onSliceInput"
      />
    </div>
  </div>
</template>

<style scoped>
.card { border:1px solid #1f2937; border-radius:14px; background:#0b1020; padding:16px; box-sizing:border-box; }
.top { display:flex; justify-content:space-between; align-items:center; gap:10px; margin-bottom:10px; }
h3 { margin:0; }
.btns { display:flex; gap:8px; }
button { padding:6px 10px; border-radius:10px; border:1px solid #334155; background:transparent; color:#e5e7eb; cursor:pointer; }
button.active { background:#1f2937; }
.frame { position:relative; width:100%; height:520px; background:black; border-radius:10px; overflow:hidden; }
.img { width:100%; height:100%; object-fit:contain; display:block; }
.controls { margin-top:12px; }
.row { display:flex; justify-content:space-between; font-size:14px; margin-bottom:6px; }
.err { color:#fecaca; margin-top:8px; }
input[type="range"] { width:100%; }
</style>