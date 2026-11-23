<script setup>
import { ref } from 'vue'
import BaseModal from './BaseModal.vue'
import { userService } from '@/services/userService'
import { messaggioErrore } from '@/services/api'

const props = defineProps({
  utente: { type: Object, required: true },
  stesso: { type: Boolean, default: false }, // l'admin sta modificando il proprio account
})
const emit = defineEmits(['close', 'salvato'])

const nome = ref(props.utente.nome)
const email = ref(props.utente.email)
const password = ref('')
const ruolo = ref(props.utente.ruolo)
const stato = ref(props.utente.stato)
const errore = ref('')
const inCorso = ref(false)
const confermaElimina = ref(false)

async function salva() {
  errore.value = ''
  inCorso.value = true
  try {
    const aggiornato = await userService.aggiorna(props.utente.id, {
      nome: nome.value.trim(),
      email: email.value.trim(),
      password: password.value, // vuota = resta quella attuale
      ruolo: ruolo.value,
      stato: stato.value,
    })
    emit('salvato', aggiornato)
  } catch (e) {
    errore.value = messaggioErrore(e)
  } finally {
    inCorso.value = false
  }
}

async function elimina() {
  if (!confermaElimina.value) {
    confermaElimina.value = true
    return
  }
  errore.value = ''
  inCorso.value = true
  try {
    await userService.elimina(props.utente.id)
    emit('salvato')
  } catch (e) {
    errore.value = messaggioErrore(e)
    confermaElimina.value = false
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <BaseModal title="Gestisci utente" accent="#0f766e" :subtitle="`${utente.nome} · ${utente.email}`" @close="emit('close')">
    <form @submit.prevent="salva">
      <p v-if="errore" class="form-error">{{ errore }}</p>
      <div class="field">
        <label for="utente-nome">Nome e cognome</label>
        <input id="utente-nome" v-model="nome" type="text" required maxlength="100" />
      </div>
      <div class="field">
        <label for="utente-email">Email</label>
        <input id="utente-email" v-model="email" type="email" required maxlength="150" />
      </div>
      <div class="field">
        <label for="utente-password">Nuova password</label>
        <input
          id="utente-password"
          v-model="password"
          type="password"
          minlength="8"
          maxlength="72"
          autocomplete="new-password"
          placeholder="Lascia vuoto per non cambiarla"
        />
        <div v-if="!stesso" class="form-hint">Cambiando la password, l'utente viene disconnesso da tutti i dispositivi.</div>
      </div>
      <div class="field">
        <label for="utente-ruolo">Ruolo</label>
        <select id="utente-ruolo" v-model="ruolo" :disabled="stesso">
          <option value="VISITATORE">Visitatore</option>
          <option value="ADMIN">Amministratore</option>
        </select>
      </div>
      <div class="field">
        <label for="utente-stato">Stato dell'account</label>
        <select id="utente-stato" v-model="stato" :disabled="stesso">
          <option value="ATTIVO">Attivo</option>
          <option value="SOSPESO">Sospeso</option>
        </select>
        <div v-if="stesso" class="form-hint">È il tuo account: non puoi cambiarne ruolo o stato, né eliminarlo.</div>
      </div>
      <div style="display: flex; gap: 10px; margin-top: 24px; flex-wrap: wrap">
        <button type="submit" class="btn-action" style="flex: 1; background: #0f766e" :disabled="inCorso">Salva modifiche</button>
        <button
          v-if="!stesso"
          type="button"
          class="btn-outline"
          style="color: #b91c1c; border-color: #fca5a5; font-weight: 700"
          :disabled="inCorso"
          @click="elimina"
        >
          {{ confermaElimina ? 'Sì, elimina davvero' : 'Elimina utente' }}
        </button>
      </div>
    </form>
  </BaseModal>
</template>
