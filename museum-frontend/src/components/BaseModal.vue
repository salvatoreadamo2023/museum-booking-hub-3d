<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

// Finestra modale con lo stile del prototipo. Il genitore la monta con v-if e la chiude all'evento "close".
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  accent: { type: String, default: '#0f766e' },
  titleSize: { type: String, default: '1.5rem' },
})
const emit = defineEmits(['close'])

const attiva = ref(false)
const scheda = ref(null)

// Esc chiude la finestra, come ci si aspetta da qualunque finestra di dialogo
const conTastiera = (e) => e.key === 'Escape' && emit('close')

onMounted(() => {
  requestAnimationFrame(() => (attiva.value = true))
  document.addEventListener('keydown', conTastiera)
  // il cursore va da solo sul primo campo, così si può scrivere subito
  scheda.value?.querySelector('input, select, textarea')?.focus()
})
onBeforeUnmount(() => document.removeEventListener('keydown', conTastiera))
</script>

<template>
  <div class="modal-overlay" :class="{ active: attiva }" @mousedown.self="emit('close')">
    <div ref="scheda" class="modal-card" role="dialog" aria-modal="true" :aria-label="title" :style="{ borderTopColor: accent }">
      <button type="button" class="modal-close" @click="emit('close')">&times;</button>
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
