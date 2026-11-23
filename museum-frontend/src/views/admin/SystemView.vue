<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import KpiBox from '@/components/KpiBox.vue'
import CpuChart from '@/components/charts/CpuChart.vue'
import { systemService } from '@/services/systemService'
import { messaggioErrore } from '@/services/api'

const INTERVALLO_MS = 5000 // ogni quanto si chiede lo stato al backend
const MAX_CAMPIONI = 60 // 60 campioni da 5 secondi = ultimi 5 minuti nel grafico

const dati = ref(null)
const storico = ref([]) // { ora, cpu }
const errore = ref('')
let timer = null
let inCorso = false
let attiva = true

async function aggiorna() {
  // scheda nascosta: niente richieste inutili; se il backend è lento non si accavallano
  if (inCorso || document.hidden) return
  inCorso = true
  try {
    const r = await systemService.rileva()
    if (!attiva) return // si è cambiata pagina mentre la richiesta era in corso
    dati.value = r
    errore.value = ''
    storico.value = [...storico.value, { ora: new Date(r.rilevatoIl).toLocaleTimeString('it-IT'), cpu: r.processore.cpuPercentuale }].slice(
      -MAX_CAMPIONI,
    )
  } catch (e) {
    if (attiva) errore.value = messaggioErrore(e, 'Impossibile leggere lo stato del sistema')
  } finally {
    inCorso = false
  }
}

onMounted(() => {
  aggiorna()
  timer = setInterval(aggiorna, INTERVALLO_MS)
})
onUnmounted(() => {
  attiva = false
  clearInterval(timer)
})

const percentuale = (v) => (v === null || v === undefined ? 'n.d.' : `${v.toLocaleString('it-IT', { maximumFractionDigits: 1 })} %`)
const ms = (v) => `${Number(v ?? 0).toLocaleString('it-IT', { maximumFractionDigits: 1 })} ms`

const heapPercentuale = computed(() => {
  const m = dati.value?.memoria
  return m && m.heapMassimoMb > 0 ? (m.heapUsatoMb / m.heapMassimoMb) * 100 : 0
})
const aggiornatoAlle = computed(() => (dati.value ? new Date(dati.value.rilevatoIl).toLocaleTimeString('it-IT') : '—'))
</script>

<template>
  <section class="page-section active">
    <PageHeader title="Prestazioni di sistema" subtitle="Lo stato del backend in tempo reale: CPU, memoria, thread e tempi di risposta">
      <span class="testo-secondario">Aggiornato alle {{ aggiornatoAlle }} · ogni {{ INTERVALLO_MS / 1000 }} secondi</span>
    </PageHeader>
    <p v-if="errore" class="form-error">{{ errore }}</p>
    <template v-if="dati">
      <div class="sys-section">
        <h4>Processore e memoria</h4>
        <p>
          I core lavorano in parallelo e ogni thread serve una richiesta. L'heap è la memoria dove Java crea gli oggetti; il garbage collector la
          libera fermando l'esecuzione per qualche millisecondo.
        </p>
      </div>
      <div class="kpi-row">
        <KpiBox label="Core logici" :value="String(dati.processore.core)" sub="Lavori eseguiti davvero in parallelo" sub-color="#64748b" />
        <KpiBox label="CPU del backend" :value="percentuale(dati.processore.cpuPercentuale)" sub="Quota usata da questo processo" sub-color="#64748b" variant="teal" />
        <KpiBox
          label="Heap Java"
          :value="`${dati.memoria.heapUsatoMb} / ${dati.memoria.heapMassimoMb} MB`"
          :sub="`${heapPercentuale.toFixed(0)} % del massimo`"
          sub-color="#64748b"
          variant="cobalt"
        />
        <KpiBox
          label="Garbage collector"
          :value="`${dati.memoria.gcConteggio} cicli`"
          :sub="`${dati.memoria.gcTempoMs} ms di pausa in totale`"
          sub-color="#64748b"
          variant="slate"
        />
      </div>
      <div class="chart-box" style="margin-top: 14px">
        <h4 style="margin-bottom: 14px">CPU negli ultimi minuti</h4>
        <div style="position: relative; height: 220px">
          <CpuChart :etichette="storico.map((c) => c.ora)" :valori="storico.map((c) => c.cpu)" />
        </div>
      </div>
      <div class="sys-section">
        <h4>Richieste alle API</h4>
        <p>Numero di richieste servite dall'avvio e tempi di risposta. Le chiamate di questa pagina non sono contate.</p>
      </div>
      <div class="kpi-row">
        <KpiBox label="Richieste servite" :value="String(dati.richieste.totali)" sub="Dall'avvio del backend" sub-color="#64748b" />
        <KpiBox label="Tempo medio" :value="ms(dati.richieste.tempoMedioMs)" sub="Per ogni richiesta" sub-color="#64748b" variant="teal" />
        <KpiBox label="Tempo massimo" :value="ms(dati.richieste.tempoMassimoMs)" sub="La richiesta più lenta" sub-color="#64748b" variant="cobalt" />
        <KpiBox label="Thread attivi" :value="String(dati.concorrenza.threadAttivi)" sub="Al lavoro in questo momento" sub-color="#64748b" variant="slate" />
      </div>
      <p class="sys-note">
        Sono misure del processo Java, non dell'hardware. Valgono per questa istanza del backend e ripartono da zero a ogni riavvio; il grafico
        tiene solo gli ultimi {{ MAX_CAMPIONI }} campioni, nel browser.
      </p>
    </template>
    <p v-else-if="!errore" class="empty-state">Lettura in corso…</p>
  </section>
</template>
