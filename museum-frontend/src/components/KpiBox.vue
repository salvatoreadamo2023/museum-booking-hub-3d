<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: String, required: true },
  sub: { type: String, default: '' },
  variant: { type: String, default: '' }, // '', 'teal', 'cobalt', 'slate'
  subColor: { type: String, default: '' }, // vuoto = verde
})
</script>

<template>
  <div class="kpi-box" :class="variant">
    <div class="kpi-lbl">{{ label }}</div>
    <div class="kpi-val">{{ value }}</div>
    <span class="kpi-sub" :style="subColor ? { color: subColor } : {}">{{ sub }}</span>
  </div>
</template>
