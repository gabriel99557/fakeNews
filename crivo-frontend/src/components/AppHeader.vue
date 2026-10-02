<template>
  <header class="row items-center justify-between q-py-md" style="gap: 12px">
    <router-link to="/" class="row items-center marca" style="gap: 10px">
      <q-icon name="fact_check" color="orange-6" size="22px" />
      <span class="font-display text-weight-600" style="font-size: 1.15rem; color: var(--color-paper)">
        CrivoNews
      </span>
    </router-link>

    <nav class="row items-center font-mono" style="gap: 4px">
      <router-link to="/" class="nav-link" exact-active-class="nav-link--ativo">Analisar</router-link>
      <router-link to="/historico" class="nav-link" active-class="nav-link--ativo">Histórico</router-link>

      <q-btn
        v-if="usuarioLogado"
        flat
        dense
        no-caps
        class="nav-usuario q-ml-xs"
        :aria-label="`Conta de ${usuarioLogado.nome}`"
      >
        <span class="avatar">{{ inicial }}</span>
        <q-icon name="expand_more" size="16px" style="color: var(--color-paper-dim)" />
        <q-menu anchor="bottom right" self="top right" class="menu-usuario">
          <div class="q-px-md q-pt-md q-pb-sm">
            <div class="text-weight-medium" style="color: var(--color-paper)">{{ usuarioLogado.nome }}</div>
            <div class="font-mono text-caption" style="color: var(--color-paper-dim)">{{ usuarioLogado.email }}</div>
          </div>
          <q-separator dark />
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
        color="orange-6"
        text-color="dark"
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
  color: var(--color-paper-dim);
  text-decoration: none;
  padding: 6px 12px;
  border-radius: 4px;
  border: 1px solid transparent;
  transition: color 150ms ease, border-color 150ms ease;
}

.nav-link:hover {
  color: var(--color-paper);
}

.nav-link:focus-visible {
  outline: 2px solid var(--color-signal);
  outline-offset: 2px;
}

.nav-link--ativo {
  color: var(--color-signal);
  border-color: var(--color-ink-line);
  background: var(--color-ink-soft);
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
  background: var(--color-signal-soft);
  border: 1px solid var(--color-signal);
  color: var(--color-signal);
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
  background: var(--color-ink-soft);
  border: 1px solid var(--color-ink-line);
  color: var(--color-paper);
  min-width: 220px;
}
</style>
