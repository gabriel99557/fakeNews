<template>
  <div class="painel-pessoal q-pa-lg">
    <!-- Validação do veredito -->
    <div class="eyebrow q-mb-sm">Sua validação</div>

    <div v-if="editandoValidacao || !analise.avaliacaoUsuario">
      <template v-if="!discordando">
        <p class="q-mb-sm text-body2" style="color: var(--color-paper)">
          Você concorda com o veredito da IA?
        </p>
        <div class="row" style="gap: 8px">
          <q-btn
            unelevated
            no-caps
            color="positive"
            icon="thumb_up"
            label="Concordo"
            class="font-mono btn-validacao"
            :loading="salvandoValidacao === 'concordo'"
            :disable="!!salvandoValidacao"
            @click="concordar"
          />
          <q-btn
            outline
            no-caps
            color="negative"
            icon="thumb_down"
            label="Discordo"
            class="font-mono btn-validacao"
            :disable="!!salvandoValidacao"
            @click="discordando = true"
          />
          <q-btn
            v-if="editandoValidacao"
            flat
            no-caps
            label="Cancelar"
            class="font-mono btn-validacao"
            style="color: var(--color-paper-dim)"
            @click="editandoValidacao = false"
          />
        </div>
      </template>

      <template v-else>
        <p class="q-mb-sm text-body2" style="color: var(--color-paper)">
          Na sua avaliação, qual seria o veredito correto?
        </p>
        <div class="row items-center" style="gap: 8px">
          <q-btn-toggle
            v-model="vereditoEscolhido"
            no-caps
            unelevated
            toggle-color="orange-6"
            toggle-text-color="dark"
            color="grey-9"
            text-color="grey-4"
            class="font-mono btn-validacao"
            :options="opcoesDiscordancia"
          />
          <q-btn
            unelevated
            no-caps
            color="orange-6"
            text-color="dark"
            label="Salvar"
            class="font-mono btn-validacao"
            :disable="!vereditoEscolhido"
            :loading="salvandoValidacao === 'discordo'"
            @click="discordar"
          />
          <q-btn
            flat
            no-caps
            label="Cancelar"
            class="font-mono btn-validacao"
            style="color: var(--color-paper-dim)"
            @click="cancelarDiscordancia"
          />
        </div>
      </template>
    </div>

    <div v-else class="row items-center" style="gap: 12px">
      <div class="selo-validacao font-mono" :class="analise.avaliacaoUsuario === 'CONCORDO' ? 'selo--concordo' : 'selo--discordo'">
        <q-icon :name="analise.avaliacaoUsuario === 'CONCORDO' ? 'thumb_up' : 'thumb_down'" size="15px" />
        <span v-if="analise.avaliacaoUsuario === 'CONCORDO'">Você concorda com a IA</span>
        <span v-else>Você discorda — para você: <strong>{{ analise.vereditoUsuario }}</strong></span>
      </div>
      <div class="row" style="gap: 4px">
        <q-btn flat dense no-caps label="Alterar" class="font-mono link-acao" @click="editandoValidacao = true" />
        <q-btn
          flat
          dense
          no-caps
          label="Remover"
          class="font-mono link-acao"
          :loading="salvandoValidacao === 'remover'"
          @click="limparValidacao"
        />
      </div>
    </div>

    <hr class="divider-line q-my-lg" />

    <!-- Anotações -->
    <div class="eyebrow q-mb-sm">
      Suas anotações <span v-if="notas.length" style="color: var(--color-paper-dim)">({{ notas.length }})</span>
    </div>

    <ul v-if="notas.length" class="lista-notas q-mb-md">
      <li v-for="nota in notas" :key="nota.id" class="nota">
        <div class="nota-texto">{{ nota.texto }}</div>
        <a
          v-if="nota.linkEvidencia"
          :href="nota.linkEvidencia"
          target="_blank"
          rel="noopener noreferrer"
          class="nota-link font-mono"
        >
          <q-icon name="link" size="14px" /> {{ nota.linkEvidencia }}
        </a>
        <div class="row items-center justify-between q-mt-xs">
          <span class="font-mono nota-data">{{ formatarData(nota.dataCriacao) }}</span>
          <q-btn
            flat
            dense
            round
            size="sm"
            icon="delete_outline"
            class="nota-excluir"
            :loading="excluindoNotaId === nota.id"
            :aria-label="`Excluir anotação de ${formatarData(nota.dataCriacao)}`"
            @click="excluirNota(nota.id)"
          />
        </div>
      </li>
    </ul>
    <p v-else class="text-body2 q-mb-md" style="color: var(--color-paper-dim)">
      Registre apontamentos, contrapontos ou evidências sobre esta notícia para consultar depois.
    </p>

    <q-form class="column" style="gap: 8px" @submit="salvarNota">
      <q-input
        v-model="textoNota"
        type="textarea"
        autogrow
        dark
        filled
        color="orange-6"
        placeholder="Escreva uma anotação…"
        maxlength="2000"
        counter
        :disable="salvandoNota"
      />
      <q-input
        v-model="linkNota"
        dark
        filled
        dense
        color="orange-6"
        placeholder="Link de evidência (opcional) — ex.: uma checagem publicada"
        maxlength="2048"
        :disable="salvandoNota"
        :rules="[v => !v || /^https?:\/\/\S+$/i.test(v.trim()) || 'O link deve começar com http:// ou https://']"
        lazy-rules
      >
        <template #prepend><q-icon name="link" size="18px" /></template>
      </q-input>
      <div>
        <q-btn
          type="submit"
          unelevated
          no-caps
          color="orange-6"
          text-color="dark"
          icon="add"
          label="Adicionar anotação"
          class="font-mono btn-validacao"
          :disable="!textoNota.trim()"
          :loading="salvandoNota"
        />
      </div>
    </q-form>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Notify } from 'quasar'
