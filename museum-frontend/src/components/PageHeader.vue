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
      <h1 style="font-family: 'Lora', serif; font-size: 2.2rem; color: #1e293b">{{ title }}</h1>
      <p v-if="subtitle" style="font-size: 0.88rem; color: #64748b">{{ subtitle }}</p>
    </div>
    <slot />
  </div>
</template>
