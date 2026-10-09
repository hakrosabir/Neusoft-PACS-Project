<template>
  <div class="logs-view">

    <!-- Filter bar -->
    <div class="log-filter-bar">
      <div class="log-filter-group">
        <span class="log-filter-label">LEVEL:</span>
        <button
          v-for="lvl in levels" :key="lvl"
          :class="['log-pill', { active: logFilter === lvl }, 'lf-' + lvl.toLowerCase()]"
          @click="logFilter = lvl">
          {{ lvl }}
        </button>
      </div>
      <div class="log-filter-group">
        <span class="log-filter-label">EVENT:</span>
        <button
          v-for="cat in categories" :key="cat"
          :class="['log-pill', { active: logCategory === cat }]"
          @click="logCategory = cat">
          {{ cat }}
        </button>
      </div>
      <span class="log-count">{{ filteredLogs.length }} events</span>
    </div>

    <!-- Terminal window -->
    <div class="terminal-window">
      <div class="terminal-header">
        <span class="dot red"></span>
        <span class="dot yellow"></span>
        <span class="dot green"></span>
        <span class="term-title">system.log</span>
        <span class="term-live"><span class="blink">●</span> LIVE</span>
      </div>

      <div class="terminal-body" ref="logBody">
        <!-- Loading state -->
        <div v-if="loading" class="log-line">
          <span class="log-time">[{{ currentTime }}]</span>
          <span class="log-level INFO">INFO</span>
          <span class="log-msg">Fetching audit logs from database...</span>
        </div>

        <!-- Log rows -->
        <div v-for="log in filteredLogs" :key="log.id" class="log-line">
          <span class="log-time">[{{ formatLogTime(log.timestamp) }}]</span>
          <span :class="['log-level', log.level]">{{ log.level }}</span>
          <span class="log-cat">[{{ log.actionType }}]</span>
          <span class="log-msg">{{ log.message }}</span>
        </div>

        <!-- Empty state -->
        <div v-if="!loading && filteredLogs.length === 0" class="log-line">
          <span class="log-msg empty">No log entries match the current filter.</span>
        </div>

        <!-- Blinking cursor -->
        <div class="log-line"><span class="blink cursor">_</span></div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'

const props = defineProps({
  logs:        { type: Array,  default: () => [] },
  loading:     { type: Boolean, default: false },
  currentTime: { type: String,  default: '' },
})

// Local filter state — lives here, not in parent
const logFilter   = ref('ALL')
const logCategory = ref('ALL')
const logBody     = ref(null)

const levels     = ['ALL', 'INFO', 'SUCCESS', 'WARN', 'ERROR']
const categories = ['ALL', 'LOGIN_SUCCESS', 'LOGIN_FAILED', 'REGISTER', 'APPROVE', 'LOCK', 'EDIT', 'DELETE', 'SYSTEM']

const filteredLogs = computed(() =>
  props.logs.filter(log => {
    const levelOk    = logFilter.value   === 'ALL' || log.level      === logFilter.value
    const categoryOk = logCategory.value === 'ALL' || log.actionType === logCategory.value
    return levelOk && categoryOk
  })
)

// Auto-scroll to top whenever new logs arrive
watch(() => props.logs, async () => {
  await nextTick()
  if (logBody.value) logBody.value.scrollTop = 0
})

function formatLogTime(iso) {
  if (!iso) return '??:??:??'
  const d = new Date(iso)
  return d.toLocaleTimeString() + ' ' + d.toLocaleDateString()
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400&display=swap');

.logs-view { display: flex; flex-direction: column; gap: 12px; }

/* Filter bar */
.log-filter-bar { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.log-filter-group { display: flex; align-items: center; gap: 6px; background: #1e293b; padding: 4px 8px; border-radius: 8px; flex-wrap: wrap; }
.log-filter-label { font-size: 0.65rem; color: #64748b; font-weight: 700; letter-spacing: 1px; margin-right: 4px; }
.log-pill { background: transparent; border: none; color: #64748b; padding: 4px 10px; border-radius: 5px; cursor: pointer; font-size: 0.7rem; font-weight: 700; transition: 0.15s; font-family: 'JetBrains Mono', monospace; }
.log-pill:hover, .log-pill.active { color: white; background: #334155; }
.log-pill.lf-success.active { background: rgba(34,197,94,0.25);  color: #4ade80; }
.log-pill.lf-info.active    { background: rgba(56,189,248,0.2);  color: #38bdf8; }
.log-pill.lf-warn.active    { background: rgba(234,179,8,0.2);   color: #facc15; }
.log-pill.lf-error.active   { background: rgba(239,68,68,0.2);   color: #f87171; }
.log-count { margin-left: auto; font-size: 0.72rem; color: #64748b; font-family: 'JetBrains Mono', monospace; }

/* Terminal */
.terminal-window { background: #020617; border: 1px solid #334155; border-radius: 8px; font-family: 'JetBrains Mono', monospace; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
.terminal-header { background: #1e293b; padding: 8px 15px; display: flex; align-items: center; gap: 8px; border-bottom: 1px solid #334155; border-radius: 8px 8px 0 0; }
.dot { width: 12px; height: 12px; border-radius: 50%; }
.dot.red    { background: #ef4444; }
.dot.yellow { background: #eab308; }
.dot.green  { background: #22c55e; }
.term-title { margin-left: 10px; color: #94a3b8; font-size: 0.8rem; }
.term-live  { margin-left: auto; font-size: 0.7rem; color: #ef4444; font-weight: 700; display: flex; align-items: center; gap: 4px; }

.terminal-body { padding: 20px; height: 450px; overflow-y: auto; font-size: 0.8rem; line-height: 1.9; }
.log-line { display: flex; align-items: baseline; gap: 8px; flex-wrap: wrap; }
.log-time  { color: #475569; min-width: 160px; }
.log-level { font-weight: 700; min-width: 64px; display: inline-block; }
.log-level.INFO    { color: #38bdf8; }
.log-level.SUCCESS { color: #4ade80; }
.log-level.WARN    { color: #facc15; }
.log-level.ERROR   { color: #f87171; }
.log-cat  { color: #8b5cf6; font-size: 0.75rem; min-width: 120px; }
.log-msg  { color: #cbd5e1; flex: 1; }
.log-msg.empty { color: #475569; font-style: italic; }
.cursor { color: #60a5fa; font-size: 1rem; }

@keyframes blink { 50% { opacity: 0; } }
.blink { animation: blink 1s infinite; }
</style>