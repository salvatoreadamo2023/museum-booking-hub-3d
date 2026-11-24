<script setup>
import { computed } from 'vue'
import { descrizionePosti, formatDataOra, formatEuro } from '@/utils/formatters'

const props = defineProps({ prenotazione: { type: Object, required: true } })
defineEmits(['ricevuta', 'disdici', 'feedback'])

const confermata = computed(() => props.prenotazione.stato === 'CONFERMATA')
const posti = computed(() => descrizionePosti(props.prenotazione.posti))
</script>

<template>
  <div class="card" :style="{ borderTop: `4px solid ${confermata ? '#b45309' : '#94a3b8'}` }">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px">
      <div>
        <span style="font-size: 0.88rem; font-weight: 700; color: #475569">
          CODICE: {{ prenotazione.codice }}
        </span>
        <h3 style="font-family: 'Lora', serif; font-size: 1.3rem; color: #b45309; margin-top: 2px">
          {{ prenotazione.tour }}
        </h3>
      </div>
      <span class="badge" :class="confermata ? 'badge-ok' : 'badge-ko'" style="font-size: 0.85rem">
        {{ confermata ? 'CONFERMATA' : 'ANNULLATA' }}
      </span>
    </div>
    <p style="font-size: 0.95rem; margin-bottom: 16px; background: #f8fafc; padding: 12px; border: 1px solid #f1f5f9">
      <strong>Data &amp; Orario:</strong> {{ formatDataOra(prenotazione.data, prenotazione.orario) }} &nbsp;|&nbsp;
      <template v-if="confermata">
        <strong>Posti:</strong> {{ posti }} &nbsp;|&nbsp;
        <strong>Costo Totale:</strong> {{ formatEuro(prenotazione.totale) }}
      </template>
      <template v-else>
        <strong>Posti liberati dalla disdetta</strong>
      </template>
    </p>
    <div v-if="confermata" style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap">
      <button
        style="background: #b45309; border: none; color: #ffffff; padding: 8px 16px; font-weight: 700; font-size: 0.9rem; cursor: pointer; border-radius: 999px"
        @click="$emit('ricevuta', prenotazione)"
      >
        📄 Dettaglio Prenotazione
      </button>
      <button
        v-if="prenotazione.annullabile"
        style="background: #fef2f2; border: 1px solid #fca5a5; color: #991b1b; padding: 8px 16px; font-weight: 700; font-size: 0.9rem; cursor: pointer; border-radius: 999px"
        @click="$emit('disdici', prenotazione)"
      >
        Disdici Prenotazione
      </button>
      <button
        v-if="prenotazione.passata"
        class="btn-outline"
        style="padding: 8px 16px"
        @click="$emit('feedback', prenotazione)"
      >
        💬 Lascia Feedback
      </button>
    </div>
  </div>
</template>
