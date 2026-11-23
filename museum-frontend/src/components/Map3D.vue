<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { exhibitsData } from '@/data/exhibits'
import { tariffaPredefinita } from '@/store/booking'
import { formatData, formatEuro } from '@/utils/formatters'

const props = defineProps({
  tour: { type: Object, required: true }, // itinerario (capienza, tariffe)
  data: { type: String, required: true },
  orario: { type: String, required: true },
  occupati: { type: Array, default: () => [] }, // numeri dei posti già prenotati
  selezionati: { type: Array, default: () => [] }, // [{ numero, tariffaId }]
  inviando: { type: Boolean, default: false },
})
const emit = defineEmits(['toggle', 'tariffa', 'conferma'])

const palco = ref(null)
const stato = ref('caricamento') // caricamento | pronta | errore
const reperto = ref(null)
const suggerimento = ref(null) // { n, stato, x, y }
const avviso = ref('')
const vistaPosto = ref(false)
let sala = null
let smontato = false
let timerAvviso = null

const numeriSelezionati = () => props.selezionati.map((p) => p.numero)
const prezzoDi = (posto) => props.tour.tariffe.find((t) => t.id === posto.tariffaId)?.prezzo ?? 0
const totale = computed(() => props.selezionati.reduce((somma, p) => somma + prezzoDi(p), 0))
const liberi = computed(() => props.tour.capienza - props.occupati.length)
const prezzoBase = computed(() => tariffaPredefinita(props.tour)?.prezzo ?? 0)

function mostraAvviso(testo) {
  avviso.value = testo
  clearTimeout(timerAvviso)
  timerAvviso = setTimeout(() => (avviso.value = ''), 2500)
}

onMounted(async () => {
  try {
    // three.js e la sala si scaricano solo quando serve davvero la mappa
    const { creaSala } = await import('@/sala3d/salaImmersiva')
    const creata = await creaSala(palco.value, {
      tour: props.tour,
      reperti: exhibitsData,
      occupati: props.occupati,
      selezionati: numeriSelezionati(),
      onPosto: (n) => emit('toggle', n),
      onReperto: (ex) => (reperto.value = ex),
      onPassaggio: (info) => (suggerimento.value = info),
      onAvviso: mostraAvviso,
    })
    if (smontato) return creata.distruggi() // si è cambiata pagina mentre la sala si caricava
    sala = creata
    sala.aggiorna(props.occupati, numeriSelezionati(), false) // nel frattempo i posti potrebbero essere cambiati
    stato.value = 'pronta'
  } catch (e) {
    console.error(e)
    if (!smontato) stato.value = 'errore'
  }
})

onBeforeUnmount(() => {
  smontato = true
  clearTimeout(timerAvviso)
  sala?.distruggi()
  sala = null
})

// Posti occupati o selezione cambiati (clic, conflitto 409): la sala si aggiorna da sola
watch(
  () => [props.occupati.join(','), numeriSelezionati().join(',')],
  () => {
    sala?.aggiorna(props.occupati, numeriSelezionati())
    if (sala && !sala.inVistaPosto) vistaPosto.value = false
  },
)

function alternaVista() {
  if (!sala) return
  if (vistaPosto.value) {
    sala.tornaAllaSala()
    vistaPosto.value = false
  } else if (sala.vistaDalPosto(Math.min(...numeriSelezionati()))) {
    reperto.value = null
    vistaPosto.value = true
  }
}

function chiudiReperto() {
  reperto.value = null
  sala?.tornaAllaSala()
}

function testoSuggerimento(st) {
  if (st === 'occ') return 'già prenotato'
  if (st === 'sel') return 'il tuo posto, clic per liberarlo'
  return `libero · da ${formatEuro(prezzoBase.value)}`
}
</script>

