<template>
  <svg viewBox="0 0 220 220" class="verdict-stamp" :class="corClasse" width="176" height="176">
    <defs>
      <path
        id="stampEdgePath"
        d="M 110,110 m -86,0 a 86,86 0 1,1 172,0 a 86,86 0 1,1 -172,0"
      />
    </defs>

    <circle cx="110" cy="110" r="104" class="stamp-ring-outer" />
    <circle cx="110" cy="110" r="86" class="stamp-ring" />
    <circle cx="110" cy="110" r="70" class="stamp-ring-inner" />

    <text class="stamp-edge-text">
      <textPath href="#stampEdgePath" startOffset="2%">
        CRIVONEWS · ANÁLISE POR IA · {{ origemLabel }} ·
      </textPath>
    </text>

    <text x="110" y="102" text-anchor="middle" class="stamp-veredito">{{ veredito }}</text>
    <text x="110" y="132" text-anchor="middle" class="stamp-percentual">{{ probabilidadeFake }}% fake</text>
  </svg>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  veredito: { type: String, required: true }, // VERDADEIRO | FALSO | INCONCLUSIVO
  probabilidadeFake: { type: Number, required: true },
  origem: { type: String, default: 'texto' }
})

const corClasse = computed(() => {
  switch (props.veredito) {
    case 'VERDADEIRO': return 'stamp--true'
    case 'FALSO': return 'stamp--false'
    default: return 'stamp--unclear'
  }
})

const origemLabel = computed(() => (props.origem === 'url' ? 'FONTE: URL' : 'FONTE: TEXTO'))
</script>

<style scoped>
.verdict-stamp {
  overflow: visible;
}

.stamp-ring-outer {
  fill: none;
  stroke: currentColor;
  stroke-width: 1;
  opacity: 0.35;
}

.stamp-ring {
  fill: none;
  stroke: currentColor;
  stroke-width: 2.5;
}

.stamp-ring-inner {
  fill: none;
  stroke: currentColor;
  stroke-width: 1;
  stroke-dasharray: 2 4;
  opacity: 0.6;
}

.stamp-edge-text {
  font-family: var(--font-mono);
  font-size: 8.5px;
  letter-spacing: 0.14em;
  fill: currentColor;
  opacity: 0.85;
}

.stamp-veredito {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 27px;
  fill: currentColor;
}

.stamp-percentual {
  font-family: var(--font-mono);
  font-size: 12px;
  letter-spacing: 0.04em;
  fill: currentColor;
  opacity: 0.85;
}

.stamp--true { color: var(--color-true); }
.stamp--false { color: var(--color-false); }
.stamp--unclear { color: var(--color-unclear); }
</style>
