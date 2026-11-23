import api from './api'

export const userService = {
  elenca: (pagina = 0, dimensione = 20) => api.get('/users', { params: { pagina, dimensione } }).then((r) => r.data),
  crea: (corpo) => api.post('/users', corpo).then((r) => r.data),
  aggiorna: (id, corpo) => api.put(`/users/${id}`, corpo).then((r) => r.data),
  elimina: (id) => api.delete(`/users/${id}`),
}
