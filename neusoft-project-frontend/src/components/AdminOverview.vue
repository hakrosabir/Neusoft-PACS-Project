<template>
  <div class="command-center">

    <!-- Architecture flow diagram -->
    <div class="architecture-flow">
      <div class="arch-node">
        <div class="node-icon">🖥️</div>
        <div class="node-details">
          <h5>Client Layer</h5>
          <span class="tech">Vue.js 3</span>
          <span class="status-row"><span class="light green"></span> Active</span>
        </div>
      </div>
      <div class="flow-line animated"></div>

      <div :class="['arch-node', backendOnline ? 'active' : 'dead']">
        <div class="node-icon">⚙️</div>
        <div class="node-details">
          <h5>Orchestrator</h5>
          <span class="tech">Spring Boot</span>
          <span class="status-row">
            <span :class="['light', backendOnline ? 'green' : 'red']"></span>
            {{ backendOnline ? 'Online' : 'Unreachable' }}
          </span>
        </div>
      </div>

      <div class="flow-split">
        <div class="flow-line animated"></div>
        <div class="flow-line animated down"></div>
      </div>

      <div class="split-nodes">
        <div :class="['arch-node', dbOnline ? 'active' : 'dead']">
          <div class="node-icon">🗄️</div>
          <div class="node-details">
            <h5>Data Store</h5>
            <span class="tech">MySQL 8</span>
            <span class="status-row">
              <span :class="['light', dbOnline ? 'green' : 'red']"></span>
              {{ dbOnline ? 'Connected' : 'Disconnected' }}
            </span>
          </div>
        </div>

        <div :class="['arch-node', aiOnline ? 'active' : 'dead']">
          <div class="node-icon">🧠</div>
          <div class="node-details">
            <h5>Neural Engine</h5>
            <span class="tech">Python (TotalSeg)</span>
            <span class="status-row">
              <span :class="['light', aiOnline ? 'green' : 'red']"></span>
              {{ aiOnline ? 'Ready' : 'Offline' }}
            </span>
          </div>
        </div>

        <div :class="['arch-node', llmOnline ? 'active' : 'dead']">
          <div class="node-icon">🤖</div>
          <div class="node-details">
            <h5>LLM Server</h5>
            <span class="tech">Ollama (neusoft-ai)</span>
            <span class="status-row">
              <span :class="['light', llmOnline ? 'green' : 'red']"></span>
              {{ llmOnline ? 'Active' : 'Offline' }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- HUD cards -->
    <div class="hud-grid">

      <!-- JVM Backend -->
      <div :class="['hud-card', 'spring-theme', backendOnline ? '' : 'offline']">
        <div class="hud-header">
          <span class="hud-title">JVM BACKEND</span>
          <div :class="['status-badge', backendOnline ? 'green' : 'red']">
            <span class="dot"></span> {{ backendOnline ? 'ONLINE' : 'OFFLINE' }}
          </div>
        </div>
        <div class="hud-content">
          <div class="bar-metric">
            <div class="bar-label">
              <span>Heap Memory</span>
              <span>{{ liveMetrics.jvmHeap }}MB / 2GB</span>
            </div>
            <div class="progress-bar">
              <div class="fill" :style="{ width: (liveMetrics.jvmHeap / 2048 * 100) + '%' }"></div>
            </div>
          </div>
          <div class="data-row">
            <div class="data-item"><span>Threads:</span> <strong>{{ liveMetrics.jvmThreads }}</strong></div>
            <div class="data-item"><span>Uptime:</span> <strong>{{ uptime }}</strong></div>
          </div>
        </div>
      </div>

      <!-- AI Engine -->
      <div :class="['hud-card', 'ai-theme', aiOnline ? '' : 'offline']">
        <div class="hud-header">
          <span class="hud-title">TOTAL SEGMENTATOR</span>
          <div :class="['status-badge', aiOnline ? 'green' : 'red']">
            <span class="dot"></span> {{ aiOnline ? 'ONLINE' : 'OFFLINE' }}
          </div>
        </div>
        <div class="hud-content">
          <div class="data-grid">
            <div class="data-box">
              <span class="lbl">DEVICE</span>
              <strong v-if="aiOnline" :class="isAiGpu ? 'neon-green' : 'neon-orange'">
                {{ isAiGpu ? '🚀 CUDA GPU' : '🐢 CPU' }}
              </strong>
              <strong v-else class="neon-red">DISCONNECTED</strong>
            </div>
            <div class="data-box">
              <span class="lbl">LATENCY</span>
              <strong>{{ aiOnline ? liveMetrics.aiLatency + 'ms' : '--' }}</strong>
            </div>
          </div>
          <div class="status-line mt-3">Backend: Python Flask</div>
        </div>
      </div>

      <!-- LLM Engine -->
      <div :class="['hud-card', 'llm-theme', llmOnline ? '' : 'offline']">
        <div class="hud-header">
          <span class="hud-title">LLM ENGINE (OLLAMA)</span>
          <div :class="['status-badge', llmOnline ? 'green' : 'red']">
            <span class="dot"></span> {{ llmOnline ? 'ONLINE' : 'OFFLINE' }}
          </div>
        </div>
        <div class="hud-content">
          <div class="data-grid">
            <div class="data-box" style="flex:2">
              <span class="lbl">ACTIVE MODEL</span>
              <strong class="neon-blue" style="font-size:0.9rem">{{ llmModelName }}</strong>
            </div>
            <div class="data-box">
              <span class="lbl">VRAM</span>
              <strong :class="llmGpu ? 'neon-green' : 'neon-orange'">{{ llmGpu ? 'GPU' : 'CPU' }}</strong>
            </div>
          </div>
          <div class="bar-metric mt-3">
            <div class="bar-label"><span>Context Load</span><span>{{ liveMetrics.llmLoad }}%</span></div>
            <div class="progress-bar">
              <div class="fill" :style="{ width: liveMetrics.llmLoad + '%' }"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Database -->
      <div :class="['hud-card', 'db-theme', dbOnline ? '' : 'offline']">
        <div class="hud-header">
          <span class="hud-title">DATABASE</span>
          <div :class="['status-badge', dbOnline ? 'green' : 'red']">
            <span class="dot"></span> {{ dbOnline ? 'ONLINE' : 'ERROR' }}
          </div>
        </div>
        <div class="hud-content">
          <div class="stat-big">
            <span class="number">{{ totalUsers }}</span>
            <span class="label">Total Records</span>
          </div>
          <div class="status-line">
            Pool Active:
            <strong :class="dbOnline ? 'text-ok' : 'text-err'">
              {{ dbOnline ? liveMetrics.dbConns : 0 }}
            </strong>
          </div>
        </div>
      </div>

    </div>

    <!-- Quick stats bar -->
    <div class="quick-stats-bar">
      <div class="qs-item">
        <span class="qs-label">PENDING APPROVALS</span>
        <span class="qs-val orange">{{ pendingDoctors }}</span>
      </div>
      <div class="qs-item">
        <span class="qs-label">TOTAL DOCTORS</span>
        <span class="qs-val blue">{{ totalDoctors }}</span>
      </div>
      <div class="qs-item">
        <span class="qs-label">TOTAL PATIENTS</span>
        <span class="qs-val green">{{ totalPatients }}</span>
      </div>
      <div class="qs-item">
        <span class="qs-label">NURSES / TECHNICIANS</span>
        <span class="qs-val teal">{{ totalTechnicians }}</span>
      </div>
      <div class="qs-item">
        <span class="qs-label">ERRORS (24H)</span>
        <span class="qs-val gray">0</span>
      </div>
    </div>

  </div>
</template>

<script setup>
// All data passed in as props from AdminDashboard.vue — no API calls here
defineProps({
  backendOnline:  { type: Boolean, default: false },
  dbOnline:       { type: Boolean, default: false },
  aiOnline:       { type: Boolean, default: false },
  isAiGpu:        { type: Boolean, default: false },
  llmOnline:      { type: Boolean, default: false },
  llmModelName:   { type: String,  default: 'Loading...' },
  llmGpu:         { type: Boolean, default: false },
  liveMetrics:    { type: Object,  default: () => ({}) },
  uptime:         { type: String,  default: '--' },
  totalUsers:     { type: Number,  default: 0 },
  pendingDoctors: { type: Number,  default: 0 },
  totalDoctors:   { type: Number,  default: 0 },
  totalPatients:  { type: Number,  default: 0 },
  totalTechnicians:{ type: Number, default: 0 },
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Rajdhani:wght@500;600;700&display=swap');

.command-center { display: flex; flex-direction: column; gap: 30px; }

/* Architecture flow */
.architecture-flow { display: flex; justify-content: center; align-items: center; gap: 20px; }
.arch-node { background: #1e293b; border: 1px solid #334155; border-radius: 12px; padding: 15px; width: 160px; display: flex; gap: 10px; align-items: center; box-shadow: 0 4px 15px rgba(0,0,0,0.3); transition: all 0.3s; }
.arch-node.active { border-color: #22c55e; box-shadow: 0 0 15px rgba(34,197,94,0.15); }
.arch-node.dead   { border-color: #ef4444; opacity: 0.7; }
.node-icon { font-size: 1.8rem; }
.node-details h5   { margin: 0; color: white; font-family: 'Rajdhani', sans-serif; font-size: 0.9rem; }
.node-details .tech{ font-size: 0.7rem; color: #94a3b8; display: block; }
.status-row { font-size: 0.7rem; font-weight: bold; display: flex; align-items: center; gap: 6px; color: #94a3b8; }
.light { width: 6px; height: 6px; border-radius: 50%; display: inline-block; }
.light.green { background: #22c55e; box-shadow: 0 0 6px #22c55e; }
.light.red   { background: #ef4444; box-shadow: 0 0 6px #ef4444; }
.flow-line { height: 2px; width: 50px; background: #334155; position: relative; overflow: hidden; }
.flow-line.animated::after { content: ''; position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: linear-gradient(90deg, transparent, #60a5fa, transparent); animation: flow 1.5s infinite; }
.flow-split { display: flex; flex-direction: column; gap: 40px; align-items: center; }
.flow-line.down { width: 2px; height: 40px; }
.split-nodes { display: flex; flex-direction: column; gap: 20px; }

/* HUD grid */
.hud-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }
.hud-card { background: rgba(30,41,59,0.6); border: 1px solid #334155; border-radius: 12px; padding: 20px; position: relative; overflow: hidden; }
.hud-card::before { content: ''; position: absolute; top: 0; left: 0; width: 100%; height: 2px; background: linear-gradient(90deg, transparent, var(--tc), transparent); }
.spring-theme { --tc: #22c55e; } .ai-theme { --tc: #eab308; } .llm-theme { --tc: #8b5cf6; } .db-theme { --tc: #3b82f6; }
.hud-card.offline { border-color: #ef4444; opacity: 0.7; }
.hud-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; border-bottom: 1px solid rgba(255,255,255,0.05); padding-bottom: 10px; }
.hud-title { font-family: 'Rajdhani', sans-serif; font-weight: 700; color: var(--tc); letter-spacing: 1px; font-size: 0.9rem; }
.status-badge { display: flex; align-items: center; gap: 6px; font-size: 0.7rem; font-weight: 800; padding: 4px 8px; border-radius: 4px; }
.status-badge.green { background: rgba(34,197,94,0.1); color: #22c55e; border: 1px solid rgba(34,197,94,0.3); }
.status-badge.red   { background: rgba(239,68,68,0.1);  color: #ef4444; border: 1px solid rgba(239,68,68,0.3); }
.dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; box-shadow: 0 0 6px currentColor; }
.bar-metric { margin-bottom: 12px; }
.mt-3 { margin-top: 15px; }
.bar-label { display: flex; justify-content: space-between; font-size: 0.75rem; color: #cbd5e1; margin-bottom: 4px; }
.progress-bar { height: 6px; background: #0f172a; border-radius: 3px; overflow: hidden; }
.progress-bar .fill { height: 100%; background: var(--tc); transition: width 0.5s ease; box-shadow: 0 0 8px var(--tc); }
.data-row, .data-grid { display: flex; justify-content: space-between; font-size: 0.85rem; color: #94a3b8; }
.data-item { display: flex; gap: 6px; font-size: 0.85rem; color: #94a3b8; }
.data-box { background: #0f172a; padding: 10px; border-radius: 6px; text-align: center; min-width: 80px; border: 1px solid #1e293b; flex: 1; }
.data-box .lbl { display: block; font-size: 0.6rem; color: #64748b; margin-bottom: 4px; }
.stat-big { text-align: center; margin-bottom: 10px; }
.stat-big .number { font-size: 2.5rem; font-weight: 800; color: white; }
.stat-big .label   { display: block; font-size: 0.75rem; color: #94a3b8; }
.status-line { text-align: center; font-size: 0.8rem; color: #94a3b8; }
.neon-green  { color: #22c55e; text-shadow: 0 0 8px rgba(34,197,94,0.5); }
.neon-red    { color: #ef4444; }
.neon-blue   { color: #3b82f6; }
.neon-orange { color: #f97316; }
.text-ok { color: #22c55e; } .text-err { color: #ef4444; }

/* Quick stats */
.quick-stats-bar { display: flex; gap: 20px; background: #1e293b; padding: 15px; border-radius: 12px; border: 1px solid #334155; flex-wrap: wrap; }
.qs-item { flex: 1; display: flex; flex-direction: column; align-items: center; border-right: 1px solid #334155; min-width: 80px; }
.qs-item:last-child { border-right: none; }
.qs-label { font-size: 0.7rem; color: #94a3b8; letter-spacing: 1px; font-weight: 600; margin-bottom: 5px; text-align: center; }
.qs-val { font-size: 1.5rem; font-weight: 800; color: white; }
.qs-val.orange { color: #f97316; } .qs-val.blue { color: #3b82f6; } .qs-val.green { color: #22c55e; }
.qs-val.teal   { color: #14b8a6; } .qs-val.gray  { color: #64748b; }

@keyframes flow { 0% { left: -100%; } 100% { left: 100%; } }
</style>