import {
  validarAnalise,
  removerValidacao,
  adicionarNota,
  removerNota,
  extrairMensagemErro
} from '../services/api.js'

const props = defineProps({
  /** Análise com id, veredito e (opcionalmente) avaliacaoUsuario, vereditoUsuario e notas */
  analise: { type: Object, required: true }
})

const emit = defineEmits(['atualizado'])

const VEREDITOS = [
  { label: 'Verdadeiro', value: 'VERDADEIRO' },
  { label: 'Falso', value: 'FALSO' },
  { label: 'Inconclusivo', value: 'INCONCLUSIVO' }
]

const notas = computed(() => props.analise.notas || [])
const opcoesDiscordancia = computed(() => VEREDITOS.filter(v => v.value !== props.analise.veredito))

const editandoValidacao = ref(false)
const discordando = ref(false)
const vereditoEscolhido = ref(null)
const salvandoValidacao = ref(null)

const textoNota = ref('')
const linkNota = ref('')
const salvandoNota = ref(false)
const excluindoNotaId = ref(null)

/**
 * Logo após uma análise, a gravação no histórico ainda pode estar em andamento
 * (ela é assíncrona no backend). Se a API responder 404, tenta mais uma vez.
 */
async function comNovaTentativa(chamada) {
  try {
    return await chamada()
  } catch (e) {
    if (e.response?.status !== 404) throw e
    await new Promise(r => setTimeout(r, 1200))
    return await chamada()
  }
}

async function executar(chamada, mensagemSucesso) {
  try {
    const { data } = await comNovaTentativa(chamada)
    emit('atualizado', data)
    if (mensagemSucesso) Notify.create({ type: 'positive', message: mensagemSucesso, timeout: 2000 })
    return true
  } catch (e) {
    Notify.create({ type: 'negative', message: extrairMensagemErro(e) })
    return false
  }
}

async function concordar() {
  salvandoValidacao.value = 'concordo'
  if (await executar(() => validarAnalise(props.analise.id, { concorda: true }), 'Validação registrada.')) {
    editandoValidacao.value = false
  }
  salvandoValidacao.value = null
}

async function discordar() {
  salvandoValidacao.value = 'discordo'
  const ok = await executar(
    () => validarAnalise(props.analise.id, { concorda: false, vereditoUsuario: vereditoEscolhido.value }),
    'Validação registrada.'
  )
  if (ok) {
    cancelarDiscordancia()
    editandoValidacao.value = false
  }
  salvandoValidacao.value = null
}

function cancelarDiscordancia() {
  discordando.value = false
  vereditoEscolhido.value = null
}

async function limparValidacao() {
  salvandoValidacao.value = 'remover'
  await executar(() => removerValidacao(props.analise.id))
  salvandoValidacao.value = null
}

async function salvarNota() {
  if (!textoNota.value.trim()) return
  salvandoNota.value = true
  const ok = await executar(
    () => adicionarNota(props.analise.id, { texto: textoNota.value.trim(), linkEvidencia: linkNota.value.trim() }),
    'Anotação salva.'
  )
  if (ok) {
    textoNota.value = ''
    linkNota.value = ''
  }
  salvandoNota.value = false
}

async function excluirNota(notaId) {
  excluindoNotaId.value = notaId
  await executar(() => removerNota(props.analise.id, notaId), 'Anotação excluída.')
  excluindoNotaId.value = null
}

function formatarData(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleString('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}
</script>

<style scoped>
.painel-pessoal {
  background: var(--color-ink-soft);
  border: 1px solid var(--color-ink-line);
  border-top: 2px solid var(--color-signal);
  border-radius: 4px;
}

.btn-validacao {
  font-size: 0.78rem;
}

.selo-validacao {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.78rem;
  padding: 6px 12px;
  border-radius: 4px;
  border: 1px solid;
}

.selo--concordo {
  color: var(--color-true);
  background: var(--color-true-soft);
  border-color: var(--color-true);
}

.selo--discordo {
  color: var(--color-false);
  background: var(--color-false-soft);
  border-color: var(--color-false);
}

.link-acao {
  font-size: 0.74rem;
  color: var(--color-paper-dim);
}

.lista-notas {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nota {
  background: var(--color-ink);
  border: 1px solid var(--color-ink-line);
  border-left: 3px solid var(--color-signal);
  border-radius: 2px;
  padding: 10px 12px 6px;
}

.nota-texto {
  color: var(--color-paper);
  font-size: 0.92rem;
  line-height: 1.5;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.nota-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  font-size: 0.74rem;
  color: var(--color-signal);
  text-decoration: none;
  overflow-wrap: anywhere;
  word-break: break-all;
}

.nota-link:hover {
  text-decoration: underline;
}

.nota-data {
  font-size: 0.7rem;
  color: var(--color-paper-dim);
}

.nota-excluir {
  color: var(--color-paper-dim);
}

.nota-excluir:hover {
  color: var(--color-false);
}
</style>
