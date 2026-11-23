// Scarica in anticipo three.js e i modelli delle persone, così quando si apre la mappa è quasi tutto pronto.
// Lo chiamo appena l'utente sceglie una fascia oraria: è il segnale che sta per prenotare.
let avviato = false

export function precaricaSala() {
  if (avviato) return
  avviato = true
  import('./salaImmersiva').catch(() => (avviato = false))
  import('./persone')
    .then((m) => m.caricaModelli())
    .catch(() => (avviato = false)) // se fallisce, ci riprova l'apertura della mappa
}
