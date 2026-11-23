import api from './api'

export const statsService = {
  /** Statistiche del periodo: "oggi", "7giorni" oppure "mese" (ultimi 30 giorni). */
  periodo: (periodo) => api.get('/stats', { params: { periodo } }).then((r) => r.data),
}
