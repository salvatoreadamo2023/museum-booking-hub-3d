<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
})
</script>

<template>
  <div
    style="
      margin-bottom: 28px;
      border-bottom: 1px solid #e2e8f0;
      padding-bottom: 16px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 16px;
    "
  >
    <div>
      <h1 style="font-family: 'Lora', serif; font-size: 2rem; color: #1e293b">{{ title }}</h1>
      <p v-if="subtitle" style="font-size: 1rem; color: #475569; margin-top: 4px">{{ subtitle }}</p>
    </div>
    <slot />
  </div>
</template>
