<template>
  <div class="resultado-card q-pa-lg">
    <div class="row items-start" style="gap: 28px">
      <div class="col-12 col-sm-auto row justify-center">
        <verdict-stamp
          :veredito="resultado.veredito"
          :probabilidade-fake="resultado.probabilidadeFake"
          :origem="resultado.origem"
        />
      </div>

      <div class="col-12 col-sm" style="min-width: 0">
        <div class="eyebrow q-mb-xs">{{ tituloVeredito }}</div>
        <p class="q-mb-md text-body1" style="color: var(--color-paper); max-width: 56ch">
          {{ resultado.justificativa }}
        </p>

        <div v-if="resultado.indicadores && resultado.indicadores.length" class="q-mb-sm">
          <div class="eyebrow q-mb-xs">Indicadores identificados</div>
          <ul class="indicadores-lista">
            <li v-for="(indicador, i) in resultado.indicadores" :key="i" :class="corVeredito">
              {{ indicador }}
            </li>
          </ul>
        </div>

        <div class="row items-center q-mt-md font-mono text-caption" style="color: var(--color-paper-dim); gap: 16px">
          <span>{{ dataFormatada }}</span>
          <span>·</span>
          <span>{{ resultado.origem === 'url' ? 'origem: URL' : 'origem: texto colado' }}</span>
          <template v-if="resultado.modelo">
            <span>·</span>
            <span>{{ resultado.modelo === 'local' ? 'modelo: Llama 3 local' : 'modelo: nuvem (Groq)' }}</span>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import VerdictStamp from './VerdictStamp.vue'

const props = defineProps({
  resultado: { type: Object, required: true }
})

const tituloVeredito = computed(() => {
  switch (props.resultado.veredito) {
    case 'VERDADEIRO': return 'Sem indícios relevantes de desinformação'
    case 'FALSO': return 'Padrões de desinformação identificados'
    default: return 'Resultado inconclusivo — avalie com cautela'
  }
})

const corVeredito = computed(() => {
  switch (props.resultado.veredito) {
    case 'VERDADEIRO': return 'indicador--true'
    case 'FALSO': return 'indicador--false'
    default: return 'indicador--unclear'
  }
})

const dataFormatada = computed(() => {
  if (!props.resultado.dataAnalise) return ''
  const d = new Date(props.resultado.dataAnalise)
  return d.toLocaleString('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
})
</script>

<style scoped>
.resultado-card {
  background: var(--color-ink-soft);
  border: 1px solid var(--color-ink-line);
  border-radius: 4px;
  animation: fade-up 320ms ease both;
}

@keyframes fade-up {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.indicadores-lista {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.indicadores-lista li {
  font-family: var(--font-mono);
  font-size: 0.78rem;
  line-height: 1.5;
  color: var(--color-paper);
  background: var(--color-ink);
  border: 1px solid var(--color-ink-line);
  border-left-width: 3px;
  border-radius: 2px;
  padding: 7px 10px;
  white-space: normal;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.indicador--true { border-left-color: var(--color-true); }
.indicador--false { border-left-color: var(--color-false); }
.indicador--unclear { border-left-color: var(--color-unclear); }
</style>
