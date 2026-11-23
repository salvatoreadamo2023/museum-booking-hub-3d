import api from './api'

export const feedbackService = {
  crea: (corpo) => api.post('/feedback', corpo).then((r) => r.data),
  // elenco per l'admin, dal più recente
  elenca: (pagina = 0, dimensione = 10) => api.get('/feedback', { params: { pagina, dimensione } }).then((r) => r.data),
}
