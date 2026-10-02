import { reactive, computed } from 'vue'

/**
 * Sessão do usuário logado (token JWT + dados básicos), guardada no
 * localStorage para sobreviver a recarregamentos da página.
 */
const CHAVE = 'crivonews.sessao'

function sessaoValida(sessao) {
  return !!(sessao && sessao.token && sessao.expiraEm && new Date(sessao.expiraEm) > new Date())
}

function lerSessaoSalva() {
  try {
    const sessao = JSON.parse(localStorage.getItem(CHAVE))
    return sessaoValida(sessao) ? sessao : null
  } catch {
    return null
  }
}

const estado = reactive({ sessao: lerSessaoSalva() })

export const usuarioLogado = computed(() => estado.sessao?.usuario ?? null)
export const estaLogado = computed(() => estado.sessao !== null)

/** Token atual, ou null se não houver sessão (ou se ela já expirou). */
export function tokenAtual() {
  if (estado.sessao && !sessaoValida(estado.sessao)) {
    encerrarSessao()
  }
  return estado.sessao?.token ?? null
}

/** @param {{ token: string, expiraEm: string, usuario: { id: number, nome: string, email: string } }} resposta */
export function iniciarSessao(resposta) {
  estado.sessao = { token: resposta.token, expiraEm: resposta.expiraEm, usuario: resposta.usuario }
  try {
    localStorage.setItem(CHAVE, JSON.stringify(estado.sessao))
  } catch {
    // Sem localStorage (ex.: navegação privada): a sessão vale só até recarregar a página
  }
}

export function encerrarSessao() {
  estado.sessao = null
  try {
    localStorage.removeItem(CHAVE)
  } catch {
    // ignorado
  }
}
