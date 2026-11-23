import { reactive } from 'vue'

// Prenotazione in corso: itinerario, data, fascia e posti scelti nella mappa 3D.
export const booking = reactive({
  tour: null, // itinerario dalla lista (con capienza e tariffe)
  data: '', // yyyy-MM-dd
  orario: '', // HH:mm
  posti: [], // [{ numero, tariffaId }]
})

export function tariffaPredefinita(tour = booking.tour) {
  return tour?.tariffe.find((t) => t.predefinita) ?? tour?.tariffe[0] ?? null
}

export function iniziaPrenotazione(tour, data, orario) {
  booking.tour = tour
  booking.data = data
  booking.orario = orario
  booking.posti = []
}

export function togglePosto(numero) {
  const indice = booking.posti.findIndex((p) => p.numero === numero)
  if (indice >= 0) {
    booking.posti.splice(indice, 1)
  } else {
    booking.posti.push({ numero, tariffaId: tariffaPredefinita()?.id })
    booking.posti.sort((a, b) => a.numero - b.numero)
  }
}

export function impostaTariffa(numero, tariffaId) {
  const posto = booking.posti.find((p) => p.numero === numero)
  if (posto) posto.tariffaId = tariffaId
}

export function prezzoPosto(posto) {
  return booking.tour?.tariffe.find((t) => t.id === posto.tariffaId)?.prezzo ?? 0
}

export function totale() {
  return booking.posti.reduce((somma, p) => somma + prezzoPosto(p), 0)
}

export function azzera() {
  booking.tour = null
  booking.data = ''
  booking.orario = ''
  booking.posti = []
}
