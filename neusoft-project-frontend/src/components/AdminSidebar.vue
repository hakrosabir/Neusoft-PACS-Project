<template>
  <aside class="sidebar">

    <!-- Brand -->
    <div class="brand-section">
      <div class="logo-box">A</div>
      <div class="brand-info">
        <span class="brand-name">NeuroPACS</span>
        <span class="brand-sub">System Control</span>
      </div>
    </div>

    <!-- Navigation -->
    <nav class="nav-links">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        :class="['nav-item', { active: currentTab === tab.id }]"
        @click="$emit('tab-change', tab.id)"
      >
        <span class="icon">{{ tab.icon }}</span>
        <span class="label">{{ tab.name }}</span>
      </button>
    </nav>

    <!-- Logged-in admin info + logout -->
    <div class="user-section">
      <div class="user-info">
        <div class="avatar-circle admin">S</div>
        <div class="user-text">
          <span class="dr-name">Super Admin</span>
          <span class="dr-role">Root Access</span>
        </div>
      </div>
      <button @click="$emit('logout')" class="logout-btn" title="Sign Out">➜</button>
    </div>

  </aside>
</template>

<script setup>
// Props received from AdminDashboard.vue
defineProps({
  currentTab: { type: String, required: true }
})

// Events sent up to AdminDashboard.vue
defineEmits(['tab-change', 'logout'])

const tabs = [
  { id: 'overview', name: 'Mission Control', icon: '🚀' },
  { id: 'users',    name: 'User Management', icon: '👥' },
  { id: 'logs',     name: 'System Logs',     icon: '📜' },
]
</script>

<style scoped>
.sidebar {
  width: 260px;
  background-color: #020617;
  border-right: 1px solid #1e293b;
  display: flex;
  flex-direction: column;
  height: 100vh;
}

/* Brand */
.brand-section {
  height: 75px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 12px;
  border-bottom: 1px solid #1e293b;
}
.logo-box {
  width: 36px; height: 36px;
  background: #3b82f6;
  border-radius: 6px;
  display: flex; align-items: center; justify-content: center;
  font-weight: 800; color: white;
}
.brand-name { font-weight: 700; color: white; font-size: 1rem; }
.brand-sub  { font-size: 0.7rem; color: #64748b; }
.brand-info { display: flex; flex-direction: column; }

/* Nav */
.nav-links { padding: 20px 0; flex: 1; }
.nav-item {
  display: flex; align-items: center;
  width: 100%; padding: 14px 24px;
  color: #94a3b8; background: none; border: none;
  cursor: pointer; transition: 0.2s;
  gap: 12px; text-align: left; font-size: 0.9rem;
}
.nav-item:hover { background: #0f172a; color: #cbd5e1; }
.nav-item.active {
  background: #1e293b;
  color: #60a5fa;
  border-right: 3px solid #60a5fa;
}
.icon  { font-size: 1.1rem; }
.label { font-weight: 600; }

/* User / Logout */
.user-section {
  padding: 20px;
  border-top: 1px solid #1e293b;
  display: flex; justify-content: space-between; align-items: center;
}
.user-info { display: flex; align-items: center; }
.avatar-circle.admin {
  width: 38px; height: 38px;
  background: #ef4444; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-weight: bold; color: white;
}
.user-text  { display: flex; flex-direction: column; margin-left: 10px; }
.dr-name    { font-weight: 600; font-size: 0.85rem; color: white; }
.dr-role    { font-size: 0.7rem; color: #ef4444; }
.logout-btn { background: none; border: none; color: #64748b; cursor: pointer; font-size: 1.2rem; }
.logout-btn:hover { color: white; }
</style>