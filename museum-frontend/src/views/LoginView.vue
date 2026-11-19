<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import IntroCristalli from '@/components/IntroCristalli.vue'
import RegisterModal from '@/components/RegisterModal.vue'
import { authService } from '@/services/authService'
import { messaggioErrore } from '@/services/api'
import { homePath, iniziaSessione } from '@/store/session'

const router = useRouter()

const email = ref('')
const password = ref('')
const errore = ref('')
const inCorso = ref(false)
const mostraRegistrazione = ref(false)
const campoEmail = ref(null)

// L'intro si vede una volta per scheda: dopo un logout si torna direttamente al form
const CHIAVE_INTRO = 'mbh_intro_vista'
const introVista = () => {
  try { return sessionStorage.getItem(CHIAVE_INTRO) === '1' } catch { return false }
}
const introAperta = ref(introVista())

function entra() {
  if (introAperta.value) return
  introAperta.value = true
  try { sessionStorage.setItem(CHIAVE_INTRO, '1') } catch { /* storage non disponibile, pazienza */ }
  setTimeout(() => campoEmail.value?.focus({ preventScroll: true }), 900)
}

function rivediIntro(e) {
  e.currentTarget.blur() // altrimenti un Invio successivo ripremerebbe questo bottone
  introAperta.value = false
}

function invioDaTastiera(e) {
  if (!introAperta.value && (e.key === 'Enter' || e.key === 'ArrowDown')) entra()
}
onMounted(() => addEventListener('keydown', invioDaTastiera))
onBeforeUnmount(() => removeEventListener('keydown', invioDaTastiera))

async function accedi() {
  errore.value = ''
  inCorso.value = true
  try {
    const auth = await authService.login(email.value, password.value)
    iniziaSessione(auth)
    router.push(homePath())
  } catch (e) {
    errore.value = messaggioErrore(e, 'Accesso non riuscito')
  } finally {
    inCorso.value = false
  }
}

function registrato() {
  mostraRegistrazione.value = false
  router.push(homePath())
}
</script>

<template>
  <div id="landing-portal" :class="{ aperto: introAperta }">
    <section class="login-side">
      <button class="rivedi-intro" type="button" @click="rivediIntro">← Rivedi l'ingresso</button>
      <div class="login-box">
        <div class="login-brand">Galleria Geologica</div>
        <h2 class="login-title">Accedi al Museo</h2>
        <p class="login-subtitle">Inserisci le tue credenziali per entrare nell'area personale.</p>
        <form @submit.prevent="accedi">
          <p v-if="errore" class="form-error">{{ errore }}</p>
          <div class="field"><label for="login-email">Indirizzo Email</label><input id="login-email" ref="campoEmail" v-model="email" type="email" placeholder="mario.rossi@email.it" required /></div>
          <div class="field"><label for="login-password">Password</label><input id="login-password" v-model="password" type="password" placeholder="••••••••" required /></div>
          <button type="submit" class="btn-submit" :disabled="inCorso">ACCEDI ORA</button>
        </form>
        <div style="text-align: center; margin-top: 16px; font-size: 0.82rem; color: #64748b">
          Non hai ancora un account?
          <span style="color: #0f766e; font-weight: 700; cursor: pointer" @click="mostraRegistrazione = true">Registrati qui</span>
        </div>
      </div>
    </section>

    <Transition name="sipario">
      <IntroCristalli v-if="!introAperta" @entra="entra" />
    </Transition>
  </div>

  <RegisterModal v-if="mostraRegistrazione" @close="mostraRegistrazione = false" @registrato="registrato" />
</template>
