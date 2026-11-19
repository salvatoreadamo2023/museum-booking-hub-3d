import { reactive } from 'vue'

// Sessione dell'utente: modulo reattivo semplice (niente Pinia).
// Qui NON c'è nessun token: il backend lo tiene in cookie HttpOnly che JavaScript non può leggere.
// Dopo un ricaricamento della pagina l'utente si recupera dal backend con GET /auth/me (vedi main.js).
export const session = reactive({
  user: null, // { id, nome, email, ruolo }
})

export function iniziaSessione(auth) {
  session.user = { id: auth.id, nome: auth.nome, email: auth.email, ruolo: auth.ruolo }
}

/** Svuota lo stato locale (il logout sul server è in authService.logout). */
export function azzeraSessione() {
  session.user = null
}

export const isLoggedIn = () => !!session.user
export const isAdmin = () => session.user?.ruolo === 'ADMIN'

/** Pagina iniziale dopo il login: l'admin parte dalle statistiche, il visitatore dagli itinerari. */
export function homePath() {
  return isAdmin() ? '/admin/analytics' : '/visite'
}
