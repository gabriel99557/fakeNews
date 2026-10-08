import { ref } from 'vue'
import { Dark } from 'quasar'

/**
 * Tema claro (padrão, cores da UFN) ou escuro. A escolha fica no localStorage
 * e é aplicada no atributo data-theme do <html>, que seleciona as variáveis de
 * cor em src/css/app.scss, e no plugin Dark do Quasar (componentes como q-input).
 *
 * O index.html aplica o tema salvo antes de o Vue carregar, para a página não
 * aparecer no tema errado por um instante.
 */
const CHAVE = 'crivonews.tema'

function lerTemaSalvo() {
  try {
    return localStorage.getItem(CHAVE) === 'dark' ? 'dark' : 'light'
  } catch {
    return 'light'
  }
}

export const tema = ref(lerTemaSalvo())

function aplicar(valor) {
  document.documentElement.setAttribute('data-theme', valor)
  Dark.set(valor === 'dark')
}

export function iniciarTema() {
  aplicar(tema.value)
}

export function alternarTema() {
  tema.value = tema.value === 'dark' ? 'light' : 'dark'
  aplicar(tema.value)
  try {
    localStorage.setItem(CHAVE, tema.value)
  } catch {
    // Sem localStorage (ex.: navegação privada): o tema vale só até recarregar a página
  }
}
