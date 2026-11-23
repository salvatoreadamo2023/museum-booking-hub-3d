<script setup>
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import TourEditModal from '@/components/TourEditModal.vue'
import { tourService } from '@/services/tourService'
import { usePaginazione } from '@/composables/usePaginazione'
import { formatEuro } from '@/utils/formatters'

const { elementi: tours, pagina, dimensione, totaleElementi, totalePagine, caricamento, errore, carica, vaiAPagina, cambiaDimensione } =
  usePaginazione(tourService.gestione, { dimensione: 10, erroreBase: 'Impossibile caricare gli itinerari' })

const modale = ref(false)
const inModifica = ref(null) // null = nuovo itinerario

onMounted(carica)

function apri(tour) {
  inModifica.value = tour
  modale.value = true
}

async function salvato() {
  modale.value = false
  await carica()
}
</script>

<template>
  <section class="page-section active">
    <PageHeader title="Itinerari e capienze" subtitle="Crea e modifica gli itinerari, i posti disponibili e le fasce orarie">
      <button class="btn-action" @click="apri(null)">+ Nuovo itinerario</button>
    </PageHeader>
    <p v-if="errore" class="form-error">{{ errore }}</p>
    <div class="card">
      <div class="tabella-scorrevole">
        <table>
          <thead><tr><th>Itinerario</th><th>Durata</th><th>Prezzo intero</th><th>Capienza</th><th>Fasce orarie</th><th></th></tr></thead>
          <tbody>
            <tr v-for="t in tours" :key="t.id">
              <td><strong>{{ t.titolo }}</strong></td>
              <td>{{ t.durataMin }} min</td>
              <td>{{ formatEuro(t.prezzo) }}</td>
              <td>{{ t.capienza }} posti</td>
              <td>
                <span v-for="o in t.orari.filter((o) => o.attivo)" :key="o.orario" class="slot-chip">{{ o.orario }}</span>
              </td>
              <td><button class="btn-outline" @click="apri(t)">Modifica</button></td>
            </tr>
            <tr v-if="!caricamento && tours.length === 0"><td colspan="6" class="empty-state">Nessun itinerario: creane uno.</td></tr>
          </tbody>
        </table>
      </div>
      <PaginationBar
        v-if="totaleElementi > 0"
        nome="Itinerari"
        :pagina="pagina"
        :dimensione="dimensione"
        :totale-elementi="totaleElementi"
        :totale-pagine="totalePagine"
        :disabilitata="caricamento"
        @pagina="vaiAPagina"
        @dimensione="cambiaDimensione"
      />
    </div>
    <TourEditModal v-if="modale" :tour="inModifica" @close="modale = false" @salvato="salvato" />
  </section>
</template>
