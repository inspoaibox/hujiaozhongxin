import { createRouter, createWebHistory } from 'vue-router'
import RealtimeDashboard from '@/views/dashboard/RealtimeDashboard.vue'
import LoginView from '@/views/LoginView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: RealtimeDashboard,
      meta: { requiresAuth: true }
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView
    }
  ]
})

router.beforeEach(to => {
  if (to.meta.requiresAuth && !localStorage.getItem('token')) {
    return '/login'
  }
  if (to.path === '/login' && localStorage.getItem('token')) {
    return '/'
  }
  return true
})

export default router
