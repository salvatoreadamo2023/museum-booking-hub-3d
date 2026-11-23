import { createRouter, createWebHistory } from 'vue-router'
import { homePath, isAdmin, isLoggedIn } from '@/store/session'

const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { pubblica: true } },
  {
    path: '/',
    component: () => import('@/views/layouts/DashboardLayout.vue'),
    children: [
      { path: '', redirect: () => homePath() },
      // Area visitatore
      { path: 'visite', name: 'visite', component: () => import('@/views/visitor/ToursView.vue'), meta: { ruolo: 'VISITATORE' } },
      { path: 'mappa', name: 'mappa', component: () => import('@/views/visitor/MapView.vue'), meta: { ruolo: 'VISITATORE' } },
      { path: 'prenotazioni', name: 'prenotazioni', component: () => import('@/views/visitor/ReservationsView.vue'), meta: { ruolo: 'VISITATORE' } },
      { path: 'feedback', name: 'feedback', component: () => import('@/views/visitor/FeedbackView.vue'), meta: { ruolo: 'VISITATORE' } },
      // Pagina provvisoria: l'area admin vera arriva nella fase 17
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
