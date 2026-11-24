<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import KpiBox from '@/components/KpiBox.vue'
import WeeklyChart from '@/components/charts/WeeklyChart.vue'
import ToursShareChart from '@/components/charts/ToursShareChart.vue'
import IncassoChart from '@/components/charts/IncassoChart.vue'
import { statsService } from '@/services/statsService'
import { messaggioErrore } from '@/services/api'
import { dataBreve, formatEuro, giornoBreve } from '@/utils/formatters'

const periodo = ref('7giorni')
const stats = ref(null)
const errore = ref('')
let ultimaRichiesta = 0

async function carica() {
  const richiesta = ++ultimaRichiesta
  errore.value = ''
  try {
    const risposta = await statsService.periodo(periodo.value)
    // cambiando periodo in fretta, una risposta lenta non deve sovrascrivere quella più recente
    if (richiesta === ultimaRichiesta) stats.value = risposta
  } catch (e) {
    if (richiesta === ultimaRichiesta) errore.value = messaggioErrore(e, 'Impossibile caricare le statistiche')
  }
}

onMounted(carica)
watch(periodo, carica)

const numero = (v) => Number(v ?? 0).toLocaleString('it-IT', { maximumFractionDigits: 1 })

function variazione(v) {
  if (v === null || v === undefined) return 'nessun confronto possibile'
  return `${v > 0 ? '+' : ''}${numero(v)}% rispetto al periodo prima`
}
const coloreVar = (v) => (v < 0 ? '#be123c' : v === null || v === undefined ? '#64748b' : '')

const etichette = computed(() =>
  (stats.value?.andamento ?? []).map((g) => (periodo.value === '7giorni' ? giornoBreve(g.data) : dataBreve(g.data))),
)
const biglietti = computed(() => (stats.value?.andamento ?? []).map((g) => g.biglietti))
const incassi = computed(() => (stats.value?.andamento ?? []).map((g) => Number(g.incasso)))
const itinerari = computed(() => (stats.value?.itinerari ?? []).filter((i) => i.biglietti > 0))
const vuoto = computed(() => stats.value && stats.value.biglietti === 0)
const COLORI = ['#b45309', '#0f766e', '#1e3a8a', '#475569', '#7c3aed', '#be123c']
</script>

<template>
  <section class="page-section active">
    <PageHeader title="Analytics & KPI" subtitle="Incassi e affluenza del museo">
      <select v-model="periodo" class="select-inline" aria-label="Periodo">
        <option value="oggi">Oggi</option>
        <option value="7giorni">Ultimi 7 giorni</option>
        <option value="mese">Ultimi 30 giorni</option>
      </select>
    </PageHeader>
    <p v-if="errore" class="form-error">{{ errore }}</p>
    <template v-if="stats">
      <div class="kpi-row">
        <KpiBox icona="💶" label="Incasso totale" :value="formatEuro(stats.incasso)" :sub="variazione(stats.incassoVariazione)" :sub-color="coloreVar(stats.incassoVariazione)" />
        <KpiBox icona="🎟️" label="Biglietti venduti" :value="String(stats.biglietti)" :sub="variazione(stats.bigliettiVariazione)" :sub-color="coloreVar(stats.bigliettiVariazione)" variant="teal" />
        <KpiBox icona="⏰" label="Ora di punta" :value="stats.oraDiPunta ?? '—'" sub="La fascia più richiesta" sub-color="#57534e" variant="cobalt" />
        <KpiBox icona="🪑" label="Posti riservati" :value="`${numero(stats.postiRiservatiPercentuale)}%`" :sub="`${stats.postiRiservati} su ${stats.postiTotali} posti`" sub-color="#57534e" variant="slate" />
      </div>
      <div class="charts-duo-grid">
        <div class="chart-box">
          <h4 style="margin-bottom: 14px">Biglietti giorno per giorno</h4>
          <div style="position: relative; height: 230px">
            <div v-if="vuoto" class="grafico-vuoto"><div><b>🗓️</b>Nessuna visita in questo periodo.<br />Prova con "Ultimi 30 giorni".</div></div>
            <WeeklyChart v-else :etichette="etichette" :valori="biglietti" />
          </div>
        </div>
        <div class="chart-box">
          <h4 style="margin-bottom: 14px">Incasso giorno per giorno</h4>
          <div style="position: relative; height: 230px">
            <div v-if="vuoto" class="grafico-vuoto"><div><b>💶</b>Nessun incasso in questo periodo.</div></div>
            <IncassoChart v-else :etichette="etichette" :valori="incassi" />
          </div>
        </div>
      </div>
      <div class="charts-duo-grid">
        <div class="chart-box">
          <h4 style="margin-bottom: 14px">Biglietti per itinerario</h4>
          <div style="position: relative; height: 220px">
            <ToursShareChart v-if="itinerari.length" :etichette="itinerari.map((i) => i.titolo)" :valori="itinerari.map((i) => i.biglietti)" />
            <div v-else class="grafico-vuoto"><div><b>🧭</b>Nessun biglietto venduto nel periodo.</div></div>
          </div>
        </div>
        <div class="chart-box">
          <h4 style="margin-bottom: 6px">Classifica itinerari</h4>
          <table class="tabella-quote">
            <tbody>
              <tr v-for="(it, k) in itinerari" :key="it.tourId">
                <td style="width: 55%">
                  <strong>{{ it.titolo }}</strong>
                  <div class="barra-quota"><i :style="{ width: `${it.percentuale}%`, background: COLORI[k % COLORI.length] }"></i></div>
                </td>
                <td>{{ it.biglietti }} biglietti</td>
                <td style="text-align: right"><strong>{{ formatEuro(it.incasso) }}</strong></td>
              </tr>
              <tr v-if="!itinerari.length"><td colspan="3" class="empty-state">Ancora nessuna vendita.</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
    <p v-else-if="!errore" class="empty-state">Caricamento delle statistiche…</p>
  </section>
</template>
