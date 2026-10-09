<template>
  <aside class="sidebar">

    <!-- Brand / logo — click goes back to doctor dashboard -->
    <div class="brand-section" @click="$emit('go-home')">
      <div class="logo-box"><span class="ai-dot"></span></div>
      <div class="brand-info">
        <span class="brand-name">NeuroPACS</span>
        <span class="brand-sub">Diagnosis AI</span>
      </div>
    </div>

    <!-- Navigation tabs -->
    <nav class="nav-links">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        :class="['nav-item', { active: tab.id === 'diagnosis' }]"
        @click="$emit('navigate', tab)"
      >
        <span class="icon">{{ tab.icon }}</span>
        <span class="label">{{ tab.name }}</span>
      </button>
    </nav>

    <!-- Logged-in user info + logout -->
    <div class="user-section">
      <div class="user-info">
        <div class="avatar-circle">{{ (username || 'D').charAt(0) }}</div>
        <div class="user-text">
          <span class="dr-name">Dr. {{ username || 'User' }}</span>
          <span class="dr-role">Radiologist</span>
        </div>
      </div>
      <button @click="$emit('logout')" class="logout-btn" title="Sign Out">➜</button>
    </div>

  </aside>
</template>

<script setup>
// Props from DiagnosisStudio.vue
defineProps({
  username: { type: String, default: '' }
})

// Events sent up to parent
defineEmits(['go-home', 'navigate', 'logout'])

// Nav tabs — same as original file
const tabs = [
  { id: 'appointments', name: 'Appointments',    icon: '📅', route: '/doctor-dashboard' },
  { id: 'reports',      name: 'Reports History', icon: '📄', route: '/doctor-dashboard' },
  { id: 'worklist',     name: 'Worklist',        icon: '📋', route: '/doctor-dashboard' },
  { id: 'diagnosis',    name: 'Diagnosis Studio',icon: '🖥️', route: '/diagnosis' },
  { id: 'analytics',    name: 'Analytics',       icon: '📊', route: '/analytics' },
  { id: 'converter',    name: 'Image Converter', icon: '🔁', route: '/image-converter' },
]
</script>

<style scoped>
.sidebar {
  width: 260px;
  background-color: #001529;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  box-shadow: 2px 0 8px rgba(0,0,0,0.3);
  z-index: 50;
  height: 100vh;
}

/* Brand */
.brand-section {
  height: 64px;
  display: flex; align-items: center; gap: 12px;
  padding: 0 20px;
  background-color: #002140;
  cursor: pointer;
}
.logo-box {
  width: 32px; height: 32px;
  background: #409eff; border-radius: 6px;
  display: flex; align-items: center; justify-content: center;
}
.ai-dot { width: 8px; height: 8px; background: white; border-radius: 50%; }
.brand-info { display: flex; flex-direction: column; }
.brand-name  { color: white; font-weight: 700; font-size: 1rem; }
.brand-sub   { color: #8bb4ff; font-size: 0.7rem; }

/* Nav */
.nav-links {
  flex: 1; padding: 20px 0;
  display: flex; flex-direction: column; gap: 4px;
}
.nav-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 24px;
  background: transparent; border: none;
  color: #a6b0c3; font-size: 0.95rem; font-weight: 500;
  cursor: pointer; width: 100%; text-align: left;
  transition: all 0.2s;
}
.nav-item:hover { background-color: rgba(255,255,255,0.05); color: white; }
.nav-item.active { background-color: #1890ff; color: white; border-right: 3px solid white; }
.icon { font-size: 1.1rem; }

/* User / Logout */
.user-section {
  padding: 20px;
  border-top: 1px solid rgba(255,255,255,0.1);
  display: flex; align-items: center; justify-content: space-between;
}
.user-info { display: flex; align-items: center; gap: 10px; }
.avatar-circle {
  width: 36px; height: 36px;
  background: #2563eb; color: white;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-weight: bold;
}
.user-text  { display: flex; flex-direction: column; }
.dr-name    { color: white; font-size: 0.85rem; font-weight: 600; }
.dr-role    { color: #94a3b8; font-size: 0.75rem; }
.logout-btn { background: none; border: none; color: #f87171; font-size: 1.2rem; cursor: pointer; }
.logout-btn:hover { color: white; }
</style>