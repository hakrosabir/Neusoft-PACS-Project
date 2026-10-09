<template>
  <div>
    <!-- ── FAB ───────────────────────────────────────────────────────── -->
    <button class="chat-fab" @click="toggleChat" :class="{ open: isChatOpen }">
      <transition name="icon-swap" mode="out-in">
        <span v-if="!isChatOpen" key="open" class="fab-inner">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
          <span>Health AI</span>
          <span v-if="unreadCount > 0" class="fab-badge">{{ unreadCount }}</span>
        </span>
        <span v-else key="close" class="fab-inner">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </span>
      </transition>
    </button>

    <!-- ── CHAT PANEL ─────────────────────────────────────────────── -->
    <Teleport to="body">
    <transition name="chat-panel">
    <div v-if="isChatOpen" class="chat-panel">

      <!-- Header -->
      <div class="cp-header">
        <div class="cp-avatar">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
          </svg>
        </div>
        <div class="cp-header-info">
          <div class="cp-name">NeuroPACS AI</div>
          <div class="cp-status">
            <span class="status-dot"></span>
            {{ aiStatus }}
          </div>
        </div>
        <div class="cp-header-actions">
          <button class="hdr-btn" @click="clearChat" title="Clear chat">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="1,4 1,10 7,10"/><path d="M3.51 15a9 9 0 1 0 .49-3.51"/>
            </svg>
          </button>
          <button class="hdr-btn close-hdr" @click="isChatOpen = false">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- Messages -->
      <div class="cp-messages" ref="chatBodyRef">

        <!-- Welcome state -->
        <div v-if="chatMessages.length === 0" class="welcome-state">
          <div class="ws-icon">🏥</div>
          <h3>Hi {{ props.user?.username || 'there' }}!</h3>
          <p>I have access to your medical records and can answer questions about your appointments, diagnoses, and reports.</p>
          <div class="quick-chips-initial">
            <button v-for="chip in welcomeChips" :key="chip" class="chip" @click="sendQuick(chip)">
              {{ chip }}
            </button>
          </div>
        </div>

        <!-- Messages list -->
        <template v-else>
          <div
            v-for="(msg, i) in chatMessages"
            :key="i"
            :class="['msg-row', msg.sender]"
          >
            <div v-if="msg.sender === 'bot'" class="msg-avatar-sm">AI</div>
            <div class="msg-content">
              <div class="bubble" v-html="formatMessage(msg.text)"></div>

              <!-- Contextual action chips after bot messages -->
              <div v-if="msg.chips && msg.chips.length" class="msg-chips">
                <button
                  v-for="(chip, ci) in msg.chips"
                  :key="ci"
                  class="chip"
                  :class="{ 'chip-report': chip.type === 'VIEW_REPORT', 'chip-action': chip.type !== 'VIEW_REPORT' }"
                  @click="handleChip(chip)"
                >
                  <span class="chip-icon">{{ chip.icon }}</span>
                  {{ chip.label }}
                </button>
              </div>
            </div>
          </div>

          <!-- Typing indicator -->
          <div v-if="isBotTyping" class="msg-row bot">
            <div class="msg-avatar-sm">AI</div>
            <div class="msg-content">
              <div class="bubble typing-bubble">
                <span class="dot d1"></span>
                <span class="dot d2"></span>
                <span class="dot d3"></span>
              </div>
            </div>
          </div>
        </template>

      </div>

      <!-- Quick chips (context-aware) -->
      <div class="quick-chips-bar" v-if="contextChips.length > 0 && chatMessages.length > 0">
        <button v-for="chip in contextChips" :key="chip" class="chip chip-sm" @click="sendQuick(chip)">
          {{ chip }}
        </button>
      </div>

      <!-- Input area -->
      <div class="cp-input-area">
        <input
          v-model="userQuery"
          @keyup.enter="sendToAI"
          type="text"
          :placeholder="isBotTyping ? 'AI is thinking...' : 'Ask about your health records...'"
          :disabled="isBotTyping"
          ref="inputRef"
        />
        <button class="send-btn" @click="sendToAI" :disabled="!userQuery.trim() || isBotTyping">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path d="M22 2L11 13M22 2L15 22l-4-9-9-4 20-7z"/>
          </svg>
        </button>
      </div>

    </div>
    </transition>
    </Teleport>

    <!-- ── REPORT VIEWER (uses the main ReportSystem) ──────────────── -->
    <!-- Report viewing is delegated to the parent's reportSystemRef.
         If this component is used standalone, it has its own lightweight viewer. -->
    <Teleport to="body">
    <transition name="report-modal">
    <div v-if="showReportViewer" class="report-overlay" @click.self="showReportViewer = false">
      <div class="report-sheet">

        <!-- Toolbar -->
        <div class="rs-toolbar">
          <div class="rst-left">
            <div class="rst-logo">N</div>
            <div>
              <div class="rst-title">Medical Report</div>
              <div class="rst-sub">{{ typeLabel(viewingReport.analysisType) }} · #{{ viewingReport.id }}</div>
            </div>
          </div>
          <div class="rst-right">
            <button class="rst-btn" @click="printReport">🖨 Print</button>
            <button class="rst-btn rst-close" @click="showReportViewer = false">✕</button>
          </div>
        </div>

        <!-- Paper -->
        <div class="rs-paper" id="chat-report-paper">

          <!-- Hospital header -->
          <div class="rp-header">
            <div>
              <div class="rp-brand">NeuroPACS <span>Medical Center</span></div>
              <div class="rp-address">AI-Powered Diagnostic Radiology · Shenyang, China</div>
            </div>
            <div class="rp-meta">
              <div><span>Report</span> <strong>#{{ viewingReport.id }}</strong></div>
              <div><span>Date</span> <strong>{{ formatDate(viewingReport.reportDate) }}</strong></div>
              <div class="rp-finalized">✓ FINALIZED</div>
            </div>
          </div>

          <div class="rp-divider thick"></div>

          <!-- Patient details row -->
          <div class="rp-patient-row">
            <div class="rpd-col">
              <div class="rpd-lbl">PATIENT</div>
              <div class="rpd-val">{{ viewingReport.patientName }}</div>
            </div>
            <div class="rpd-col">
              <div class="rpd-lbl">DOCTOR</div>
              <div class="rpd-val">Dr. {{ viewingReport.doctorName }}</div>
            </div>
            <div class="rpd-col">
              <div class="rpd-lbl">SCAN TYPE</div>
              <div class="rpd-val">
                <span class="rp-type-badge" :class="'rptb-' + normalizeType(viewingReport.analysisType)">
                  {{ typeIcon(viewingReport.analysisType) }} {{ typeLabel(viewingReport.analysisType) }}
                </span>
              </div>
            </div>
            <div class="rpd-col">
              <div class="rpd-lbl">SEVERITY</div>
              <div class="rpd-val">
                <span class="rp-sev-badge" :class="'rpsb-' + (viewingReport.severity || 'routine')">
                  {{ severityLabel(viewingReport.severity) }}
                </span>
              </div>
            </div>
          </div>

          <div class="rp-divider"></div>

          <!-- Scan images -->
          <div v-if="hasImages(viewingReport)" class="rp-images">
            <div class="rpi-title">DIAGNOSTIC IMAGES</div>
            <div class="rpi-row">
              <div class="rpi-box" v-if="viewingReport.axialImage">
                <img :src="viewingReport.axialImage" alt="Axial">
                <span>Axial</span>
              </div>
              <div class="rpi-box" v-if="viewingReport.coronalImage">
                <img :src="viewingReport.coronalImage" alt="Coronal">
                <span>Coronal</span>
              </div>
              <div class="rpi-box" v-if="viewingReport.sagittalImage">
                <img :src="viewingReport.sagittalImage" alt="Sagittal">
                <span>Sagittal</span>
              </div>
              <div class="rpi-box" v-if="viewingReport.mesh3dImage || viewingReport.mesh3DImage">
                <img :src="viewingReport.mesh3dImage || viewingReport.mesh3DImage" alt="3D">
                <span>3D View</span>
              </div>
            </div>
          </div>

          <!-- Findings -->
          <div class="rp-section">
            <div class="rp-sec-title">DIAGNOSIS</div>
            <div class="rp-sec-body">{{ viewingReport.diagnosis || '—' }}</div>
          </div>

          <div class="rp-section" v-if="viewingReport.prescription">
            <div class="rp-sec-title">PRESCRIPTION</div>
            <div class="rp-sec-body">{{ viewingReport.prescription }}</div>
          </div>

          <div class="rp-section" v-if="viewingReport.advice">
            <div class="rp-sec-title">ADVICE & FOLLOW-UP</div>
            <div class="rp-sec-body">{{ viewingReport.advice }}</div>
            <div v-if="viewingReport.followUpDate" class="rp-followup">
              📅 Follow-up: <strong>{{ viewingReport.followUpDate }}</strong>
            </div>
          </div>

          <!-- Footer -->
          <div class="rp-footer">
            <div class="rpf-sig">
              <div class="rpf-line"></div>
              <div class="rpf-name">Dr. {{ viewingReport.doctorName }}</div>
              <div class="rpf-role">Licensed Radiologist</div>
            </div>
            <div class="rpf-legal">
              Electronically signed via NeuroPACS system.<br>
              Valid without physical signature.
            </div>
          </div>

        </div>
      </div>
    </div>
    </transition>
    </Teleport>

  </div>
