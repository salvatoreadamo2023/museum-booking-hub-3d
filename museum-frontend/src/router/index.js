import { createRouter, createWebHistory } from 'vue-router'
import { homePath, isAdmin, isLoggedIn } from '@/store/session'

const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { pubblica: true } },
  {
    path: '/',
    component: () => import('@/views/layouts/DashboardLayout.vue'),
    children: [
      { path: '', redirect: () => homePath() },
      // Pagine provvisorie: le aree vere del visitatore e dell'admin arrivano nelle fasi 16 e 17
      { path: 'visite', name: 'visite', component: () => import('@/views/BenvenutoView.vue'), meta: { ruolo: 'VISITATORE' } },
      { path: 'admin/analytics', name: 'analytics', component: () => import('@/views/BenvenutoView.vue'), meta: { ruolo: 'ADMIN' } },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({ history: createWebHistory(), routes })

// Guardia: senza login si va alla pagina di accesso; ogni area è riservata al proprio ruolo.
router.beforeEach((to) => {
  if (to.meta.pubblica) {
    return isLoggedIn() ? homePath() : true
  }
  if (!isLoggedIn()) return '/login'
  if (to.meta.ruolo && to.meta.ruolo !== (isAdmin() ? 'ADMIN' : 'VISITATORE')) return homePath()
  return true
})

export default router
