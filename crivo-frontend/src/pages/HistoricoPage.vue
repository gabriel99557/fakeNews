<template>
  <q-page class="bg-crivo">
    <div class="page-shell">
      <app-header />

      <hr class="divider-line q-mb-xl" />

      <section class="q-mb-lg">
        <div class="eyebrow q-mb-sm">Histórico de consultas</div>
        <h1 class="font-display q-mt-none q-mb-sm hero-title">Análises anteriores.</h1>
        <p class="text-body1 q-mb-none" style="max-width: 52ch; color: var(--color-paper-dim)">
          Todas as verificações feitas na plataforma, das mais recentes para as mais antigas.
          Selecione uma análise para rever o veredito completo.
        </p>
      </section>

      <!-- Filtros -->
      <section class="row items-end justify-between q-mb-md" style="gap: 12px">
        <div>
          <div class="eyebrow q-mb-xs">Filtrar por veredito</div>
          <q-btn-toggle
            v-model="filtroVeredito"
            no-caps
            unelevated
            toggle-color="orange-6"
            toggle-text-color="dark"
            color="grey-9"
            text-color="grey-4"
            class="font-mono filtro-toggle"
            :options="opcoesFiltro"
          />
        </div>
        <span v-if="pagina" class="font-mono text-caption" style="color: var(--color-paper-dim)">
          {{ pagina.totalItens }} {{ pagina.totalItens === 1 ? 'análise' : 'análises' }}
        </span>
      </section>

      <!-- Erro -->
      <q-banner v-if="erro" dense rounded class="bg-negative text-white q-mb-md">
        <template #avatar><q-icon name="error_outline" /></template>
        {{ erro }}
        <template #action>
          <q-btn flat dense no-caps label="Tentar novamente" @click="carregar" />
        </template>
      </q-banner>

      <!-- Carregando (primeira carga) -->
      <div v-else-if="carregando && !pagina" class="lista">
        <q-skeleton v-for="n in 4" :key="n" type="rect" height="84px" dark class="skeleton" />
      </div>

      <!-- Vazio -->
      <div v-else-if="pagina && pagina.itens.length === 0" class="vazio q-pa-xl text-center">
        <q-icon name="history" size="36px" style="color: var(--color-paper-dim)" />
        <div class="font-display q-mt-sm q-mb-xs" style="font-size: 1.15rem; color: var(--color-paper)">
          {{ filtroVeredito ? 'Nenhuma análise com esse veredito.' : 'Nenhuma análise registrada ainda.' }}
        </div>
        <div class="text-body2 q-mb-md" style="color: var(--color-paper-dim)">
          As verificações aparecem aqui assim que são concluídas.
        </div>
        <q-btn
          v-if="!filtroVeredito"
          to="/"
          unelevated
          no-caps
          color="orange-6"
          text-color="dark"
          class="font-mono"
          label="Fazer uma análise"
        />
      </div>

      <!-- Lista -->
      <div v-else-if="pagina" class="lista" :class="{ 'lista--atualizando': carregando }">
        <div v-for="item in pagina.itens" :key="item.id">
          <button
            type="button"
            class="item"
            :class="[classeVeredito(item.veredito), { 'item--aberto': selecionadoId === item.id }]"
            :aria-expanded="selecionadoId === item.id"
            @click="alternar(item.id)"
          >
            <div class="item-veredito font-mono">
              <span class="item-veredito-nome">{{ item.veredito }}</span>
              <span class="item-veredito-prob">{{ item.probabilidadeFake }}% fake</span>
            </div>

            <div class="item-corpo">
              <div class="item-conteudo">{{ item.conteudoOriginal }}</div>
              <div class="item-meta font-mono">
                <span>{{ formatarData(item.dataAnalise) }}</span>
                <span>·</span>
                <span>{{ item.origem === 'url' ? 'URL' : 'texto' }}</span>
                <span>·</span>
                <span>{{ item.modelo === 'local' ? 'Llama 3 local' : 'nuvem' }}</span>
              </div>
            </div>

            <q-icon
              name="expand_more"
              size="20px"
              class="item-seta"
              :class="{ 'item-seta--aberta': selecionadoId === item.id }"
            />
          </button>

          <div v-if="selecionadoId === item.id" class="q-mt-sm q-mb-md">
            <resultado-analise :resultado="item" />
          </div>
        </div>
      </div>

      <!-- Paginação -->
      <div v-if="pagina && pagina.totalPaginas > 1" class="row justify-center q-mt-lg">
        <q-pagination
          :model-value="paginaAtual + 1"
          :max="pagina.totalPaginas"
          :max-pages="6"
          boundary-numbers
          direction-links
          color="grey-7"
          active-color="orange-6"
          active-text-color="dark"
          class="font-mono"
          @update:model-value="irParaPagina"
        />
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import AppHeader from '../components/AppHeader.vue'
import ResultadoAnalise from '../components/ResultadoAnalise.vue'
import { listarHistorico, extrairMensagemErro } from '../services/api.js'

