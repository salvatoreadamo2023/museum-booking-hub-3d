import api from './api'

export const reservationService = {
  elenca: () => api.get('/reservations').then((r) => r.data),
  crea: (corpo) => api.post('/reservations', corpo).then((r) => r.data),
  annulla: (id) => api.delete(`/reservations/${id}`).then((r) => r.data),
}
