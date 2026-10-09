<template>
  <aside class="sidebar">

    <!-- Brand -->
    <div class="brand-section" @click="$emit('go-home')">
      <div class="logo-box">
        <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
          <circle cx="9" cy="9" r="4" fill="white" opacity="0.9"/>
          <circle cx="9" cy="9" r="8" stroke="white" stroke-width="1.5" opacity="0.4"/>
          <line x1="9" y1="1" x2="9" y2="17" stroke="white" stroke-width="1" opacity="0.3"/>
          <line x1="1" y1="9" x2="17" y2="9" stroke="white" stroke-width="1" opacity="0.3"/>
        </svg>
      </div>
      <div class="brand-info">
        <span class="brand-name">NeuroPACS</span>
        <span class="brand-sub">Doctor Portal</span>
      </div>
    </div>

    <!-- Nav -->
    <nav class="nav-links">
      <div class="nav-group-label">WORKSPACE</div>
      <button
        v-for="tab in tabs"
        :key="tab.id"
        @click="$emit('navigate', tab)"
        :class="['nav-item', { active: activeId === tab.id }]"
      >
        <span class="nav-icon">{{ tab.icon }}</span>
        <span class="nav-label">{{ tab.name }}</span>
        <span v-if="tab.badge" class="nav-badge">{{ tab.badge }}</span>
        <span v-if="activeId === tab.id" class="active-indicator"></span>
      </button>
    </nav>

    <div class="sidebar-divider"></div>

    <!-- User -->
    <div class="user-section">
      <div class="avatar-ring">
        <div class="avatar-circle">{{ avatarLetter }}</div>
      </div>
      <div class="user-text">
        <span class="dr-name">Dr. {{ username }}</span>
        <span class="dr-role">
          <span class="online-dot"></span>
          Radiologist
        </span>
      </div>
      <button @click="$emit('logout')" class="logout-btn" title="Sign Out">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
          <polyline points="16 17 21 12 16 7"/>
          <line x1="21" y1="12" x2="9" y2="12"/>
        </svg>
      </button>
    </div>

  </aside>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  activeId: { type: String,  default: '' },
  username: { type: String,  default: 'User' },
  badges:   { type: Object,  default: () => ({}) },
})

defineEmits(['go-home', 'navigate', 'logout'])

// ── Single source of truth for ALL doctor-portal tabs ─────────────────
const tabs = computed(() => [
  { id: 'appointments', name: 'Appointments',     icon: '📅', route: '/doctor-dashboard', badge: props.badges.appointments || null },
  { id: 'reports',      name: 'Reports History',  icon: '📋', route: '/doctor-dashboard', badge: null },
  { id: 'worklist',     name: 'Worklist',         icon: '🗂️', route: '/doctor-dashboard', badge: props.badges.worklist || null },
  { id: 'brain',        name: 'Brain Analysis',   icon: '🧠', route: '/brain-analysis',   badge: null },
  { id: 'spleen',       name: 'CT Spleen / ICH',  icon: '🫀', route: '/spleen-analysis',  badge: null },
  { id: 'diagnosis',    name: 'Diagnosis Studio', icon: '🖥️', route: '/diagnosis',        badge: null },
  { id: 'analytics',    name: 'AI Utilities',     icon: '⚙️', route: '/analytics',        badge: null },
  { id: 'converter',    name: 'Image Converter',  icon: '🔁', route: '/image-converter',  badge: null },
])

const avatarLetter = computed(() =>
  (props.username || 'D').charAt(0).toUpperCase()
)
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&display=swap');

.sidebar {
  width: 248px;
  min-width: 248px;
  height: 100vh;
  background: #05101f;
  border-right: 1px solid rgba(255,255,255,0.06);
  display: flex;
  flex-direction: column;
  font-family: 'DM Sans', sans-serif;
  position: relative;
  z-index: 50;
  flex-shrink: 0;
}
.sidebar::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg,
    rgba(37,99,235,0.06) 0%,
    transparent 40%,
    rgba(37,99,235,0.03) 100%);
  pointer-events: none;
}