</template>

<script setup>
import { JAVA_API_URL } from '../config.js'
import { ref, computed, nextTick } from 'vue'
import axios from 'axios'

// ── Props ─────────────────────────────────────────────────────────────────────
const props = defineProps({
  user: { type: Object, required: true, default: () => ({}) },
  // Optional: pass the parent's reportSystemRef to use the full ReportSystem viewer
  reportSystemRef: { type: Object, default: null },
})

const API_BASE   = JAVA_API_URL
const sessionKey = 'session_' + Math.random().toString(36).slice(2)  // unique per page load

// ── State ─────────────────────────────────────────────────────────────────────
const isChatOpen       = ref(false)
const chatMessages     = ref([])
const userQuery        = ref('')
const isBotTyping      = ref(false)
const unreadCount      = ref(0)
const chatBodyRef      = ref(null)
const inputRef         = ref(null)
const aiStatus         = ref('Online · Secure RAG')

// Report viewer state
const showReportViewer = ref(false)
const viewingReport    = ref({})

// All reports cache
const patientReports   = ref([])

// ── Static quick chips ────────────────────────────────────────────────────────
const welcomeChips = [
  'What are my upcoming appointments?',
  'Show my latest diagnosis',
  'What medications was I prescribed?',
  'When is my next follow-up?',
]

