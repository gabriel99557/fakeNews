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

- `src/pages/IndexPage.vue` — página única com hero, formulário e resultado
- `src/components/AnaliseForm.vue` — input de texto/URL + seleção de modelo (nuvem/local)
- `src/components/ResultadoAnalise.vue` — card de resultado
- `src/components/VerdictStamp.vue` — selo circular do veredito (elemento visual de assinatura)
- `src/services/api.js` — cliente Axios para os endpoints `/api/analise/texto` e `/api/analise/texto/local`
