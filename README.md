# CrivoNews

Plataforma para verificação de fatos baseada no ajuste fino do modelo Llama 3 otimizado com LoRA.
Trabalho Final de Graduação — Ciência da Computação, Universidade Franciscana (UFN).

## Estrutura

| Pasta | Descrição |
|---|---|
| `fakeNews/` | Backend — API REST em Spring Boot (Java), orquestra scraping, inferência de IA e persistência |
| `crivo-frontend/` | Frontend — SPA em Vue 3 + Quasar (Vite) |

## Como rodar

### Backend (porta 8080)

Defina a chave da Groq em uma variável de ambiente (nunca no código):

```bash
setx GROQ_API_KEY "sua-chave"
```

Depois, dentro de `fakeNews/`:

```bash
./mvnw spring-boot:run
```

Documentação da API (Swagger): http://localhost:8080/swagger-ui.html

Para o endpoint `/api/analise/texto/local`, o Ollama precisa estar rodando com o modelo `meu-llama-tcc`.

### Frontend (porta 9000)

Dentro de `crivo-frontend/`:

```bash
npm install
cp .env.example .env
npm run dev
```

## Branches

Fluxo GitFlow adaptado:

- `main` — código estável e homologado
- `develop` — integração das funcionalidades de cada Sprint
- `feature/*` — uma branch por requisito, criada a partir da `develop` e integrada via Pull Request
