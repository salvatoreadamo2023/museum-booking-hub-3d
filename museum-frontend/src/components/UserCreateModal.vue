<script setup>
import { ref } from 'vue'
import BaseModal from './BaseModal.vue'
import { userService } from '@/services/userService'
import { messaggioErrore } from '@/services/api'

const emit = defineEmits(['close', 'salvato'])

const nome = ref('')
const email = ref('')
const password = ref('')
const ruolo = ref('VISITATORE')
const stato = ref('ATTIVO')
const errore = ref('')
const inCorso = ref(false)

async function salva() {
  errore.value = ''
  inCorso.value = true
  try {
    await userService.crea({ nome: nome.value.trim(), email: email.value.trim(), password: password.value, ruolo: ruolo.value, stato: stato.value })
    emit('salvato')
  } catch (e) {
    errore.value = messaggioErrore(e)
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <BaseModal title="Nuovo utente" accent="#4f46e5" @close="emit('close')">
    <form @submit.prevent="salva">
      <p v-if="errore" class="form-error">{{ errore }}</p>
      <div class="field">
        <label for="nuovo-nome">Nome e cognome</label>
        <input id="nuovo-nome" v-model="nome" type="text" required maxlength="100" placeholder="Es. Marco Verdi" />
      </div>
      <div class="field">
        <label for="nuovo-email">Email</label>
        <input id="nuovo-email" v-model="email" type="email" required maxlength="150" autocomplete="off" />
      </div>
      <div class="field">
        <label for="nuovo-password">Password</label>
        <input id="nuovo-password" v-model="password" type="password" required minlength="8" maxlength="72" autocomplete="new-password" />
        <div class="form-hint">Da 8 a 72 caratteri.</div>
      </div>
      <div class="field">
        <label for="nuovo-ruolo">Ruolo</label>
        <select id="nuovo-ruolo" v-model="ruolo">
          <option value="VISITATORE">Visitatore</option>
          <option value="ADMIN">Amministratore</option>
        </select>
      </div>
      <div class="field">
        <label for="nuovo-stato">Stato dell'account</label>
        <select id="nuovo-stato" v-model="stato">
          <option value="ATTIVO">Attivo</option>
          <option value="SOSPESO">Sospeso</option>
        </select>
      </div>
      <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 24px">
        <button type="button" class="btn-outline" @click="emit('close')">Annulla</button>
        <button type="submit" class="btn-action" style="background: #4f46e5" :disabled="inCorso">Crea utente</button>
      </div>
    </form>
  </BaseModal>
</template>
