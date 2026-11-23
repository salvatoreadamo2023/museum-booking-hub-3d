<script setup>
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import UserCreateModal from '@/components/UserCreateModal.vue'
import UserEditModal from '@/components/UserEditModal.vue'
import { userService } from '@/services/userService'
import { usePaginazione } from '@/composables/usePaginazione'
import { session } from '@/store/session'
import { formatData } from '@/utils/formatters'

const { elementi: utenti, pagina, dimensione, totaleElementi, totalePagine, caricamento, errore, carica, vaiAPagina, cambiaDimensione } =
  usePaginazione(userService.elenca, { dimensione: 20, erroreBase: 'Impossibile caricare gli utenti' })

const creazione = ref(false)
const inModifica = ref(null)

onMounted(carica)

const stesso = (u) => u.id === session.user?.id

async function salvato(aggiornato) {
  // se l'admin ha cambiato nome o email al proprio account, la barra laterale deve mostrarli subito
  if (aggiornato && stesso(aggiornato)) {
    session.user = { ...session.user, nome: aggiornato.nome, email: aggiornato.email }
  }
  creazione.value = false
  inModifica.value = null
  await carica()
}
</script>

<template>
  <section class="page-section active">
    <PageHeader title="Utenti" subtitle="Account, ruoli e stato degli utenti">
      <button class="btn-action" @click="creazione = true">+ Nuovo utente</button>
    </PageHeader>
    <p v-if="errore" class="form-error">{{ errore }}</p>
    <div class="card">
      <div class="tabella-scorrevole">
        <table>
          <thead><tr><th>Nome</th><th>Email</th><th>Ruolo</th><th>Stato</th><th>Registrato il</th><th></th></tr></thead>
          <tbody>
            <tr v-for="u in utenti" :key="u.id">
              <td><strong>{{ u.nome }}</strong> <span v-if="stesso(u)" class="testo-secondario">(tu)</span></td>
              <td>{{ u.email }}</td>
              <td>
                <span class="badge" :class="u.ruolo === 'ADMIN' ? 'badge-admin' : 'badge-visitor'">{{ u.ruolo === 'ADMIN' ? 'Admin' : 'Visitatore' }}</span>
              </td>
              <td>
                <span class="badge" :class="u.stato === 'ATTIVO' ? 'badge-ok' : 'badge-ko'">{{ u.stato === 'ATTIVO' ? 'Attivo' : 'Sospeso' }}</span>
              </td>
              <td>{{ formatData((u.creatoIl || '').slice(0, 10)) }}</td>
              <td><button class="btn-outline" @click="inModifica = u">Modifica</button></td>
            </tr>
            <tr v-if="!caricamento && utenti.length === 0"><td colspan="6" class="empty-state">Nessun utente.</td></tr>
          </tbody>
        </table>
      </div>
      <PaginationBar
        v-if="totaleElementi > 0"
        nome="Utenti"
        :pagina="pagina"
        :dimensione="dimensione"
        :totale-elementi="totaleElementi"
        :totale-pagine="totalePagine"
        :disabilitata="caricamento"
        @pagina="vaiAPagina"
        @dimensione="cambiaDimensione"
      />
    </div>
    <UserCreateModal v-if="creazione" @close="creazione = false" @salvato="salvato" />
    <UserEditModal v-if="inModifica" :utente="inModifica" :stesso="stesso(inModifica)" @close="inModifica = null" @salvato="salvato" />
  </section>
</template>
