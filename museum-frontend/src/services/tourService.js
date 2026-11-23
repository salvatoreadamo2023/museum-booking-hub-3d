import api from './api'

export const tourService = {
  elenca: (data) => api.get('/tours', { params: data ? { data } : {} }).then((r) => r.data),
  posti: (id, data, orario) => api.get(`/tours/${id}/seats`, { params: { data, orario } }).then((r) => r.data),
}
