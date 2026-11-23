<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import { tourService } from '@/services/tourService'
import { messaggioErrore } from '@/services/api'
import { iniziaPrenotazione } from '@/store/booking'
import { formatEuro, oggiIso, oraAttuale } from '@/utils/formatters'
import { precaricaSala } from '@/sala3d/precarica'

const router = useRouter()

const COLORI = ['#b45309', '#0f766e', '#1e3a8a']

const oggi = oggiIso()
const data = ref(oggi)
const tours = ref([])
const scelta = ref({ tourId: null, orario: null })
const errore = ref('')
const caricamento = ref(false)
let ultimaRichiesta = 0 // se si cambia data velocemente conta solo l'ultima risposta

async function carica() {
  errore.value = ''
  scelta.value = { tourId: null, orario: null }
  if (!data.value) {
    tours.value = []
    return
  }
  const richiesta = ++ultimaRichiesta
  caricamento.value = true
  try {
    const risultato = await tourService.elenca(data.value)
    if (richiesta === ultimaRichiesta) tours.value = risultato
  } catch (e) {
    if (richiesta === ultimaRichiesta) errore.value = messaggioErrore(e, 'Impossibile caricare gli itinerari')
  } finally {
    if (richiesta === ultimaRichiesta) caricamento.value = false
  }
}

onMounted(carica)
watch(data, carica)

// Fascia non prenotabile: disattivata, piena, oppure già iniziata se la data è oggi
function stato(slot) {
  if (!slot.attivo) return 'Non attivo'
  if (data.value === oggi && slot.orario <= oraAttuale()) return 'Passato'
  if (slot.liberi === 0) return 'Pieno'
  return null
}

function etichetta(slot) {
  return stato(slot) ? `${slot.orario} (${stato(slot)})` : `${slot.orario} (${slot.liberi} ${slot.liberi === 1 ? 'libero' : 'liberi'})`
}

function seleziona(tour, slot) {
  scelta.value = { tourId: tour.id, orario: slot.orario }
  precaricaSala()
}

function vaiAllaMappa(tour) {
  iniziaPrenotazione(tour, data.value, scelta.value.orario)
  router.push('/mappa')
}
</script>

<template>
  <section class="page-section active">
    <PageHeader title="Itinerari & Fasce Orarie" subtitle="Scegli la data e seleziona una fascia oraria per accedere alla Mappa 3D e riservare i posti" />

    <div class="field date-field">
      <label>Data della visita</label>
      <input v-model="data" type="date" :min="oggi" required />
    </div>

    <p v-if="errore" class="form-error">{{ errore }}</p>
    <div v-if="!caricamento && !errore && tours.length === 0" class="card empty-state">Nessun itinerario disponibile.</div>

    <div v-for="(tour, i) in tours" :key="tour.id" class="card" :style="{ borderLeft: `4px solid ${COLORI[i % COLORI.length]}` }">
      <h3 style="font-family: 'Lora', serif; font-size: 1.4rem; margin-bottom: 8px">{{ i + 1 }}. {{ tour.titolo }}</h3>
      <p style="font-size: 0.88rem; color: #64748b; margin-bottom: 8px">{{ tour.descrizione }}</p>
      <p style="font-size: 0.78rem; color: #64748b; margin-bottom: 20px">
        {{ tour.durataMin }} min ·
        <span v-for="(t, k) in tour.tariffe" :key="t.id">{{ k ? ' · ' : '' }}{{ t.nome }} {{ formatEuro(t.prezzo) }}</span>
      </p>
      <div style="font-size: 0.72rem; font-weight: 700; text-transform: uppercase; color: #334155; margin-bottom: 10px">Seleziona una fascia oraria:</div>
      <div style="display: flex; gap: 10px; margin-bottom: 20px; flex-wrap: wrap">
        <button
          v-for="slot in tour.orari"
          :key="slot.orario"
          class="btn-outline slot-btn"
          :class="{ selected: scelta.tourId === tour.id && scelta.orario === slot.orario, disabled: stato(slot) }"
          :disabled="!!stato(slot)"
          @click="seleziona(tour, slot)"
        >
          {{ etichetta(slot) }}
        </button>
      </div>
      <div v-if="scelta.tourId === tour.id">
        <button class="btn-action" :style="{ background: COLORI[i % COLORI.length] }" @click="vaiAllaMappa(tour)">VAI ALLA MAPPA 3D →</button>
      </div>
    </div>
  </section>
</template>
