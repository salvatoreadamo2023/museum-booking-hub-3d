<script setup>
const voto = defineModel({ type: Number, default: 5 })
</script>

<template>
  <div class="star-rating">
    <span v-for="n in 5" :key="n" :class="{ active: n <= voto }" @click="voto = n">★</span>
  </div>
</template>