const contextChips = computed(() => {
  if (!chatMessages.value.length) return []
  return [
    'Any appointments this week?',
    'Show my reports',
    'What was my last scan?',
  ]
})

// ── Helpers ───────────────────────────────────────────────────────────────────
function normalizeType(t) {
  if (!t) return 'brain'
  const s = String(t).toLowerCase()
  if (s.includes('spleen') || s.includes('ich')) return 'spleen'
  if (s.includes('lung')) return 'lungs'
  return 'brain'
}
function typeIcon(t)  { return { brain:'🧠', spleen:'🫀', lungs:'🫁' }[normalizeType(t)] || '📋' }
function typeLabel(t) { return { brain:'Brain MRI', spleen:'CT Spleen', lungs:'Lung CT' }[normalizeType(t)] || 'Radiology' }
function hasImages(r) { return r.axialImage || r.coronalImage || r.sagittalImage || r.mesh3dImage || r.mesh3DImage }
function severityLabel(s) {
  return { routine:'● Routine', urgent:'▲ Urgent', critical:'■ Critical' }[(s||'routine').toLowerCase()] || '● Routine'
}
function formatDate(d) {
  if (!d) return 'N/A'
  try { return new Date(d).toLocaleDateString('en-GB', { day:'2-digit', month:'short', year:'numeric' }) }
  catch { return String(d).slice(0, 10) }
}

function formatMessage(text) {
  if (!text) return ''
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/\n/g, '<br>')
}

function scrollToBottom() {
  nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
    }
  })
}

// ── Chat control ──────────────────────────────────────────────────────────────
function toggleChat() {
  isChatOpen.value = !isChatOpen.value
  if (isChatOpen.value) {
    unreadCount.value = 0
    nextTick(() => inputRef.value?.focus())
  }
}

