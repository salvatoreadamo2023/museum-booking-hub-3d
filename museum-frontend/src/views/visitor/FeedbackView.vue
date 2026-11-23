<script setup>
import { computed, onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import StarRating from '@/components/StarRating.vue'
import { feedbackService } from '@/services/feedbackService'
import { reservationService } from '@/services/reservationService'
import { messaggioErrore } from '@/services/api'

const prenotazioni = ref([])
const tourId = ref(null)
const voto = ref(5)
const commento = ref('')
const errore = ref('')
const inviato = ref(false)
const inCorso = ref(false)

// Itinerari per cui si può lasciare un feedback: quelli di visite confermate già iniziate
const itinerari = computed(() => {
  const mappa = new Map()
  prenotazioni.value
    .filter((p) => p.stato === 'CONFERMATA' && p.passata)
    .forEach((p) => mappa.set(p.tourId, p.tour))
  return [...mappa].map(([id, titolo]) => ({ id, titolo }))
})

onMounted(async () => {
  try {
    prenotazioni.value = await reservationService.elenca()
    tourId.value = itinerari.value[0]?.id ?? null
  } catch (e) {
    errore.value = messaggioErrore(e)
  }
})

async function invia() {
  errore.value = ''
  inviato.value = false
  inCorso.value = true
  try {
    await feedbackService.crea({ tourId: tourId.value, voto: voto.value, commento: commento.value })
    inviato.value = true
    commento.value = ''
    alert("Grazie! Il tuo feedback è stato inviato all'amministrazione del museo.")
  } catch (e) {
    errore.value = messaggioErrore(e)
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <section class="page-section active">
    <PageHeader title="Lascia un Feedback" subtitle="Valuta la tua esperienza di visita nel nostro museo" />
    <div class="card" style="max-width: 600px">
      <p v-if="itinerari.length === 0" class="empty-state" style="padding-top: 0">
        Potrai lasciare un feedback dopo aver completato una visita.
      </p>
      <form v-else @submit.prevent="invia">
        <p v-if="errore" class="form-error">{{ errore }}</p>
        <div class="field">
          <label>Itinerario Visitato</label>
          <select v-model="tourId">
            <option v-for="i in itinerari" :key="i.id" :value="i.id">{{ i.titolo }}</option>
          </select>
        </div>
        <div class="field">
          <label>Valutazione (da 1 a 5 Stelle)</label>
          <StarRating v-model="voto" />
        </div>
        <div class="field">
          <label>Commento / Opinione</label>
          <textarea v-model="commento" rows="4" maxlength="1000" placeholder="Scrivi qui cosa ti è piaciuto di più o eventuali suggerimenti..." required></textarea>
        </div>
        <button type="submit" class="btn-submit" :disabled="inCorso">INVIA RECENSIONE</button>
      </form>
    </div>
  </section>
</template>
