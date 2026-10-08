# CrivoNews — Frontend

Interface web (Vue 3 + Quasar, via Vite) para o verificador de desinformação.
Consome a API Spring Boot desenvolvida em paralelo (`fakeNews-atualizado.zip`).

## Como rodar

1. Instale as dependências:
   ```bash
   npm install
   ```

2. Copie o arquivo de ambiente e ajuste a URL do backend se necessário:
   ```bash
   cp .env.example .env
   ```

3. Suba o backend Spring Boot (porta 8080) e depois o frontend:
   ```bash
   npm run dev
   ```

4. Acesse http://localhost:9000

## Estrutura

- `src/router/index.js` — rotas `/` (análise), `/historico` (exige login) e `/entrar`
- `src/pages/IndexPage.vue` — hero, formulário e resultado
- `src/pages/HistoricoPage.vue` — histórico pessoal paginado, filtros por veredito e por avaliação (RF03)
- `src/pages/EntrarPage.vue` — login e cadastro
- `src/components/ValidacaoAnotacoes.vue` — validação do veredito e anotações pessoais (Seção IV.H)
- `src/components/AppHeader.vue` — cabeçalho com navegação e menu do usuário
- `src/components/AnaliseForm.vue` — input de texto/URL + seleção de modelo (nuvem/local)
- `src/components/ResultadoAnalise.vue` — card de resultado
- `src/components/VerdictStamp.vue` — selo circular do veredito (elemento visual de assinatura)
- `src/services/api.js` — cliente Axios (envia o token JWT e trata sessão expirada)
- `src/services/auth.js` — sessão do usuário logado (token salvo no localStorage)
