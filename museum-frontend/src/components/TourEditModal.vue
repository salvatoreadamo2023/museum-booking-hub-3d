<script setup>
import { ref } from 'vue'
import BaseModal from './BaseModal.vue'
import { tourService } from '@/services/tourService'
import { messaggioErrore } from '@/services/api'

const props = defineProps({ tour: { type: Object, default: null } }) // null = nuovo itinerario
const emit = defineEmits(['close', 'salvato'])

const nuovo = !props.tour
const titolo = ref(props.tour?.titolo ?? '')
const descrizione = ref(props.tour?.descrizione ?? '')
const durataMin = ref(props.tour?.durataMin ?? 60)
const prezzo = ref(props.tour?.prezzo ?? 12)
const capienza = ref(props.tour?.capienza ?? 20)
// solo le fasce attive: quelle tolte in passato restano nel database per le prenotazioni già fatte
const orari = ref(props.tour ? props.tour.orari.filter((o) => o.attivo).map((o) => o.orario) : ['10:30', '16:00'])
const nuovoOrario = ref('')
const errore = ref('')
const inCorso = ref(false)
const confermaElimina = ref(false)

function aggiungiOrario() {
  if (nuovoOrario.value && !orari.value.includes(nuovoOrario.value)) {
    orari.value = [...orari.value, nuovoOrario.value].sort()
  }
  nuovoOrario.value = ''
}

function togliOrario(orario) {
  orari.value = orari.value.filter((o) => o !== orario)
}

async function salva() {
  errore.value = ''
  if (orari.value.length === 0) {
    errore.value = 'Aggiungi almeno una fascia oraria'
    return
  }
  const corpo = {
    titolo: titolo.value.trim(),
    descrizione: descrizione.value.trim(),
    durataMin: durataMin.value,
    prezzo: prezzo.value,
    capienza: capienza.value,
    orari: orari.value,
  }
  inCorso.value = true
  try {
    if (nuovo) await tourService.crea(corpo)
    else await tourService.aggiorna(props.tour.id, corpo)
    emit('salvato')
  } catch (e) {
    errore.value = messaggioErrore(e)
  } finally {
    inCorso.value = false
  }
}

async function elimina() {
  if (!confermaElimina.value) {
    confermaElimina.value = true
    return
  }
  errore.value = ''
  inCorso.value = true
  try {
    await tourService.elimina(props.tour.id)
    emit('salvato')
  } catch (e) {
    errore.value = messaggioErrore(e)
    confermaElimina.value = false
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <BaseModal
    :title="nuovo ? 'Nuovo itinerario' : 'Modifica itinerario'"
    accent="#b45309"
    subtitle="Dettagli, capienza e fasce orarie"
    @close="emit('close')"
  >
    <form @submit.prevent="salva">
      <p v-if="errore" class="form-error">{{ errore }}</p>
      <div class="field">
        <label for="tour-titolo">Titolo</label>
        <input id="tour-titolo" v-model="titolo" type="text" required maxlength="150" />
      </div>
      <div class="field">
        <label for="tour-descrizione">Descrizione</label>
        <textarea id="tour-descrizione" v-model="descrizione" rows="2" maxlength="500"></textarea>
      </div>
      <div class="field" style="display: flex; gap: 10px">
        <div style="flex: 1">
          <label for="tour-durata">Durata (minuti)</label>
          <input id="tour-durata" v-model.number="durataMin" type="number" min="1" max="600" required />
        </div>
        <div style="flex: 1">
          <label for="tour-prezzo">Prezzo intero (€)</label>
          <input id="tour-prezzo" v-model.number="prezzo" type="number" step="0.01" min="0.01" required />
        </div>
      </div>
      <div class="field">
        <label for="tour-capienza">Capienza massima</label>
        <input id="tour-capienza" v-model.number="capienza" type="number" min="1" max="20" required />
        <div class="form-hint">Da 1 a 20 posti.</div>
      </div>
      <div class="field">
        <label for="tour-orario">Fasce orarie</label>
        <div style="margin-bottom: 8px">
          <span v-for="o in orari" :key="o" class="slot-chip">
            {{ o }}<button type="button" :aria-label="`Togli la fascia ${o}`" @click="togliOrario(o)">×</button>
          </span>
          <span v-if="orari.length === 0" class="form-hint">Nessuna fascia: aggiungine almeno una.</span>
        </div>
        <div style="display: flex; gap: 8px">
          <input id="tour-orario" v-model="nuovoOrario" type="time" style="flex: 1" @keydown.enter.prevent="aggiungiOrario" />
          <button type="button" class="btn-outline" @click="aggiungiOrario">+ Aggiungi</button>
        </div>
        <div v-if="!nuovo" class="form-hint">Togliendo una fascia, le prenotazioni già fatte in quella fascia restano valide.</div>
      </div>
      <div style="display: flex; gap: 10px; margin-top: 24px; flex-wrap: wrap">
        <button type="submit" class="btn-action" style="flex: 1" :disabled="inCorso">Salva itinerario</button>
        <button
          v-if="!nuovo"
          type="button"
          class="btn-outline"
          style="color: #b91c1c; border-color: #fca5a5; font-weight: 700"
          :disabled="inCorso"
          @click="elimina"
        >
          {{ confermaElimina ? 'Sì, elimina davvero' : 'Elimina itinerario' }}
        </button>
      </div>
    </form>
  </BaseModal>
</template>
