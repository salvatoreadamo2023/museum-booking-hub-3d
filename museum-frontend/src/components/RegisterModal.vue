<script setup>
import { ref } from 'vue'
import BaseModal from './BaseModal.vue'
import { authService } from '@/services/authService'
import { messaggioErrore } from '@/services/api'
import { iniziaSessione } from '@/store/session'

const emit = defineEmits(['close', 'registrato'])

const nome = ref('')
const email = ref('')
const password = ref('')
const errore = ref('')
const inCorso = ref(false)

async function registra() {
  errore.value = ''
  inCorso.value = true
  try {
    const auth = await authService.register(nome.value, email.value, password.value)
    iniziaSessione(auth)
    emit('registrato')
  } catch (e) {
    errore.value = messaggioErrore(e, 'Registrazione non riuscita')
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <BaseModal title="Crea Profilo" accent="#0f766e" title-size="1.6rem" @close="emit('close')">
    <p style="font-size: 0.85rem; color: #64748b; margin-bottom: 24px; margin-top: -6px">
      Registrati per riservare le tue visite e i posti 3D.
    </p>
    <form @submit.prevent="registra">
      <p v-if="errore" class="form-error">{{ errore }}</p>
      <div class="field"><label>Nome Completo</label><input v-model="nome" type="text" placeholder="Mario Rossi" required maxlength="100" /></div>
      <div class="field"><label>Email</label><input v-model="email" type="email" placeholder="mario.rossi@email.it" required /></div>
      <div class="field">
        <label>Crea Password</label>
        <input v-model="password" type="password" placeholder="••••••••" required minlength="8" maxlength="72" />
        <div class="form-hint">Almeno 8 caratteri</div>
      </div>
      <button type="submit" class="btn-submit" style="background: #0f766e" :disabled="inCorso">COMPLETA REGISTRAZIONE</button>
    </form>
  </BaseModal>
</template>
