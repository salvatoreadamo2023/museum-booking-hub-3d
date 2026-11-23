import api from './api'

export const reservationService = {
  elenca: () => api.get('/reservations').then((r) => r.data),
  crea: (corpo) => api.post('/reservations', corpo).then((r) => r.data),
  annulla: (id) => api.delete(`/reservations/${id}`).then((r) => r.data),
  // registro completo (admin), una pagina alla volta: { contenuto, pagina, dimensione, totaleElementi, totalePagine }
  registro: (pagina = 0, dimensione = 20) =>
    api.get('/reservations/registro', { params: { pagina, dimensione } }).then((r) => r.data),
  aggiorna: (id, corpo) => api.put(`/reservations/${id}`, corpo).then((r) => r.data),
}
