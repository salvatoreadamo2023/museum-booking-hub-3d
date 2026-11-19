import api from './api'

export const authService = {
  login: (email, password) => api.post('/auth/login', { email, password }).then((r) => r.data),
  register: (nome, email, password) => api.post('/auth/register', { nome, email, password }).then((r) => r.data),
  me: () => api.get('/auth/me').then((r) => r.data),
  refresh: () => api.post('/auth/refresh').then((r) => r.data),
  logout: () => api.post('/auth/logout'),

  /** Recupera l'utente dopo un ricaricamento: prima /auth/me, se l'accesso è scaduto prova il rinnovo. */
  async ripristina() {
    try {
      return await this.me()
    } catch (e) {
      if (e.response?.status === 401) {
        try {
          return await this.refresh()
        } catch {
          return null
        }
      }
      return null
    }
  },
}
