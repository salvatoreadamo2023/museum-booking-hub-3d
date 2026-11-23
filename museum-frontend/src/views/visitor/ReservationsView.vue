<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import TicketCard from '@/components/TicketCard.vue'
import { reservationService } from '@/services/reservationService'
import { messaggioErrore } from '@/services/api'
import { session } from '@/store/session'
import { scaricaRicevuta } from '@/utils/pdfReceipt'

const router = useRouter()
const prenotazioni = ref([])
const errore = ref('')
const caricamento = ref(true)

async function carica() {
  try {
    prenotazioni.value = await reservationService.elenca()
  } catch (e) {
    errore.value = messaggioErrore(e, 'Impossibile caricare le prenotazioni')
  } finally {
    caricamento.value = false
  }
}

onMounted(carica)

async function disdici(prenotazione) {
  if (!confirm('Sei sicuro di voler disdire questa prenotazione?')) return
  try {
    await reservationService.annulla(prenotazione.id)
    await carica()
  } catch (e) {
    alert(messaggioErrore(e))
    await carica()
  }
}
</script>

<template>
  <section class="page-section active">
    <PageHeader title="Le Mie Prenotazioni" subtitle="Visualizza i tuoi biglietti attivi, scarica il PDF di dettaglio o disdici la presenza" />
    <p v-if="errore" class="form-error">{{ errore }}</p>
    <div v-if="!caricamento && !errore && prenotazioni.length === 0" class="card empty-state">
      Non hai ancora prenotazioni. Scegli un itinerario da “Visite &amp; Orari”.
    </div>
    <div>
      <TicketCard
        v-for="p in prenotazioni"
        :key="p.id"
        :prenotazione="p"
        @ricevuta="scaricaRicevuta($event, session.user.nome)"
        @disdici="disdici"
        @feedback="router.push('/feedback')"
      />
    </div>
  </section>
</template>
