<script setup>
import { computed, ref } from 'vue'
import BaseModal from './BaseModal.vue'
import { reservationService } from '@/services/reservationService'
import { messaggioErrore } from '@/services/api'
import { formatData } from '@/utils/formatters'

const props = defineProps({
  prenotazione: { type: Object, required: true },
  tour: { type: Object, default: null }, // itinerario con fasce orarie e capienza (null se eliminato)
})
const emit = defineEmits(['close', 'salvata'])

const orario = ref(props.prenotazione.orario)
const numeroPosti = ref(props.prenotazione.posti.length)
const errore = ref('')
const inCorso = ref(false)
const confermaAnnullo = ref(false) // il primo clic chiede conferma, il secondo annulla

// fasce attive dell'itinerario, più quella attuale anche se nel frattempo è stata disattivata
const orari = computed(() => {
  const attive = props.tour?.orari.filter((o) => o.attivo).map((o) => o.orario) ?? []
  return attive.includes(props.prenotazione.orario) ? attive : [props.prenotazione.orario, ...attive].sort()
})
const massimo = computed(() => props.tour?.capienza ?? 100)
const invariata = computed(() => orario.value === props.prenotazione.orario && numeroPosti.value === props.prenotazione.posti.length)

async function salva() {
  errore.value = ''
  inCorso.value = true
  try {
    await reservationService.aggiorna(props.prenotazione.id, { orario: orario.value, numeroPosti: numeroPosti.value })
    emit('salvata')
  } catch (e) {
    errore.value = messaggioErrore(e)
  } finally {
    inCorso.value = false
  }
}

async function annulla() {
  if (!confermaAnnullo.value) {
    confermaAnnullo.value = true
    return
  }
  errore.value = ''
  inCorso.value = true
  try {
    await reservationService.annulla(props.prenotazione.id)
    emit('salvata')
  } catch (e) {
    errore.value = messaggioErrore(e)
    confermaAnnullo.value = false
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <BaseModal
    title="Gestisci prenotazione"
    accent="#b45309"
    :subtitle="`${prenotazione.codice} · ${prenotazione.utenteNome} · ${prenotazione.tour} del ${formatData(prenotazione.data)}`"
    @close="emit('close')"
  >
    <form @submit.prevent="salva">
      <p v-if="errore" class="form-error">{{ errore }}</p>
      <div class="field">
        <label for="modifica-orario">Fascia oraria</label>
        <select id="modifica-orario" v-model="orario">
          <option v-for="o in orari" :key="o" :value="o">{{ o }}</option>
        </select>
      </div>
      <div class="field">
        <label for="modifica-posti">Numero di posti</label>
        <input id="modifica-posti" v-model.number="numeroPosti" type="number" min="1" :max="massimo" required />
        <div class="form-hint">
          I posti in più hanno la tariffa intera; togliendone si liberano gli ultimi. Se un posto è già preso nella nuova fascia, ne viene
          assegnato un altro libero.
        </div>
      </div>
      <div style="display: flex; gap: 10px; margin-top: 24px; flex-wrap: wrap">
        <button type="submit" class="btn-action" style="flex: 1" :disabled="inCorso || invariata">Salva modifiche</button>
        <button
          type="button"
          class="btn-outline"
          style="color: #b91c1c; border-color: #fca5a5; font-weight: 700"
          :disabled="inCorso"
          @click="annulla"
        >
          {{ confermaAnnullo ? 'Sì, annulla davvero' : 'Annulla prenotazione' }}
        </button>
      </div>
    </form>
  </BaseModal>
</template>
