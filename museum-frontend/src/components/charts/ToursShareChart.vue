<script setup>
import { computed } from 'vue'
import { Doughnut } from 'vue-chartjs'
import { ArcElement, Chart, Legend, Tooltip } from 'chart.js'
import { COLORI } from './opzioniGrafici'

Chart.register(ArcElement, Tooltip, Legend)

const props = defineProps({
  etichette: { type: Array, required: true },
  valori: { type: Array, required: true },
})

const dati = computed(() => ({
  labels: props.etichette,
  datasets: [
    {
      data: props.valori,
      backgroundColor: props.etichette.map((_, i) => COLORI[i % COLORI.length]),
      borderWidth: 1,
    },
  ],
}))

const opzioni = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { position: 'right', labels: { boxWidth: 12, font: { size: 14 } } } },
  cutout: '65%',
}
</script>

<template>
  <Doughnut :data="dati" :options="opzioni" />
</template>
