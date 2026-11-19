import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { authService } from './services/authService'
import { iniziaSessione } from './store/session'
import './assets/style.css'

// Prima di mostrare l'app si chiede al backend se esiste già una sessione valida (cookie HttpOnly),
// così la guardia del router sa subito se l'utente è collegato e con quale ruolo.
async function avvia() {
  const utente = await authService.ripristina()
  if (utente) iniziaSessione(utente)
  createApp(App).use(router).mount('#app')
}

avvia()