function clearChat() {
  chatMessages.value = []
  patientReports.value = []
}

// ── Send a quick-chip message ─────────────────────────────────────────────────
async function sendQuick(text) {
  userQuery.value = text
  await sendToAI()
}

// ── Main send ─────────────────────────────────────────────────────────────────
async function sendToAI() {
  const text = userQuery.value.trim()
  if (!text || isBotTyping.value) return

  chatMessages.value.push({ sender: 'user', text })
  userQuery.value = ''
  isBotTyping.value = true
  aiStatus.value = 'Analyzing your records...'
  scrollToBottom()

  try {
    const res = await axios.post(`${API_BASE}/api/chat/ask`, {
      message:   text,
      patientId: props.user?.id || null,
      sessionId: sessionKey,
    })

    let reply    = res.data.reply    || "I couldn't process that request."
    let action   = res.data.action   || ''
    let actionId = res.data.actionId || ''

    // Strip leftover think tags
    reply = reply.replace(/<think>[\s\S]*?<\/think>/g, '').trim()

    // Build smart chip suggestions based on reply content
    const chips = buildChips(reply, action, actionId)

    chatMessages.value.push({ sender: 'bot', text: reply, chips })

    // If backend says SHOW_REPORT, open the report automatically
    if (action === 'SHOW_REPORT' && actionId) {
      await openReportById(actionId)
    }

  } catch (e) {
    console.error(e)
    chatMessages.value.push({
      sender: 'bot',
      text: '⚠️ Error connecting to the AI service. Please try again.',
      chips: []
    })
  } finally {
    isBotTyping.value = false
    aiStatus.value = 'Online · Secure RAG'
    scrollToBottom()
    if (!isChatOpen.value) unreadCount.value++
  }
}

// Build context-aware chips based on bot reply content
function buildChips(reply, action, actionId) {
  const chips = []
  const low = reply.toLowerCase()

  // If AI mentions a report but didn't auto-show it, offer to view
  if ((low.includes('report') || low.includes('diagnosis') || low.includes('scan'))
      && action !== 'SHOW_REPORT') {
    chips.push({ icon: '📄', label: 'View My Report', type: 'VIEW_REPORT', id: null })
  }

  if (low.includes('appointment') || low.includes('scheduled') || low.includes('booking')) {
    chips.push({ icon: '📅', label: 'All My Appointments', type: 'QUICK_MSG', msg: 'List all my appointments' })
  }

  if (low.includes('prescription') || low.includes('medication') || low.includes('medicine')) {
    chips.push({ icon: '💊', label: 'My Prescriptions', type: 'QUICK_MSG', msg: 'What medications was I prescribed?' })
  }

  if (low.includes('follow') || low.includes('follow-up') || low.includes('next visit')) {
    chips.push({ icon: '📆', label: 'Follow-up Info', type: 'QUICK_MSG', msg: 'When is my next follow-up appointment?' })
  }

  return chips
}

// ── Chip handler ──────────────────────────────────────────────────────────────
async function handleChip(chip) {
  if (chip.type === 'VIEW_REPORT') {
    await fetchAndShowLatestReport()
  } else if (chip.type === 'QUICK_MSG') {
    userQuery.value = chip.msg
    await sendToAI()
  }
}

// ── Report loading ────────────────────────────────────────────────────────────
async function fetchAndShowLatestReport() {
  if (!props.user?.id) {
    chatMessages.value.push({ sender: 'bot', text: '⚠️ Cannot identify your account. Please log in again.', chips: [] })
    return
  }
  chatMessages.value.push({ sender: 'bot', text: '🔄 Loading your latest report...', chips: [] })
  scrollToBottom()

  try {
    const res = await axios.get(`${API_BASE}/reports/patient/${props.user.id}`)
    if (res.data && res.data.length > 0) {
      patientReports.value = res.data
      openReportInViewer(res.data[0])
      chatMessages.value.push({
        sender: 'bot',
        text: `✅ Your most recent **${typeLabel(res.data[0].analysisType)}** report is now open.\n\nYou have **${res.data.length}** report(s) on file.`,
        chips: res.data.length > 1
          ? res.data.slice(1, 4).map((r, i) => ({
              icon: typeIcon(r.analysisType),
              label: `Report #${r.id} · ${formatDate(r.reportDate)}`,
              type: 'SHOW_SPECIFIC',
              id: r.id,
            }))
          : []
      })
    } else {
      chatMessages.value.push({ sender: 'bot', text: '📋 No finalized medical reports found in your account yet.', chips: [] })
    }
  } catch (e) {
    chatMessages.value.push({ sender: 'bot', text: '⚠️ Error loading your reports. Please try again.', chips: [] })
  }
  scrollToBottom()
}

