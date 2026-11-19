<script setup>
import PageHeader from '@/components/PageHeader.vue'
import { isAdmin, session } from '@/store/session'
</script>

<template>
  <PageHeader :title="`Ciao, ${session.user?.nome}`" :subtitle="isAdmin() ? 'Area amministrazione' : 'Area visitatore'" />
  <p style="color: #64748b">Accesso riuscito. Le pagine di questa area arrivano nelle prossime fasi.</p>
</template>
