<script setup>
import { computed } from 'vue'
import { Line } from 'vue-chartjs'
import { CategoryScale, Chart, Filler, LineElement, LinearScale, PointElement, Tooltip } from 'chart.js'
import { ORO } from './opzioniGrafici'

Chart.register(CategoryScale, LinearScale, PointElement, LineElement, Filler, Tooltip)

const props = defineProps({
  etichette: { type: Array, required: true },
  valori: { type: Array, required: true },
})

const dati = computed(() => ({
  labels: props.etichette,
  datasets: [
    {
      label: 'Biglietti',
      data: props.valori,
      borderColor: ORO,
      backgroundColor: 'rgba(180, 83, 9, 0.06)',
      fill: true,
      tension: 0.3,
      borderWidth: 2,
      pointRadius: 3,
    },
  ],
}))

const opzioni = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  scales: { x: { grid: { display: false } }, y: { beginAtZero: true, ticks: { precision: 0 } } },
}
</script>

<template>
  <Line :data="dati" :options="opzioni" />
</template>
