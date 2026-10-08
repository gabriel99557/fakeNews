import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/analise',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

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

/**
 * Lista o histórico de análises (RF03), das mais recentes para as mais antigas.
 * @param {{ pagina?: number, tamanho?: number, veredito?: string|null }} filtros
 * @returns {Promise<import('axios').AxiosResponse>} data = { itens, pagina, tamanho, totalItens, totalPaginas }
 */
export function listarHistorico({ pagina = 0, tamanho = 10, veredito = null } = {}) {
  const params = { pagina, tamanho }
  if (veredito) params.veredito = veredito
  return api.get('/historico', { params })
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
