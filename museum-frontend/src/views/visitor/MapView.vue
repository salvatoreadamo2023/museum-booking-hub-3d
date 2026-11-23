<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Map3D from '@/components/Map3D.vue'
import { reservationService } from '@/services/reservationService'
import { tourService } from '@/services/tourService'
import { messaggioErrore } from '@/services/api'
import { azzera, booking, impostaTariffa, togglePosto } from '@/store/booking'
import { formatData } from '@/utils/formatters'

const router = useRouter()
const occupati = ref([])
const postiPronti = ref(false) // finché non so quali posti sono occupati non faccio selezionare niente
const errore = ref('')
const inCorso = ref(false) // evita due prenotazioni con un doppio clic su Conferma

// Finestra mostrata a prenotazione riuscita
const mostraModalConferma = ref(false)
const dettaglioPrenotazione = ref(null)

const mostraModalErrore = ref(false)
const testoErroreModal = ref('')

// Avviso breve in alto, usato quando qualcuno ci ha preceduto su un posto
const messaggioToast = ref('')

async function caricaPosti() {
  const stato = await tourService.posti(booking.tour.id, booking.data, booking.orario)
  occupati.value = stato.occupati
}

onMounted(async () => {
  if (!booking.tour) {
    router.replace('/visite')
    return
  }
  // la sala 3D si prepara intanto: i posti occupati le arrivano appena il server risponde
  try {
    await caricaPosti()
    postiPronti.value = true
  } catch (e) {
    errore.value = messaggioErrore(e, 'Impossibile caricare i posti')
  }
})

async function ricaricaDopoConflitto() {
  try {
    await caricaPosti()
    booking.posti = booking.posti.filter((p) => !occupati.value.includes(p.numero))
  } catch {
    // se non riesco a ricaricare lascio la selezione com'è
  }
  return booking.posti.length
}

function mostraToast(testo) {
  messaggioToast.value = testo
  setTimeout(() => {
    messaggioToast.value = ''
  }, 4000)
}

async function conferma() {
  if (inCorso.value) return
  inCorso.value = true
  const corpo = {
    tourId: booking.tour.id,
    data: booking.data,
    orario: booking.orario,
    posti: booking.posti.map((p) => ({ numero: p.numero, tariffaId: p.tariffaId })),
  }
  try {
    const prenotazione = await reservationService.crea(corpo)

    dettaglioPrenotazione.value = {
      codice: prenotazione.codice,
      tour: prenotazione.tour || booking.tour.titolo,
      orario: prenotazione.orario || booking.orario,
      data: booking.data,
      numPosti: booking.posti.length,
    }
    mostraModalConferma.value = true
  } catch (e) {
    const msg = messaggioErrore(e)
    // 409 di solito vuol dire che un posto è stato preso nel frattempo: ricarico la mappa e lo tolgo dalla selezione.
    // Se dopo il ricarico nessun posto risulta preso, il conflitto era un altro e mostro il messaggio del server.
    const primaDelRicarico = booking.posti.length
    if (e.response?.status === 409 && (await ricaricaDopoConflitto()) < primaDelRicarico) {
      mostraToast('Qualcuno ha appena prenotato uno dei posti che avevi scelto: la mappa è stata aggiornata.')
    } else {
      testoErroreModal.value = msg
      mostraModalErrore.value = true
    }
  } finally {
    inCorso.value = false
  }
}

function vaiAPrenotazioni() {
  mostraModalConferma.value = false
  azzera()
  router.push('/prenotazioni')
}
</script>

