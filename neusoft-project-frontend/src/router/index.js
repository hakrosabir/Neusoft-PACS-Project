import { createRouter, createWebHistory } from 'vue-router'
import AuthView from '../views/AuthView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'auth',
      component: AuthView
    },
    {
      path: '/doctor-dashboard',
      name: 'doctor-dashboard',
      component: () => import('../views/DoctorDashboard.vue')
    },
    {
      path: '/patient-dashboard',
      name: 'patient-dashboard',
      component: () => import('../views/PatientDashboard.vue')
    },
    {
      path: '/nurse-dashboard',
      name: 'nurse-dashboard',
      component: () => import('../views/NurseDashboard.vue')
    },

    // --- BRAIN ANALYSIS STUDIO ---
    {
      path: '/brain-analysis',
      name: 'brain-analysis',
      component: () => import('../views/BrainAnalysis.vue')
    },

    // --- CT SPLEEN / ICH ANALYSIS ---
    {
      path: '/spleen-analysis',
      name: 'spleen-analysis',
      component: () => import('../views/SpleenAnalysis.vue')
    },

    // --- STANDALONE DIAGNOSIS STUDIO ---
    {
      path: '/diagnosis',
      name: 'diagnosis',
      component: () => import('../views/DiagnosisStudio.vue')
    },

    // --- STANDALONE AI ANALYTICS ---
    {
      path: '/analytics',
      name: 'analytics',
      component: () => import('../views/AiAnalytics.vue')
    },

    // --- STANDALONE IMAGE CONVERTER ---
    {
      path: '/image-converter',
      name: 'image-converter',
      component: () => import('../views/ImageConverter.vue')
    },

    // --- SYSTEM ADMIN DASHBOARD ---
    {
      path: '/admin',
      name: 'admin-dashboard',
      component: () => import('../views/AdminDashboard.vue')
    }
  ]
})

export default router