async function openReportById(reportId) {
  try {
    // Check cache first
    let report = patientReports.value.find(r => String(r.id) === String(reportId))
    if (!report) {
      const res = await axios.get(`${API_BASE}/reports/patient/${props.user.id}`)
      if (res.data) {
        patientReports.value = res.data
        report = res.data.find(r => String(r.id) === String(reportId))
      }
    }
    if (report) {
      openReportInViewer(report)
    }
  } catch (e) {
    console.error('Error opening report by id:', e)
  }
}

function openReportInViewer(report) {
  // If parent passed a reportSystemRef, use the full ReportSystem viewer
  if (props.reportSystemRef?.openViewer) {
    props.reportSystemRef.openViewer(report, {})
  } else {
    // Fallback: use our built-in lightweight viewer
    viewingReport.value = report
    showReportViewer.value = true
  }
}

function printReport() {
  const style = document.createElement('style')
  style.id = 'chat-print-style'
  style.textContent = `
    @media print {
      body > * { display: none !important; }
      #chat-report-paper {
        display: block !important;
        position: fixed; top: 0; left: 0; right: 0;
        padding: 30px; background: white; color: black;
        -webkit-print-color-adjust: exact; print-color-adjust: exact;
      }
    }
  `
  document.head.appendChild(style)
  window.print()
  setTimeout(() => document.getElementById('chat-print-style')?.remove(), 2000)
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Sora:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap');

/* ── Variables ─────────────────────────────────────────────────────────────── */
:root {
  --c-teal:      #0d9488;
  --c-teal-dk:   #0f766e;
  --c-teal-lt:   #ccfbf1;
  --c-surface:   #ffffff;
  --c-bg:        #f0fdfa;
  --c-border:    #e2e8f0;
  --c-text:      #0f172a;
  --c-muted:     #64748b;
  --c-user-bg:   #0d9488;
  --c-bot-bg:    #ffffff;
  --radius-lg:   16px;
  --radius-md:   10px;
  --font:        'Sora', sans-serif;
}

/* ── FAB ─────────────────────────────────────────────────────────────────── */
.chat-fab {
  position: fixed; bottom: 28px; right: 28px; z-index: 2100;
  background: linear-gradient(135deg, #0d9488, #0891b2);
  color: white; border: none; border-radius: 28px;
  padding: 12px 20px; cursor: pointer;
  font-family: var(--font); font-weight: 600; font-size: 0.88rem;
  box-shadow: 0 8px 24px rgba(13,148,136,.4);
  transition: all 0.25s cubic-bezier(.34,1.56,.64,1);
}
.chat-fab:hover { transform: translateY(-2px); box-shadow: 0 12px 28px rgba(13,148,136,.5); }
.chat-fab.open  { border-radius: 50%; padding: 14px; background: #334155; box-shadow: 0 4px 12px rgba(0,0,0,.2); }
.fab-inner { display: flex; align-items: center; gap: 8px; position: relative; }
.fab-badge {
  position: absolute; top: -8px; right: -8px;
  background: #ef4444; color: white; font-size: 0.65rem; font-weight: 700;
  min-width: 16px; height: 16px; border-radius: 99px;
  display: flex; align-items: center; justify-content: center; padding: 0 3px;
}
.icon-swap-enter-active, .icon-swap-leave-active { transition: all .15s; }
.icon-swap-enter-from { opacity: 0; transform: scale(.7); }
.icon-swap-leave-to   { opacity: 0; transform: scale(.7); }

/* ── Chat Panel ──────────────────────────────────────────────────────────── */
.chat-panel {
  position: fixed; bottom: 96px; right: 28px;
  width: 380px; height: 560px;
  background: var(--c-surface); border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0,0,0,.18), 0 0 0 1px rgba(0,0,0,.05);
  display: flex; flex-direction: column; z-index: 2099;
  font-family: var(--font); overflow: hidden;
}
.chat-panel-enter-active { transition: all .3s cubic-bezier(.34,1.56,.64,1); }
.chat-panel-leave-active  { transition: all .2s ease-in; }
.chat-panel-enter-from { opacity: 0; transform: translateY(20px) scale(.95); }
.chat-panel-leave-to   { opacity: 0; transform: translateY(10px) scale(.97); }

/* Header */
.cp-header {
  background: linear-gradient(135deg, #0d9488, #0891b2);
  padding: 14px 16px; display: flex; align-items: center; gap: 10px; flex-shrink: 0;
}
.cp-avatar {
  width: 36px; height: 36px; background: rgba(255,255,255,.2);
  border-radius: 10px; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.cp-name     { color: white; font-weight: 700; font-size: 0.88rem; }
.cp-status   { color: rgba(255,255,255,.75); font-size: 0.72rem; display: flex; align-items: center; gap: 5px; margin-top: 1px; }
.status-dot  { width: 6px; height: 6px; background: #4ade80; border-radius: 50%; animation: pulse-dot 2s infinite; }
@keyframes pulse-dot { 0%,100%{opacity:1;} 50%{opacity:.4;} }
.cp-header-info { flex: 1; }
.cp-header-actions { display: flex; gap: 6px; }
.hdr-btn {
  background: rgba(255,255,255,.15); border: none; color: white;
  width: 28px; height: 28px; border-radius: 7px; cursor: pointer;
  display: flex; align-items: center; justify-content: center; transition: background .15s;
}
.hdr-btn:hover { background: rgba(255,255,255,.25); }
.hdr-btn.close-hdr:hover { background: rgba(239,68,68,.5); }

/* Messages */
.cp-messages {
  flex: 1; overflow-y: auto; padding: 16px;
  display: flex; flex-direction: column; gap: 12px;
  scroll-behavior: smooth;
}
.cp-messages::-webkit-scrollbar { width: 4px; }
.cp-messages::-webkit-scrollbar-track { background: transparent; }
.cp-messages::-webkit-scrollbar-thumb { background: #e2e8f0; border-radius: 99px; }

/* Welcome state */
.welcome-state {
  display: flex; flex-direction: column; align-items: center;
  text-align: center; padding: 20px 10px; gap: 10px;
}
.ws-icon { font-size: 2.5rem; }
.welcome-state h3 { margin: 0; font-size: 1rem; font-weight: 700; color: var(--c-text); }
.welcome-state p  { margin: 0; font-size: 0.8rem; color: var(--c-muted); line-height: 1.5; }
.quick-chips-initial { display: flex; flex-wrap: wrap; gap: 7px; justify-content: center; margin-top: 6px; }

/* Message rows */
.msg-row {
  display: flex; gap: 8px; align-items: flex-end;
  animation: msgIn .2s ease-out;
}
@keyframes msgIn { from { opacity:0; transform: translateY(8px); } to { opacity:1; transform: translateY(0); } }
.msg-row.user { justify-content: flex-end; }
.msg-row.bot  { justify-content: flex-start; }

.msg-avatar-sm {
  width: 24px; height: 24px; background: linear-gradient(135deg,#0d9488,#0891b2);
  border-radius: 7px; color: white; font-size: 0.6rem; font-weight: 800;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0; margin-bottom: 2px;
}

.msg-content { display: flex; flex-direction: column; gap: 6px; max-width: 82%; }
.msg-row.user .msg-content { align-items: flex-end; }

.bubble {
  padding: 10px 13px; border-radius: 14px;
  font-size: 0.84rem; line-height: 1.55; word-break: break-word;
}
.msg-row.user .bubble {
  background: var(--c-user-bg); color: white;
  border-bottom-right-radius: 4px;
}
.msg-row.bot .bubble {
  background: var(--c-bot-bg); color: var(--c-text);
  border: 1px solid var(--c-border); border-bottom-left-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,.05);
}

/* Typing bubble */
.typing-bubble {
  display: flex; align-items: center; gap: 4px; padding: 12px 16px;
}
.dot {
  width: 6px; height: 6px; background: #94a3b8; border-radius: 50%;
  animation: bounce .9s infinite;
}
.d1 { animation-delay: 0s; }
.d2 { animation-delay: .15s; }
.d3 { animation-delay: .3s; }
@keyframes bounce { 0%,60%,100%{transform:translateY(0);} 30%{transform:translateY(-5px);} }

/* Chips */
.msg-chips { display: flex; flex-wrap: wrap; gap: 6px; }
.chip {
  background: #f0fdfa; border: 1px solid #99f6e4; color: #0f766e;
  padding: 6px 11px; border-radius: 99px; font-size: 0.76rem; font-weight: 600;
  cursor: pointer; font-family: var(--font); transition: all .15s; white-space: nowrap;
}
.chip:hover { background: #0d9488; color: white; border-color: #0d9488; transform: translateY(-1px); }
.chip-report { background: #eff6ff; border-color: #bfdbfe; color: #1d4ed8; }
.chip-report:hover { background: #1d4ed8; color: white; border-color: #1d4ed8; }
.chip-sm { font-size: 0.72rem; padding: 5px 9px; }

/* Quick chips bar */
.quick-chips-bar {
  padding: 8px 14px 4px; display: flex; gap: 6px; overflow-x: auto; flex-shrink: 0;
  border-top: 1px solid #f1f5f9;
}
.quick-chips-bar::-webkit-scrollbar { display: none; }

/* Input */
.cp-input-area {
  padding: 12px 14px; background: white;
  border-top: 1px solid #f1f5f9; display: flex; gap: 8px; align-items: center; flex-shrink: 0;
}
.cp-input-area input {
  flex: 1; padding: 10px 14px; background: #f8fafc;
  border: 1px solid var(--c-border); border-radius: 99px;
  font-size: 0.84rem; font-family: var(--font); outline: none; transition: all .15s;
}
.cp-input-area input:focus { border-color: #0d9488; background: white; box-shadow: 0 0 0 3px rgba(13,148,136,.1); }
.cp-input-area input:disabled { opacity: .6; }
.send-btn {
  width: 38px; height: 38px; background: #0d9488; color: white;
  border: none; border-radius: 50%; cursor: pointer; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; transition: all .15s;
}
.send-btn:hover:not(:disabled) { background: #0f766e; transform: scale(1.05); }
.send-btn:disabled { background: #cbd5e1; cursor: not-allowed; }

/* ── Report Viewer ──────────────────────────────────────────────────────────── */
.report-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,.55);
  backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center;
  z-index: 3000;
}
.report-modal-enter-active { transition: all .25s; }
.report-modal-leave-active  { transition: all .15s; }
.report-modal-enter-from { opacity:0; }
.report-modal-leave-to   { opacity:0; }

.report-sheet {
  width: min(210mm, 94vw); height: 88vh; background: #f8fafc;
  border-radius: 14px; overflow: hidden; display: flex; flex-direction: column;
  box-shadow: 0 24px 80px rgba(0,0,0,.3);
}

/* Sheet toolbar */
.rs-toolbar {
  background: #1e293b; color: white;
  padding: 12px 18px; display: flex; align-items: center; gap: 12px; flex-shrink: 0;
}
.rst-logo {
  width: 32px; height: 32px; background: #0d9488; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-weight: 800; font-size: 1rem; flex-shrink: 0;
}
.rst-left  { display: flex; align-items: center; gap: 10px; flex: 1; }
.rst-title { font-weight: 700; font-size: 0.9rem; }
.rst-sub   { font-size: 0.72rem; opacity: .6; margin-top: 1px; }
.rst-right { display: flex; gap: 8px; }
.rst-btn {
  background: rgba(255,255,255,.12); border: none; color: white;
  padding: 6px 12px; border-radius: 6px; cursor: pointer; font-size: 0.78rem; font-weight: 600; transition: .15s;
}
.rst-btn:hover { background: rgba(255,255,255,.22); }
.rst-close:hover { background: rgba(239,68,68,.5); }

/* Paper */
.rs-paper {
  flex: 1; overflow-y: auto; padding: 36px 40px;
  background: white; font-family: 'Georgia', serif; color: #111;
}
.rs-paper::-webkit-scrollbar { width: 5px; }
.rs-paper::-webkit-scrollbar-thumb { background: #e2e8f0; }

/* Paper sections */
.rp-header {
  display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px;
}
.rp-brand { font-size: 1.6rem; font-weight: 900; color: #1e3a8a; font-family: var(--font); }
.rp-brand span { font-weight: 400; color: #64748b; font-size: 1.1rem; }
.rp-address { font-size: 0.78rem; color: #64748b; margin-top: 4px; }
.rp-meta { text-align: right; font-size: 0.82rem; line-height: 1.8; }
.rp-meta span { color: #64748b; margin-right: 5px; }
.rp-finalized { color: #166534; font-weight: 700; font-family: var(--font); }

.rp-divider       { border-bottom: 1px solid #e2e8f0; margin: 14px 0; }
.rp-divider.thick { border-bottom: 2px solid #1e3a8a; margin: 16px 0 20px; }

.rp-patient-row { display: grid; grid-template-columns: repeat(4,1fr); gap: 16px; margin-bottom: 4px; }
.rpd-lbl { font-size: 0.68rem; font-weight: 700; text-transform: uppercase; letter-spacing: .5px; color: #94a3b8; font-family: var(--font); margin-bottom: 3px; }
.rpd-val { font-size: 0.9rem; font-weight: 600; color: #111; font-family: var(--font); }

.rp-type-badge { padding: 3px 9px; border-radius: 5px; font-size: 0.75rem; font-weight: 700; font-family: var(--font); }
.rptb-brain  { background: #dbeafe; color: #1d4ed8; }
.rptb-spleen { background: #fce7f3; color: #9d174d; }
.rptb-lungs  { background: #d1fae5; color: #065f46; }

.rp-sev-badge { font-size: 0.8rem; font-weight: 700; font-family: var(--font); }
.rpsb-routine  { color: #15803d; }
.rpsb-urgent   { color: #b45309; }
.rpsb-critical { color: #dc2626; }

/* Images */
.rp-images { margin: 20px 0; }
.rpi-title  { font-size: 0.72rem; font-weight: 800; text-transform: uppercase; letter-spacing: .6px; color: #1e3a8a; margin-bottom: 12px; font-family: var(--font); border-left: 3px solid #1e3a8a; padding-left: 8px; }
.rpi-row    { display: flex; gap: 12px; }
.rpi-box    { flex: 1; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; text-align: center; }
.rpi-box img{ width: 100%; height: 120px; object-fit: contain; background: #000; display: block; }
.rpi-box span{ font-size: 0.7rem; color: #64748b; padding: 4px 0; display: block; font-family: var(--font); }

/* Sections */
.rp-section   { margin: 18px 0; }
.rp-sec-title { font-size: 0.72rem; font-weight: 800; text-transform: uppercase; letter-spacing: .6px; color: #1e3a8a; border-left: 3px solid #1e3a8a; padding-left: 8px; margin-bottom: 8px; font-family: var(--font); }
.rp-sec-body  { font-size: 0.9rem; line-height: 1.65; white-space: pre-wrap; color: #1e293b; }
.rp-followup  { font-size: 0.82rem; color: #0f766e; margin-top: 8px; font-family: var(--font); font-weight: 600; }

/* Footer */
.rp-footer { margin-top: 50px; display: flex; justify-content: space-between; align-items: flex-end; }
.rpf-sig    { text-align: center; min-width: 180px; }
.rpf-line   { border-bottom: 1px solid #1e293b; margin-bottom: 6px; }
.rpf-name   { font-weight: 700; font-size: 0.9rem; margin: 0; font-family: var(--font); }
.rpf-role   { font-size: 0.78rem; color: #64748b; margin: 2px 0 0; font-family: var(--font); }
.rpf-legal  { font-size: 0.68rem; color: #94a3b8; max-width: 45%; text-align: right; line-height: 1.5; font-family: var(--font); }
</style>