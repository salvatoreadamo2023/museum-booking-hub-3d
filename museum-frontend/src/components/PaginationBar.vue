<script setup>
import { computed } from 'vue'

// Barra di paginazione: mostra l'intervallo visualizzato e i comandi per scorrere l'elenco.
// La pagina 0 è la prima (come nelle API).
const props = defineProps({
  pagina: { type: Number, required: true },
  dimensione: { type: Number, required: true },
  totaleElementi: { type: Number, required: true },
  totalePagine: { type: Number, required: true },
  opzioni: { type: Array, default: () => [10, 20, 50] },
  nome: { type: String, default: 'Elementi' },
  disabilitata: { type: Boolean, default: false }, // durante un caricamento, per non accavallare i clic
})
const emit = defineEmits(['pagina', 'dimensione'])

const da = computed(() => (props.totaleElementi === 0 ? 0 : props.pagina * props.dimensione + 1))
const a = computed(() => Math.min((props.pagina + 1) * props.dimensione, props.totaleElementi))
const primaPagina = computed(() => props.disabilitata || props.pagina <= 0)
const ultimaPagina = computed(() => props.disabilitata || props.pagina >= props.totalePagine - 1)
</script>

<template>
  <div class="pager">
    <span class="pager-info">{{ nome }} {{ da }}–{{ a }} di {{ totaleElementi }}</span>
    <div class="pager-controls">
      <label class="pager-size">
        Righe per pagina
        <select class="select-inline" :value="dimensione" :disabled="disabilitata" @change="emit('dimensione', Number($event.target.value))">
          <option v-for="n in opzioni" :key="n" :value="n">{{ n }}</option>
        </select>
      </label>
      <button class="btn-outline" :disabled="primaPagina" aria-label="Prima pagina" @click="emit('pagina', 0)">«</button>
      <button class="btn-outline" :disabled="primaPagina" @click="emit('pagina', pagina - 1)">‹ Precedente</button>
      <span class="pager-pos">Pagina {{ totalePagine === 0 ? 0 : pagina + 1 }} di {{ totalePagine }}</span>
      <button class="btn-outline" :disabled="ultimaPagina" @click="emit('pagina', pagina + 1)">Successiva ›</button>
      <button class="btn-outline" :disabled="ultimaPagina" aria-label="Ultima pagina" @click="emit('pagina', totalePagine - 1)">»</button>
    </div>
  </div>
</template>
