import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('../views/LoginView.vue') },
    { path: '/register', component: () => import('../views/RegisterView.vue') },
    { path: '/', component: () => import('../views/DashboardView.vue'), meta: { requiresAuth: true } },
    { path: '/profile', component: () => import('../views/ProfileView.vue'), meta: { requiresAuth: true } },
    { path: '/checkin', component: () => import('../views/CheckinView.vue'), meta: { requiresAuth: true } },
    { path: '/forum', component: () => import('../views/ForumView.vue'), meta: { requiresAuth: true } },
    { path: '/user-center', component: () => import('../views/UserCenterView.vue'), meta: { requiresAuth: true } },
    { path: '/notifications', component: () => import('../views/NotificationView.vue'), meta: { requiresAuth: true } },
    { path: '/admin', component: () => import('../views/AdminView.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) return '/login'
  if ((to.path === '/login' || to.path === '/register') && token) return '/'
  if (to.meta.requiresAdmin) {
    const user = JSON.parse(localStorage.getItem('user') || 'null')
    if (!user || user.role !== 'ADMIN') return '/'
  }
})

export default router
