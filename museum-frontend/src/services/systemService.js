import api from './api'

export const systemService = {
  /** Fotografia del backend: processore, memoria, thread e richieste (solo admin). */
  rileva: () => api.get('/admin/sistema').then((r) => r.data),
}
