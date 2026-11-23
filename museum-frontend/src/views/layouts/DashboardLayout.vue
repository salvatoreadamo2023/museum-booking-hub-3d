<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { authService } from '@/services/authService'
import { azzeraSessione, isAdmin, session } from '@/store/session'
import { azzera as azzeraPrenotazione } from '@/store/booking'
import { iniziali } from '@/utils/formatters'

const route = useRoute()
const router = useRouter()

const admin = computed(() => isAdmin())
const ruolo = computed(() => (admin.value ? 'Amministratore' : 'Visitatore'))

async function esci() {
  try {
    await authService.logout() // il server revoca la sessione e cancella i cookie
  } catch {
    // anche se il server non risponde si esce comunque dall'interfaccia
  }
  azzeraSessione()
  router.push('/login')
}
</script>

<template>
  <div id="dashboard-app">
    <aside class="sidebar">
      <div>
        <div style="margin-bottom: 28px">
          <div style="font-size: 1.6rem">🏛️</div>
          <h2 style="font-family: 'Lora', serif; font-size: 1.4rem; color: #1e293b">GALLERIA GEOLOGICA</h2>
          <div style="font-size: 0.68rem; font-weight: 700; text-transform: uppercase; color: #b45309; letter-spacing: 1px">
            Museum Dashboard
          </div>
        </div>
        <nav>
          <!-- Menu Visitatore -->
          <div v-if="!admin">
            <div class="nav-group-title">Area Visitatore</div>
            <ul class="nav-menu">
              <li>
                <RouterLink to="/visite" class="nav-link" active-class="active" :class="{ active: route.path === '/mappa' }">🗺️ Visite &amp; Orari</RouterLink>
              </li>
              <li><RouterLink to="/prenotazioni" class="nav-link" active-class="active">🎟️ Le Mie Prenotazioni</RouterLink></li>
              <li><RouterLink to="/feedback" class="nav-link" active-class="active">💬 Lascia Feedback</RouterLink></li>
            </ul>
          </div>
          <!-- Menu Amministratore -->
          <div v-else>
            <div class="nav-group-title">Area Amministrazione</div>
            <ul class="nav-menu">
              <li><RouterLink to="/admin/registro" class="nav-link" active-class="active">🗺️ Visite &amp; Registro</RouterLink></li>
              <li><RouterLink to="/admin/analytics" class="nav-link" active-class="active">📊 Analytics &amp; KPI</RouterLink></li>
              <li><RouterLink to="/admin/gestione" class="nav-link" active-class="active">⚙️ Gestione Visite &amp; Capienze</RouterLink></li>
              <li><RouterLink to="/admin/utenti" class="nav-link" active-class="active">👥 Amministrazione Utenti</RouterLink></li>
              <li><RouterLink to="/admin/feedback" class="nav-link" active-class="active">💬 Guarda Feedback</RouterLink></li>
              <li><RouterLink to="/admin/sistema" class="nav-link" active-class="active">🖥️ Prestazioni di sistema</RouterLink></li>
            </ul>
          </div>
        </nav>
      </div>
      <div class="user-profile">
        <div style="display: flex; align-items: center; gap: 10px">
          <div class="avatar">{{ iniziali(session.user?.nome) }}</div>
          <div>
            <div style="font-size: 0.82rem; font-weight: 700; color: #1e293b">{{ session.user?.nome }}</div>
            <div style="font-size: 0.7rem; color: #64748b">{{ ruolo }}</div>
          </div>
        </div>
        <button class="logout-btn" @click="esci">Esci</button>
      </div>
    </aside>

    <main class="main-viewport">
      <RouterView />
    </main>
  </div>
</template>
