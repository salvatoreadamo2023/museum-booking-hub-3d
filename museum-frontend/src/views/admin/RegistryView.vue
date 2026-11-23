<script setup>
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import ReservationEditModal from '@/components/ReservationEditModal.vue'
import { reservationService } from '@/services/reservationService'
import { tourService } from '@/services/tourService'
import { messaggioErrore } from '@/services/api'
import { usePaginazione } from '@/composables/usePaginazione'
import { formatData, formatEuro } from '@/utils/formatters'

const { elementi: prenotazioni, pagina, dimensione, totaleElementi, totalePagine, caricamento, errore, carica, vaiAPagina, cambiaDimensione } =
  usePaginazione(reservationService.registro, { dimensione: 20, erroreBase: 'Impossibile caricare il registro' })

const tours = ref([])
const erroreTour = ref('')
const inModifica = ref(null)

onMounted(async () => {
  carica()
  try {
    tours.value = await tourService.elenca() // servono fasce e capienza per la modifica
  } catch (e) {
    erroreTour.value = messaggioErrore(e, 'Impossibile caricare gli itinerari')
  }
})

const tourDi = (p) => tours.value.find((t) => t.id === p.tourId) ?? null

function stato(p) {
  if (p.stato !== 'CONFERMATA') return { testo: 'Annullata', classe: 'badge-ko' }
  if (p.passata) return { testo: 'Svolta', classe: 'badge-visitor' }
  return { testo: 'Confermata', classe: 'badge-ok' }
}

async function salvata() {
  inModifica.value = null
  await carica() // si resta sulla stessa pagina
}
</script>

<template>
  <section class="page-section active">
    <PageHeader title="Registro prenotazioni" subtitle="Tutte le prenotazioni dei visitatori, dalla più recente" />
    <p v-if="errore || erroreTour" class="form-error">{{ errore || erroreTour }}</p>
    <div class="card">
      <div class="tabella-scorrevole">
        <table>
          <thead>
            <tr><th>Codice</th><th>Visitatore</th><th>Itinerario</th><th>Data</th><th>Orario</th><th>Posti</th><th>Totale</th><th>Stato</th><th></th></tr>
          </thead>
          <tbody>
            <tr v-for="p in prenotazioni" :key="p.id">
              <td><strong>{{ p.codice }}</strong></td>
              <td>{{ p.utenteNome }}<br /><span class="testo-secondario">{{ p.utenteEmail }}</span></td>
              <td>{{ p.tour }}</td>
              <td>{{ formatData(p.data) }}</td>
              <td>{{ p.orario }}</td>
              <td>{{ p.posti.map((s) => s.numero).join(', ') || '—' }}</td>
              <td>{{ formatEuro(p.totale) }}</td>
              <td><span class="badge" :class="stato(p).classe">{{ stato(p).testo }}</span></td>
              <td>
                <button v-if="p.stato === 'CONFERMATA' && !p.passata" class="btn-outline" :disabled="!tours.length" @click="inModifica = p">
                  Modifica
                </button>
              </td>
            </tr>
            <tr v-if="!caricamento && prenotazioni.length === 0"><td colspan="9" class="empty-state">Nessuna prenotazione.</td></tr>
          </tbody>
        </table>
      </div>
      <PaginationBar
        v-if="totaleElementi > 0"
        nome="Prenotazioni"
        :pagina="pagina"
        :dimensione="dimensione"
        :totale-elementi="totaleElementi"
        :totale-pagine="totalePagine"
        :disabilitata="caricamento"
        @pagina="vaiAPagina"
        @dimensione="cambiaDimensione"
      />
    </div>
    <ReservationEditModal v-if="inModifica" :prenotazione="inModifica" :tour="tourDi(inModifica)" @close="inModifica = null" @salvata="salvata" />
  </section>
</template>
