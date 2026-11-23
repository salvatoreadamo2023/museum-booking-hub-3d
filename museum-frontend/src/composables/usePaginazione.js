import { ref } from 'vue'
import { messaggioErrore } from '@/services/api'

/**
 * Stato e comandi comuni agli elenchi paginati dell'area admin (registro, itinerari, utenti, feedback).
 * `richiedi(pagina, dimensione)` chiama l'API e restituisce { contenuto, totaleElementi, totalePagine }.
 * La pagina 0 è la prima, come nelle API.
 */
export function usePaginazione(richiedi, { dimensione: righe = 20, erroreBase = 'Impossibile caricare i dati' } = {}) {
  const elementi = ref([])
  const pagina = ref(0)
  const dimensione = ref(righe)
  const totaleElementi = ref(0)
  const totalePagine = ref(0)
  const caricamento = ref(false)
  const errore = ref('')
  let ultimaRichiesta = 0

  async function carica() {
    const richiesta = ++ultimaRichiesta
    errore.value = ''
    caricamento.value = true
    try {
      const risposta = await richiedi(pagina.value, dimensione.value)
      if (richiesta !== ultimaRichiesta) return // nel frattempo è partita una richiesta più recente
      // la pagina non esiste più (es. eliminato l'ultimo elemento dell'ultima pagina): si torna all'ultima
      if (risposta.contenuto.length === 0 && risposta.totalePagine > 0 && pagina.value > risposta.totalePagine - 1) {
        pagina.value = risposta.totalePagine - 1
        return carica()
      }
      elementi.value = risposta.contenuto
      totaleElementi.value = risposta.totaleElementi
      totalePagine.value = risposta.totalePagine
    } catch (e) {
      if (richiesta === ultimaRichiesta) errore.value = messaggioErrore(e, erroreBase)
    } finally {
      if (richiesta === ultimaRichiesta) caricamento.value = false
    }
  }

  function vaiAPagina(numero) {
    pagina.value = numero
    return carica()
  }

  function cambiaDimensione(nuoveRighe) {
    dimensione.value = nuoveRighe
    pagina.value = 0
    return carica()
  }

  return { elementi, pagina, dimensione, totaleElementi, totalePagine, caricamento, errore, carica, vaiAPagina, cambiaDimensione }
}