<template>
  <section v-if="booking.tour" class="page-section active map-view-container">
    <div class="map-header">
      <button class="btn-outline" @click="router.push('/visite')">
        ← Torna a Visite &amp; Orari
      </button>
      <div class="tour-badge">
        <span class="pulse-dot"></span>
        Prenotazione: <strong>{{ booking.tour.titolo }}</strong> — {{ formatData(booking.data) }}, Turno {{ booking.orario }}
      </div>
    </div>

    <transition name="fade">
      <div v-if="messaggioToast" class="custom-toast">
        {{ messaggioToast }}
      </div>
    </transition>

    <p v-if="errore" class="form-error">{{ errore }}</p>

    <Map3D
      v-if="!errore"
      :tour="booking.tour"
      :data="booking.data"
      :orario="booking.orario"
      :occupati="occupati"
      :selezionati="booking.posti"
      :inviando="inCorso"
      @toggle="(n) => postiPronti && togglePosto(n)"
      @tariffa="({ numero, tariffaId }) => impostaTariffa(numero, tariffaId)"
      @conferma="conferma"
    />
  </section>

  <transition name="modal-fade">
    <div v-if="mostraModalConferma" class="modal-backdrop">
      <div class="modal-card">
        <div class="modal-icon">🎉</div>
        <h2>Prenotazione Confermata!</h2>
        <p class="modal-subtitle">La tua visita al museo è stata registrata con successo.</p>

        <div class="modal-details" v-if="dettaglioPrenotazione">
          <div class="detail-row">
            <span class="label">Codice Prenotazione:</span>
            <span class="value code-highlight">{{ dettaglioPrenotazione.codice }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Itinerario:</span>
            <span class="value">{{ dettaglioPrenotazione.tour }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Data &amp; Orario:</span>
            <span class="value">{{ formatData(dettaglioPrenotazione.data) }} alle {{ dettaglioPrenotazione.orario }}</span>
          </div>
          <div class="detail-row">
            <span class="label">Posti Riservati:</span>
            <span class="value">{{ dettaglioPrenotazione.numPosti }}</span>
          </div>
        </div>

        <button class="btn-primary-modal" @click="vaiAPrenotazioni">
          Vai alle Mie Prenotazioni
        </button>
      </div>
    </div>
  </transition>

  <transition name="modal-fade">
    <div v-if="mostraModalErrore" class="modal-backdrop">
      <div class="modal-card">
        <div class="modal-icon">⚠️</div>
        <h2>Impossibile completare</h2>
        <p class="modal-subtitle error-text">{{ testoErroreModal }}</p>

        <button class="btn-primary-modal btn-error" @click="mostraModalErrore = false">
          Chiudi
        </button>
      </div>
    </div>
  </transition>
</template>

<style scoped>
.map-view-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
}

.map-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 10px;
}

.map-header .btn-outline {
  font-size: 0.9rem;
  padding: 10px 16px;
}

.tour-badge {
  font-weight: 600;
  color: #b45309;
  background: #fffbeb;
  padding: 8px 14px;
  border-radius: 8px;
  border: 1px solid #fde68a;
  display: flex;
  align-items: center;
  gap: 8px;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  background-color: #d97706;
  border-radius: 50%;
  box-shadow: 0 0 0 0 rgba(217, 119, 6, 0.4);
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(217, 119, 6, 0.6);
  }
  70% {
    transform: scale(1);
    box-shadow: 0 0 0 8px rgba(217, 119, 6, 0);
  }
  100% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(217, 119, 6, 0);
  }
}

.custom-toast {
  position: absolute;
  top: 70px;
  left: 50%;
  transform: translateX(-50%);
  background: #1e293b;
  color: #fff;
  padding: 12px 22px;
  border-radius: 8px;
  z-index: 100;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.25);
  font-size: 14px;
  white-space: nowrap;
}

/* Finestre di esito */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.65);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  padding: 20px;
}

.modal-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 32px;
  max-width: 440px;
  width: 100%;
  text-align: center;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  border: 1px solid #f1f5f9;
}

.modal-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.modal-card h2 {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 6px;
}

.modal-subtitle {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 24px;
}

.error-text {
  color: #dc2626;
  font-weight: 500;
}

.modal-details {
  background: #f8fafc;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  text-align: left;
  border: 1px solid #e2e8f0;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.detail-row .label {
  color: #64748b;
}

.detail-row .value {
  font-weight: 600;
  color: #1e293b;
}

.code-highlight {
  color: #b45309 !important;
  font-family: monospace;
  font-size: 15px;
  background: #fef3c7;
  padding: 2px 8px;
  border-radius: 4px;
}

.btn-primary-modal {
  width: 100%;
  background: #b45309;
  color: white;
  border: none;
  padding: 12px 20px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 15px;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.1s ease;
}

.btn-primary-modal:hover {
  background: #92400e;
}

.btn-error {
  background: #475569;
}

.btn-error:hover {
  background: #334155;
}

.btn-primary-modal:active {
  transform: scale(0.98);
}

.fade-enter-active, .fade-leave-active,
.modal-fade-enter-active, .modal-fade-leave-active {
  transition: all 0.3s ease;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-from, .modal-fade-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
</style>