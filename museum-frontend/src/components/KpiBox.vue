<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: String, required: true },
  sub: { type: String, default: '' },
  icona: { type: String, default: '' }, // emoji nel cerchietto a sinistra
  variant: { type: String, default: '' }, // '', 'teal', 'cobalt', 'slate'
  subColor: { type: String, default: '' }, // vuoto = verde
})
</script>

<template>
  <div class="kpi-box" :class="variant">
    <div v-if="icona" class="kpi-icona" aria-hidden="true">{{ icona }}</div>
    <div>
      <div class="kpi-lbl">{{ label }}</div>
      <div class="kpi-val">{{ value }}</div>
      <span class="kpi-sub" :style="subColor ? { color: subColor } : {}">{{ sub }}</span>
    </div>
  </div>
</template>
