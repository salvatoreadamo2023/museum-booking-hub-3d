<script setup>
import { computed } from 'vue'
import { Bar } from 'vue-chartjs'
import { BarElement, CategoryScale, Chart, LinearScale, Tooltip } from 'chart.js'
import { formatEuro } from '@/utils/formatters'

Chart.register(CategoryScale, LinearScale, BarElement, Tooltip)

const props = defineProps({
  etichette: { type: Array, required: true },
  valori: { type: Array, required: true }, // euro per giorno
})

const dati = computed(() => ({
  labels: props.etichette,
  datasets: [{ label: 'Incasso', data: props.valori, backgroundColor: '#0f766e', borderRadius: 6, maxBarThickness: 34 }],
}))

const opzioni = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false }, tooltip: { callbacks: { label: (c) => formatEuro(c.parsed.y) } } },
  scales: { x: { grid: { display: false } }, y: { beginAtZero: true, ticks: { callback: (v) => `${v} €` } } },
}
</script>

<template>
  <Bar :data="dati" :options="opzioni" />
</template>