<template>
  <div ref="palco" class="palco">
    <div v-if="stato !== 'pronta'" class="caricamento">
      <template v-if="stato === 'caricamento'">
        <div class="spinner"></div>
        Preparo la sala…
      </template>
      <template v-else>Non riesco ad aprire la sala 3D: controlla che il browser abbia WebGL attivo e ricarica la pagina.</template>
    </div>

    <template v-if="stato === 'pronta'">
      <div v-if="reperto" class="vetro scheda">
        <div class="etichetta oro">Reperto in teca</div>
        <h4>{{ reperto.nome }}</h4>
        <p>{{ reperto.desc }}</p>
        <button class="pulsante" @click="chiudiReperto">Torna ai posti</button>
      </div>

      <div v-if="suggerimento" class="suggerimento" :style="{ left: `${suggerimento.x}px`, top: `${suggerimento.y}px` }">
        <b>Posto {{ suggerimento.n }}</b> · {{ testoSuggerimento(suggerimento.stato) }}
      </div>

      <transition name="dissolvi">
        <div v-if="avviso" class="avviso">{{ avviso }}</div>
      </transition>

      <div class="vetro barra">
        <div class="gruppo">
          <span class="etichetta">{{ tour.titolo }} · {{ formatData(data) }} · {{ orario }}</span>
          <span class="etichetta chiara">{{ liberi }} liberi su {{ tour.capienza }}</span>
        </div>
        <div class="legenda">
          <span><i class="libero"></i>libero</span>
          <span><i class="scelto"></i>il tuo posto</span>
          <span><i class="occupato"></i>occupato</span>
        </div>
        <div v-if="selezionati.length" class="carrello">
          <div v-for="posto in selezionati" :key="posto.numero" class="riga-posto">
            <span>Posto {{ posto.numero }}</span>
            <select
              :value="posto.tariffaId"
              :aria-label="`Tariffa del posto ${posto.numero}`"
              @change="emit('tariffa', { numero: posto.numero, tariffaId: Number($event.target.value) })"
            >
              <option v-for="t in tour.tariffe" :key="t.id" :value="t.id">{{ t.nome }} · {{ formatEuro(t.prezzo) }}</option>
            </select>
          </div>
        </div>
        <div v-else class="gruppo vuoto">Clicca una persona trasparente per scegliere il posto</div>
        <div class="gruppo">
          <b v-if="selezionati.length" class="totale">{{ formatEuro(totale) }}</b>
          <button class="pulsante" :disabled="!selezionati.length && !vistaPosto" @click="alternaVista">
            {{ vistaPosto ? 'Torna alla sala' : 'Vista dal posto' }}
          </button>
          <button class="pulsante primario" :disabled="!selezionati.length || inviando" @click="emit('conferma')">Conferma e prenota</button>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.palco {
  position: relative;
  width: 100%;
  height: calc(100vh - 150px);
  min-height: 520px;
  border-radius: 14px;
  overflow: hidden;
  background: #02040a;
  border: 1px solid #1d2742;
  color: #e8ecf6;
  font-family: 'Inter', sans-serif;
  font-size: 14px;
}
.caricamento {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 24px;
  text-align: center;
  color: #8d97b3;
  font-size: 0.9rem;
}
.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #1d2742;
  border-top-color: #4c8dff;
  border-radius: 50%;
  animation: gira 0.8s linear infinite;
}
@keyframes gira {
  to { transform: rotate(360deg); }
}
.vetro {
  background: rgba(6, 10, 22, 0.85);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(120, 160, 255, 0.16);
  border-radius: 14px;
  padding: 12px 14px;
}
.etichetta { font-size: 14px; color: #c3cbe0; }
.etichetta.chiara { color: #ffffff; font-weight: 600; }
.etichetta.oro { color: #f0c46e; font-size: 12px; font-weight: 600; letter-spacing: 0.04em; text-transform: uppercase; }
.scheda {
  position: absolute;
  top: 14px;
  right: 14px;
  width: min(280px, calc(100% - 28px));
}
.scheda h4 { font-family: 'Lora', serif; font-size: 20px; margin: 4px 0 6px; }
.scheda p { margin: 0 0 12px; color: #dde3f0; font-size: 14px; line-height: 1.5; }
.barra {
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  align-items: center;
  justify-content: space-between;
}
.gruppo { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; min-width: 0; }
.vuoto { color: #dde3f0; }
.legenda { display: flex; gap: 14px; flex-wrap: wrap; font-size: 13px; color: #dde3f0; }
.legenda i { display: inline-block; width: 11px; height: 11px; border-radius: 50%; margin-right: 6px; vertical-align: -1px; }
.legenda .libero { background: #9fc2ff; }
.legenda .scelto { background: #2fe39a; }
.legenda .occupato { background: #ff5a6e; }
.carrello { display: flex; flex-wrap: wrap; gap: 6px 14px; max-height: 76px; overflow-y: auto; }
.riga-posto { display: flex; align-items: center; gap: 8px; }
.riga-posto select {
  background: #0c1222;
  color: #e8ecf6;
  border: 1px solid #33405f;
  border-radius: 6px;
  padding: 5px 8px;
  font-family: 'Inter', sans-serif;
  font-size: 13px;
}
.totale { font-family: 'Lora', serif; font-size: 19px; font-variant-numeric: tabular-nums; }
.pulsante {
  cursor: pointer;
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 600;
  padding: 10px 18px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #e8ecf6;
}
.pulsante:hover { background: rgba(255, 255, 255, 0.14); }
.pulsante.primario { background: #2fe39a; border-color: #2fe39a; color: #03150d; font-weight: 700; }
.pulsante:disabled { opacity: 0.5; cursor: default; }
.pulsante:focus-visible,
.riga-posto select:focus-visible { outline: 2px solid #4c8dff; outline-offset: 2px; }
.suggerimento {
  position: absolute;
  pointer-events: none;
  font-size: 13px;
  background: rgba(6, 10, 22, 0.92);
  border: 1px solid rgba(120, 160, 255, 0.3);
  padding: 7px 10px;
  border-radius: 8px;
  white-space: nowrap;
  transform: translate(16px, -50%);
}
.avviso {
  position: absolute;
  left: 50%;
  top: 18px;
  transform: translateX(-50%);
  font-size: 14px;
  padding: 9px 16px;
  border-radius: 999px;
  background: rgba(6, 10, 22, 0.86);
  border: 1px solid rgba(120, 160, 255, 0.3);
  pointer-events: none;
  white-space: nowrap;
}
.dissolvi-enter-active,
.dissolvi-leave-active { transition: opacity 0.4s; }
.dissolvi-enter-from,
.dissolvi-leave-to { opacity: 0; }
@media (max-width: 860px) {
  .palco { height: 78vh; }
  .legenda { display: none; }
}
</style>
