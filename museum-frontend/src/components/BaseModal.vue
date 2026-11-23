<script setup>
import { onMounted, ref } from 'vue'

// Finestra modale con lo stile del prototipo. Il genitore la monta con v-if e la chiude all'evento "close".
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  accent: { type: String, default: '#0f766e' },
  titleSize: { type: String, default: '1.5rem' },
})
defineEmits(['close'])

const attiva = ref(false)
onMounted(() => requestAnimationFrame(() => (attiva.value = true)))
</script>

<template>
  <div class="modal-overlay" :class="{ active: attiva }">
    <div class="modal-card" :style="{ borderTopColor: accent }">
      <button type="button" class="modal-close" @click="$emit('close')">&times;</button>
      <h2
        :style="{
          fontFamily: `'Lora', serif`,
          fontSize: titleSize,
          color: accent,
          marginBottom: subtitle ? '4px' : '12px',
        }"
      >
        {{ title }}
      </h2>
      <p v-if="subtitle" style="font-size: 0.95rem; color: #475569; margin-bottom: 20px">{{ subtitle }}</p>
      <slot />
    </div>
  </div>
</template>
