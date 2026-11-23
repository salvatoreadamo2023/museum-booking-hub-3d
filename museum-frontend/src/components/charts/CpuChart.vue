<script setup>
import { computed } from 'vue'
import { Line } from 'vue-chartjs'
import { CategoryScale, Chart, LineElement, LinearScale, PointElement, Tooltip } from 'chart.js'
import { ORO } from './opzioniGrafici'

Chart.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip)

const props = defineProps({
  etichette: { type: Array, required: true }, // orari dei campioni
  valori: { type: Array, required: true }, // CPU del backend (%)
})

const dati = computed(() => ({
  labels: props.etichette,
  datasets: [{ label: 'CPU del backend', data: props.valori, borderColor: ORO, backgroundColor: ORO, tension: 0.3, borderWidth: 2, pointRadius: 0 }],
}))

const opzioni = {
  responsive: true,
  maintainAspectRatio: false,
  animation: false, // si aggiorna ogni 5 secondi: niente animazione a ogni campione
  interaction: { mode: 'index', intersect: false },
  plugins: { legend: { display: false } },
  scales: {
    x: { grid: { display: false }, ticks: { maxTicksLimit: 6, maxRotation: 0 } },
    y: { min: 0, max: 100, ticks: { callback: (v) => `${v} %` } },
  },
}
</script>

<template>
  <Line :data="dati" :options="opzioni" />
</template>
