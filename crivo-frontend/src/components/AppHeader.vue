<template>
  <header class="row items-center justify-between q-py-md" style="gap: 12px">
    <router-link to="/" class="row items-center marca" style="gap: 10px">
      <q-icon name="fact_check" color="destaque" size="22px" />
      <span class="font-display text-weight-600" style="font-size: 1.15rem; color: var(--cor-texto)">
        CrivoNews
      </span>
    </router-link>

    <nav class="row items-center font-mono" style="gap: 4px">
      <router-link to="/" class="nav-link" exact-active-class="nav-link--ativo">Analisar</router-link>
      <router-link to="/historico" class="nav-link" active-class="nav-link--ativo">Histórico</router-link>

      <q-btn
        flat
        round
        dense
        size="sm"
        class="btn-tema"
        :icon="tema === 'dark' ? 'light_mode' : 'dark_mode'"
        :aria-label="tema === 'dark' ? 'Usar tema claro' : 'Usar tema escuro'"
        @click="alternarTema"
      >
        <q-tooltip>{{ tema === 'dark' ? 'Tema claro' : 'Tema escuro' }}</q-tooltip>
      </q-btn>

      <q-btn
        v-if="usuarioLogado"
        flat
        dense
        no-caps
        class="nav-usuario q-ml-xs"
        :aria-label="`Conta de ${usuarioLogado.nome}`"
      >
        <span class="avatar">{{ inicial }}</span>
        <q-icon name="expand_more" size="16px" style="color: var(--cor-texto-suave)" />
        <q-menu anchor="bottom right" self="top right" class="menu-usuario">
          <div class="q-px-md q-pt-md q-pb-sm">
            <div class="text-weight-medium" style="color: var(--cor-texto)">{{ usuarioLogado.nome }}</div>
            <div class="font-mono text-caption" style="color: var(--cor-texto-suave)">{{ usuarioLogado.email }}</div>
          </div>
          <q-separator />
          <q-list dense>
            <q-item v-close-popup clickable to="/historico">
              <q-item-section avatar><q-icon name="history" size="18px" /></q-item-section>
              <q-item-section>Meu histórico</q-item-section>
            </q-item>
            <q-item v-close-popup clickable @click="sair">
              <q-item-section avatar><q-icon name="logout" size="18px" /></q-item-section>
              <q-item-section>Sair</q-item-section>
            </q-item>
          </q-list>
        </q-menu>
      </q-btn>

      <q-btn
        v-else
        :to="{ name: 'entrar', query: rotaAtual.name === 'entrar' ? {} : { redirect: rotaAtual.fullPath } }"
        unelevated
        dense
        no-caps
        color="primary"
        class="font-mono q-px-sm q-ml-xs btn-entrar"
        label="Entrar"
      />
    </nav>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Notify } from 'quasar'
import { usuarioLogado, encerrarSessao } from '../services/auth.js'
import { tema, alternarTema } from '../services/tema.js'

const rotaAtual = useRoute()
const router = useRouter()

const inicial = computed(() => (usuarioLogado.value?.nome || '?').trim().charAt(0).toUpperCase())

function sair() {
  encerrarSessao()
  Notify.create({ type: 'info', message: 'Você saiu da sua conta.' })
  if (rotaAtual.meta.exigeLogin) {
    router.push('/')
  }
}
</script>

<style scoped>
.marca {
  text-decoration: none;
}

.nav-link {
  font-size: 0.78rem;
  letter-spacing: 0.04em;
  color: var(--cor-texto-suave);
  text-decoration: none;
  padding: 6px 12px;
  border-radius: 4px;
  border: 1px solid transparent;
  transition: color 150ms ease, border-color 150ms ease;
}

.nav-link:hover {
  color: var(--cor-texto);
}

.nav-link:focus-visible {
  outline: 2px solid var(--cor-destaque);
  outline-offset: 2px;
}

.nav-link--ativo {
  color: var(--cor-destaque);
  border-color: var(--cor-borda);
  background: var(--cor-superficie);
}

.btn-tema {
  color: var(--cor-texto-suave);
}

.btn-tema:hover {
  color: var(--cor-texto);
}

.btn-entrar {
  font-size: 0.78rem;
}

.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--cor-destaque-suave);
  border: 1px solid var(--cor-destaque);
  color: var(--cor-destaque);
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 0.9rem;
}

@media (max-width: 420px) {
  .nav-link {
    padding: 6px 8px;
  }
}
</style>

<style>
.menu-usuario {
  background: var(--cor-superficie);
  border: 1px solid var(--cor-borda);
  color: var(--cor-texto);
  min-width: 220px;
}
</style>
