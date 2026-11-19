const GIORNI = ['Dom', 'Lun', 'Mar', 'Mer', 'Gio', 'Ven', 'Sab']

/** Data di oggi nel formato yyyy-MM-dd, secondo il fuso orario locale. */
export function oggiIso() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** Ora attuale nel formato HH:mm. */
export function oraAttuale() {
  const d = new Date()
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

export function formatEuro(valore) {
  return new Intl.NumberFormat('it-IT', { style: 'currency', currency: 'EUR' }).format(valore ?? 0)
}

/** yyyy-MM-dd -> dd/MM/yyyy */
export function formatData(iso) {
  if (!iso) return ''
  const [anno, mese, giorno] = iso.split('-')
  return `${giorno}/${mese}/${anno}`
}

/** "Oggi - 11:00" oppure "25/10/2026 - 11:00" */
export function formatDataOra(iso, orario) {
  return `${iso === oggiIso() ? 'Oggi' : formatData(iso)} - ${orario}`
}

export function iniziali(nome) {
  return (nome ?? '')
    .split(' ')
    .filter(Boolean)
    .map((parte) => parte[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()
}

/** Lun, Mar... a partire da yyyy-MM-dd */
export function giornoBreve(iso) {
  const [anno, mese, giorno] = iso.split('-').map(Number)
  return GIORNI[new Date(anno, mese - 1, giorno).getDay()]
}

/** dd/MM a partire da yyyy-MM-dd */
export function dataBreve(iso) {
  const [, mese, giorno] = iso.split('-')
  return `${giorno}/${mese}`
}

export function stelle(voto) {
  return '★'.repeat(voto) + '☆'.repeat(5 - voto)
}

/** "2 Omini (#2, #4)" */
export function descrizionePosti(posti) {
  const numeri = posti.map((p) => `#${p.numero}`).join(', ')
  return `${posti.length} ${posti.length === 1 ? 'Omino' : 'Omini'} (${numeri})`
}
