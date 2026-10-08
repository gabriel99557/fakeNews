<template>
  <form @submit.prevent="onSubmit">
    <div class="row items-center q-mb-sm" style="gap: 8px">
      <span class="eyebrow">Entrada</span>
      <q-badge
        :color="tipoDetectado === 'url' ? 'info' : 'grey-7'"
        :label="tipoDetectado === 'url' ? 'URL detectada' : 'Texto bruto'"
        outline
        class="font-mono"
        style="font-size: 0.68rem"
      />
    </div>

    <q-input
      v-model="conteudo"
      type="textarea"
      autogrow
      :rows="tipoDetectado === 'url' ? 1 : 5"
      placeholder="Cole aqui o texto de uma notícia, ou o link (URL) de uma matéria publicada..."
      filled
      color="primary"
      :rules="[val => (val && val.trim().length > 0) || 'Cole um texto ou uma URL para continuar']"
      :disable="carregando"
      class="font-body"
    />

    <div class="row items-center justify-between q-mt-md" style="gap: 16px">
      <div class="col-auto">
        <div class="eyebrow q-mb-xs">Motor de IA</div>
        <q-btn-toggle
          v-model="modelo"
          no-caps
          spread
          unelevated
          :disable="carregando"
          toggle-color="primary"
          color="neutro"
          text-color="neutro-texto"
          class="font-mono"
          style="font-size: 0.78rem"
          :options="[
            { label: 'Nuvem (Groq)', value: 'nuvem' },
            { label: 'Llama 3 local', value: 'local' }
          ]"
        />
      </div>

      <q-btn
        type="submit"
        :loading="carregando"
        :disable="!conteudo || !conteudo.trim()"
        unelevated
        color="primary"
        class="font-mono text-weight-medium q-px-lg"
        label="Analisar"
        icon-right="fact_check"
      >
        <template #loading>
          <q-spinner-dots />
        </template>
      </q-btn>
    </div>
  </form>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  carregando: { type: Boolean, default: false }
})

const emit = defineEmits(['analisar'])

const conteudo = ref('')
const modelo = ref('nuvem')

const URL_REGEX = /^(https?):\/\/.+/i

const tipoDetectado = computed(() =>
  URL_REGEX.test((conteudo.value || '').trim()) ? 'url' : 'texto'
)

function onSubmit() {
  if (!conteudo.value || !conteudo.value.trim()) return
  emit('analisar', { conteudo: conteudo.value.trim(), modelo: modelo.value })
}

defineExpose({ conteudo })
</script>
