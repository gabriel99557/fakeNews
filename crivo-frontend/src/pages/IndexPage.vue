<template>
  <q-page class="bg-crivo">
    <div class="page-shell">

      <app-header />

      <hr class="divider-line q-mb-xl" />

      <!-- Hero / console de análise -->
      <section class="q-mb-xl">
        <div class="eyebrow q-mb-sm">Antes de compartilhar, verifique</div>
        <h1 class="font-display q-mt-none q-mb-sm hero-title">
          Passe a notícia<br />pelo crivo.
        </h1>
        <p class="text-body1 q-mb-xl" style="max-width: 52ch; color: var(--color-paper-dim)">
          Cole um texto ou o link de uma matéria. Um modelo de linguagem ajustado para
          detecção de desinformação analisa o conteúdo e devolve um veredito com a
          justificativa por trás dele.
        </p>

        <div class="console-panel q-pa-lg">
          <analise-form :carregando="carregando" @analisar="executarAnalise" />
        </div>
      </section>

      <!-- Resultado -->
      <section v-if="erro" class="q-mb-xl">
        <q-banner dense class="bg-negative text-white" rounded>
          <template #avatar><q-icon name="error_outline" /></template>
          {{ erro }}
        </q-banner>
      </section>

      <section v-if="resultado" class="q-mb-xl">
        <div class="eyebrow q-mb-sm">Resultado</div>
        <resultado-analise :resultado="resultado" />
      </section>

      <!-- Como funciona -->
      <section class="q-mb-xl">
        <hr class="divider-line q-mb-lg" />
        <div class="eyebrow q-mb-md">Como funciona</div>
        <div class="row q-col-gutter-lg">
          <div class="col-12 col-sm-4" v-for="passo in passos" :key="passo.numero">
            <div class="font-mono" style="color: var(--color-signal); font-size: 0.8rem">
              {{ passo.numero }}
            </div>
            <div class="font-display text-weight-600 q-mt-xs q-mb-xs" style="color: var(--color-paper)">
              {{ passo.titulo }}
            </div>
            <div class="text-body2" style="color: var(--color-paper-dim)">
              {{ passo.descricao }}
            </div>
          </div>
        </div>
      </section>

      <footer class="q-py-lg font-mono text-caption" style="color: var(--color-paper-dim)">
        <hr class="divider-line q-mb-md" />
        Projeto acadêmico — Universidade Franciscana (UFN). Vereditos gerados por IA são
        probabilísticos e não substituem a checagem por especialistas.
      </footer>
    </div>
  </q-page>
</template>

<script setup>
import { ref } from 'vue'
import { Notify } from 'quasar'
import AppHeader from '../components/AppHeader.vue'
import AnaliseForm from '../components/AnaliseForm.vue'
import ResultadoAnalise from '../components/ResultadoAnalise.vue'
import { analisarConteudo, extrairMensagemErro } from '../services/api.js'

const carregando = ref(false)
const resultado = ref(null)
const erro = ref('')

const passos = [
  { numero: '01', titulo: 'Cole o conteúdo', descricao: 'Texto bruto ou o link de uma notícia publicada.' },
  { numero: '02', titulo: 'A IA analisa', descricao: 'Padrões linguísticos, sensacionalismo e estrutura do texto.' },
  { numero: '03', titulo: 'Receba o veredito', descricao: 'Probabilidade de desinformação, com justificativa.' }
]

async function executarAnalise({ conteudo, modelo }) {
  carregando.value = true
  erro.value = ''
  resultado.value = null

  try {
    const { data } = await analisarConteudo(conteudo, modelo)
    resultado.value = data
  } catch (e) {
    erro.value = extrairMensagemErro(e)
    Notify.create({ type: 'negative', message: erro.value })
  } finally {
    carregando.value = false
  }
}
</script>

<style scoped>
.console-panel {
  background: var(--color-ink-soft);
  border: 1px solid var(--color-ink-line);
  border-radius: 4px;
}
</style>
