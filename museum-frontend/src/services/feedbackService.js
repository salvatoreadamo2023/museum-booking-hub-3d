import api from './api'

export const feedbackService = {
  crea: (corpo) => api.post('/feedback', corpo).then((r) => r.data),
}
