<script setup>
import { onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { feedbackService } from '@/services/feedbackService'
import { usePaginazione } from '@/composables/usePaginazione'
import { formatData, stelle } from '@/utils/formatters'

const { elementi: feedback, pagina, dimensione, totaleElementi, totalePagine, caricamento, errore, carica, vaiAPagina, cambiaDimensione } =
  usePaginazione(feedbackService.elenca, { dimensione: 10, erroreBase: 'Impossibile caricare i feedback' })

onMounted(carica)
</script>

<template>
  <section class="page-section active">
    <PageHeader title="Feedback dei visitatori" subtitle="Le recensioni lasciate dopo le visite, dalla più recente" />
    <p v-if="errore" class="form-error">{{ errore }}</p>
    <div v-if="!errore && !caricamento && feedback.length === 0" class="card empty-state">Nessun feedback ricevuto per ora.</div>
    <div v-for="f in feedback" :key="f.id" class="card" style="margin-bottom: 14px">
      <div style="display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; margin-bottom: 8px">
        <strong>{{ f.utente }} · {{ f.tour }}</strong>
        <span class="testo-secondario">{{ formatData((f.data || '').slice(0, 10)) }}</span>
      </div>
      <div style="color: #b45309; font-size: 1.05rem; margin-bottom: 6px" :aria-label="`${f.voto} stelle su 5`">{{ stelle(f.voto) }}</div>
      <p v-if="f.commento" style="font-size: 0.92rem; color: #334155; white-space: pre-line">{{ f.commento }}</p>
      <p v-else class="testo-secondario">Nessun commento, solo il voto.</p>
    </div>
    <div v-if="totaleElementi > 0" class="card">
      <PaginationBar
        nome="Feedback"
        :pagina="pagina"
        :dimensione="dimensione"
        :totale-elementi="totaleElementi"
        :totale-pagine="totalePagine"
        :disabilitata="caricamento"
        @pagina="vaiAPagina"
        @dimensione="cambiaDimensione"
      />
    </div>
  </section>
</template>
