import api from './api'

export const tourService = {
  elenca: (data) => api.get('/tours', { params: data ? { data } : {} }).then((r) => r.data),
  posti: (id, data, orario) => api.get(`/tours/${id}/seats`, { params: { data, orario } }).then((r) => r.data),
  // gestione (admin)
  gestione: (pagina = 0, dimensione = 10) => api.get('/tours/gestione', { params: { pagina, dimensione } }).then((r) => r.data),
  crea: (corpo) => api.post('/tours', corpo).then((r) => r.data),
  aggiorna: (id, corpo) => api.put(`/tours/${id}`, corpo).then((r) => r.data),
  elimina: (id) => api.delete(`/tours/${id}`),
}
