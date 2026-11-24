import api from './api'

export const authService = {
  login: (email, password) => api.post('/auth/login', { email, password }).then((r) => r.data),
  register: (nome, email, password) => api.post('/auth/register', { nome, email, password }).then((r) => r.data),
  me: () => api.get('/auth/me').then((r) => r.data),
  refresh: () => api.post('/auth/refresh').then((r) => r.data),
  logout: () => api.post('/auth/logout'),

  /** Recupera l'utente dopo un ricaricamento: prima /auth/me, se l'accesso è scaduto prova il rinnovo. */
  async ripristina() {
    // Si accetta solo una risposta che sia davvero un utente: se l'indirizzo del backend è sbagliato
    // arriva una pagina HTML, e senza questo controllo l'app si aprirebbe come "collegata" senza nome.
    const valido = (u) => (u && typeof u === 'object' && u.ruolo ? u : null)
    try {
      return valido(await this.me())
    } catch (e) {
      if (e.response?.status === 401) {
        try {
          return valido(await this.refresh())
        } catch {
          return null
        }
      }
      return null
    }
  },
}
