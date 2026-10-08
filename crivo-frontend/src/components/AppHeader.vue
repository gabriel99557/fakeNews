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
    </nav>
  </header>
</template>

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
</style>