/* Brand */
.brand-section {
  height: 64px;
  display: flex; align-items: center; gap: 12px; padding: 0 20px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255,255,255,0.05);
  flex-shrink: 0;
  transition: background 0.2s;
}
.brand-section:hover { background: rgba(255,255,255,0.03); }
.logo-box {
  width: 34px; height: 34px;
  background: linear-gradient(135deg, #1d4ed8 0%, #2563eb 50%, #3b82f6 100%);
  border-radius: 9px;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 0 16px rgba(37,99,235,0.4);
  flex-shrink: 0;
}
.brand-info { display: flex; flex-direction: column; gap: 1px; }
.brand-name { color: #f1f5f9; font-size: 0.95rem; font-weight: 700; letter-spacing: -0.2px; }
.brand-sub  { color: #475569; font-size: 0.68rem; font-weight: 500; letter-spacing: 0.4px; text-transform: uppercase; }

/* Nav */
.nav-links {
  flex: 1; padding: 18px 10px 8px;
  display: flex; flex-direction: column; gap: 2px;
  overflow-y: auto; scrollbar-width: none;
}
.nav-links::-webkit-scrollbar { display: none; }

.nav-group-label {
  color: #334155; font-size: 0.62rem; font-weight: 700;
  letter-spacing: 1.2px; padding: 0 12px 10px;
}

.nav-item {
  position: relative;
  display: flex; align-items: center; gap: 11px;
  padding: 10px 12px; border-radius: 8px;
  border: none; background: transparent;
  color: #64748b; font-family: 'DM Sans', sans-serif;
  font-size: 0.875rem; font-weight: 500;
  cursor: pointer; width: 100%; text-align: left;
  transition: all 0.15s ease; outline: none;
}
.nav-item:hover  { background: rgba(255,255,255,0.05); color: #cbd5e1; }
.nav-item.active { background: rgba(37,99,235,0.15); color: #93c5fd; font-weight: 600; }

.nav-icon  { font-size: 1rem; line-height: 1; flex-shrink: 0; }
.nav-label { flex: 1; }

.nav-badge {
  background: #1d4ed8; color: white;
  font-size: 0.65rem; font-weight: 700;
  padding: 2px 6px; border-radius: 999px; line-height: 1.4;
}

.active-indicator {
  position: absolute; right: 0; top: 50%; transform: translateY(-50%);
  width: 3px; height: 60%; background: #3b82f6; border-radius: 2px 0 0 2px;
}

/* Divider */
.sidebar-divider { height: 1px; background: rgba(255,255,255,0.05); margin: 0 16px; flex-shrink: 0; }

/* User */
.user-section {
  padding: 14px 14px 18px;
  display: flex; align-items: center; gap: 10px; flex-shrink: 0;
}
.avatar-ring {
  width: 38px; height: 38px; border-radius: 50%;
  background: linear-gradient(135deg, #1e3a8a, #2563eb);
  padding: 2px; flex-shrink: 0;
}
.avatar-circle {
  width: 100%; height: 100%; border-radius: 50%;
  background: #0f172a;
  display: flex; align-items: center; justify-content: center;
  color: #93c5fd; font-weight: 700; font-size: 0.85rem;
}
.user-text { flex: 1; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.dr-name {
  color: #e2e8f0; font-size: 0.82rem; font-weight: 600;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.dr-role { color: #475569; font-size: 0.72rem; display: flex; align-items: center; gap: 5px; }
.online-dot {
  width: 6px; height: 6px; background: #22c55e; border-radius: 50%;
  box-shadow: 0 0 6px rgba(34,197,94,0.6);
}
.logout-btn {
  background: transparent; border: 1px solid rgba(255,255,255,0.08);
  color: #475569; border-radius: 7px; padding: 6px;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.15s; flex-shrink: 0;
}
.logout-btn:hover { color: #f87171; border-color: rgba(248,113,113,0.3); background: rgba(248,113,113,0.08); }
</style>