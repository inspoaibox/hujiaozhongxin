import { createRouter, createWebHistory } from 'vue-router'
import WorkspaceView from '@/views/WorkspaceView.vue'
import LoginView from '@/views/LoginView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'workspace',
      component: WorkspaceView,
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