const TAMANHO_PAGINA = 10

const opcoesFiltro = [
  { label: 'Todos', value: null },
  { label: 'Falso', value: 'FALSO' },
  { label: 'Verdadeiro', value: 'VERDADEIRO' },
  { label: 'Inconclusivo', value: 'INCONCLUSIVO' }
]

const pagina = ref(null)
const paginaAtual = ref(0)
const filtroVeredito = ref(null)
const selecionadoId = ref(null)
const carregando = ref(false)
const erro = ref('')

async function carregar() {
  carregando.value = true
  erro.value = ''
  try {
    const { data } = await listarHistorico({
      pagina: paginaAtual.value,
      tamanho: TAMANHO_PAGINA,
      veredito: filtroVeredito.value
    })
    pagina.value = data
  } catch (e) {
    erro.value = extrairMensagemErro(e)
  } finally {
    carregando.value = false
  }
}

function irParaPagina(numero) {
  paginaAtual.value = numero - 1
  selecionadoId.value = null
  carregar()
}

function alternar(id) {
  selecionadoId.value = selecionadoId.value === id ? null : id
}

function classeVeredito(veredito) {
  switch (veredito) {
    case 'VERDADEIRO': return 'item--true'
    case 'FALSO': return 'item--false'
    default: return 'item--unclear'
  }
}

function formatarData(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleString('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}

watch(filtroVeredito, () => {
  paginaAtual.value = 0
  selecionadoId.value = null
  carregar()
})

onMounted(carregar)
</script>

<style scoped>
.filtro-toggle {
  font-size: 0.74rem;
  flex-wrap: wrap;
}

.lista {
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: opacity 150ms ease;
}

.lista--atualizando {
  opacity: 0.55;
  pointer-events: none;
}

.skeleton {
  border-radius: 4px;
  background: var(--color-ink-soft);
}

.vazio {
  background: var(--color-ink-soft);
  border: 1px dashed var(--color-ink-line);
  border-radius: 4px;
}

.item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 18px;
  text-align: left;
  font: inherit;
  color: inherit;
  cursor: pointer;
  background: var(--color-ink-soft);
  border: 1px solid var(--color-ink-line);
  border-left-width: 4px;
  border-radius: 4px;
  padding: 14px 16px;
  transition: border-color 150ms ease, background 150ms ease;
}

.item:hover,
.item--aberto {
  background: #212632;
}

.item:focus-visible {
  outline: 2px solid var(--color-signal);
  outline-offset: 2px;
}

.item--true { border-left-color: var(--color-true); }
.item--false { border-left-color: var(--color-false); }
.item--unclear { border-left-color: var(--color-unclear); }

.item-veredito {
  flex: 0 0 118px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.item-veredito-nome {
  font-size: 0.74rem;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.item--true .item-veredito-nome { color: var(--color-true); }
.item--false .item-veredito-nome { color: var(--color-false); }
.item--unclear .item-veredito-nome { color: var(--color-unclear); }

.item-veredito-prob {
  font-size: 0.72rem;
  color: var(--color-paper-dim);
}

.item-corpo {
  flex: 1;
  min-width: 0;
}

.item-conteudo {
  color: var(--color-paper);
  font-size: 0.92rem;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  overflow-wrap: anywhere;
}

.item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 6px;
  font-size: 0.7rem;
  color: var(--color-paper-dim);
}

.item-seta {
  flex: 0 0 auto;
  color: var(--color-paper-dim);
  transition: transform 180ms ease;
}

.item-seta--aberta {
  transform: rotate(180deg);
}

@media (max-width: 599px) {
  .item {
    flex-wrap: wrap;
    gap: 8px 12px;
  }

  .item-veredito {
    flex: 1 1 auto;
    flex-direction: row;
    align-items: baseline;
    gap: 10px;
  }

  .item-corpo {
    flex: 1 1 100%;
    order: 3;
  }
}
</style>
