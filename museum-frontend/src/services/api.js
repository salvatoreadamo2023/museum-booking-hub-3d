import axios from 'axios'
import { azzeraSessione } from '@/store/session'

// Indirizzo del backend dal file .env (VITE_API_BASE_URL): cambia tra locale e produzione senza toccare il codice.
// withCredentials: il browser invia i cookie di sessione (HttpOnly) anche verso il backend su un'altra porta.
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
})

// Rinnovo in corso: più richieste che falliscono insieme aspettano lo stesso rinnovo
let rinnovo = null

// Accesso scaduto (401): si prova una volta a rinnovarlo con il refresh token, poi si ripete la richiesta.
// Se anche il rinnovo fallisce si torna al login. Le chiamate /auth/* gestiscono il 401 da sole.
api.interceptors.response.use(
  (risposta) => risposta,
  async (errore) => {
    const config = errore.config
    const url = config?.url ?? ''
    if (errore.response?.status !== 401 || !config || url.startsWith('/auth/') || config.giaRiprovata) {
      return Promise.reject(errore)
    }
    config.giaRiprovata = true
    try {
      rinnovo ??= api.post('/auth/refresh').finally(() => {
        rinnovo = null
      })
      await rinnovo
      return api(config)
    } catch {
      azzeraSessione()
      if (window.location.pathname !== '/login') window.location.assign('/login')
      return Promise.reject(errore)
    }
  },
)

/** Testo da mostrare all'utente per un errore di una chiamata API. */
export function messaggioErrore(errore, predefinito = 'Operazione non riuscita') {
  if (!errore.response) return 'Server non raggiungibile: controlla che il backend sia avviato'
  const dati = errore.response.data
  if (dati?.campi) return Object.values(dati.campi).join(' · ')
  return dati?.messaggio ?? predefinito
}

export default api
