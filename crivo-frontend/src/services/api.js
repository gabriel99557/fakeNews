import axios from 'axios'
import { Notify } from 'quasar'
import { tokenAtual, encerrarSessao } from './auth.js'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/analise',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Rotas de autenticação ficam em /api/auth (irmã de /api/analise)
const apiAuth = axios.create({
  baseURL: (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/analise').replace(/\/analise\/?$/, '/auth'),
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Envia o token JWT em toda requisição feita por um usuário logado
for (const cliente of [api, apiAuth]) {
  cliente.interceptors.request.use((config) => {
    const token = tokenAtual()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  })

  // Token recusado (expirou ou o servidor trocou o segredo): encerra a sessão e leva ao login
  cliente.interceptors.response.use(undefined, async (error) => {
    if (error.response?.status === 401 && error.config?.headers?.Authorization) {
      encerrarSessao()
      Notify.create({ type: 'warning', message: 'Sua sessão expirou. Entre novamente.' })
      const { default: router } = await import('../router')
      const atual = router.currentRoute.value
      if (atual.name !== 'entrar') {
        router.push({ name: 'entrar', query: { redirect: atual.fullPath } })
      }
    }
    return Promise.reject(error)
  })
}

// ---------- Análise ----------

/**
 * Envia texto bruto ou URL para análise.
 * @param {string} conteudo - texto da notícia ou URL
 * @param {'nuvem'|'local'} modelo - qual motor de IA consultar no backend
 * @returns {Promise<import('axios').AxiosResponse>}
 */
export function analisarConteudo(conteudo, modelo = 'nuvem') {
  const caminho = modelo === 'local' ? '/texto/local' : '/texto'
  return api.post(caminho, { conteudo })
}

// ---------- Autenticação ----------

/** @returns {Promise<import('axios').AxiosResponse>} data = { token, expiraEm, usuario } */
export function cadastrar({ nome, email, senha }) {
  return apiAuth.post('/cadastro', { nome, email, senha })
}

/** @returns {Promise<import('axios').AxiosResponse>} data = { token, expiraEm, usuario } */
export function entrar({ email, senha }) {
  return apiAuth.post('/login', { email, senha })
}

// ---------- Histórico, validação e anotações (exigem login) ----------

/**
 * Lista o histórico do usuário logado (RF03), das mais recentes para as mais antigas.
 * @param {{ pagina?: number, tamanho?: number, veredito?: string|null, avaliacao?: string|null }} filtros
 * @returns {Promise<import('axios').AxiosResponse>} data = { itens, pagina, tamanho, totalItens, totalPaginas }
 */
export function listarHistorico({ pagina = 0, tamanho = 10, veredito = null, avaliacao = null } = {}) {
  const params = { pagina, tamanho }
  if (veredito) params.veredito = veredito
  if (avaliacao) params.avaliacao = avaliacao
  return api.get('/historico', { params })
}

/**
 * Registra se o usuário concorda com o veredito da IA (Seção IV.H do TFG).
 * @param {string} id - id da análise
 * @param {{ concorda: boolean, vereditoUsuario?: string }} validacao
 */
export function validarAnalise(id, validacao) {
  return api.put(`/historico/${id}/validacao`, validacao)
}

export function removerValidacao(id) {
  return api.delete(`/historico/${id}/validacao`)
}

/** @param {{ texto: string, linkEvidencia?: string }} nota */
export function adicionarNota(id, nota) {
  return api.post(`/historico/${id}/notas`, nota)
}

export function removerNota(id, notaId) {
  return api.delete(`/historico/${id}/notas/${notaId}`)
}

/**
 * Extrai uma mensagem de erro amigável de uma resposta de erro da API,
 * que segue o formato ErroResponse { status, mensagem, timestamp }.
 */
export function extrairMensagemErro(error) {
  if (error.response?.data?.mensagem) {
    return error.response.data.mensagem
  }
  if (error.code === 'ECONNABORTED') {
    return 'A análise demorou mais que o esperado. Tente novamente.'
  }
  if (error.message === 'Network Error') {
    return 'Não foi possível conectar ao servidor. Verifique se o backend está em execução.'
  }
  return 'Ocorreu um erro inesperado ao processar a análise.'
}

export default api
