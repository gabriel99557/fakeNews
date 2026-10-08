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

**Banco de dados (RF03 — histórico).** Por padrão o backend usa PostgreSQL em
`localhost:5432`, banco `crivonews`, usuário/senha `postgres`/`postgres`. Para mudar, defina
`DB_URL`, `DB_USERNAME` e `DB_PASSWORD`. A tabela `analises` é criada automaticamente.

Depois, dentro de `fakeNews/`:

```bash
./mvnw spring-boot:run
```

Sem PostgreSQL instalado, use o perfil `h2` (banco em memória, apagado ao reiniciar):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

Testes automatizados (usam H2, não precisam de PostgreSQL nem da chave da Groq):

```bash
./mvnw test
```

Documentação da API (Swagger): http://localhost:8080/swagger-ui.html

Para o endpoint `/api/analise/texto/local`, o Ollama precisa estar rodando com o modelo `meu-llama-tcc`.

Principais endpoints:

| Método | Caminho | Descrição |
|---|---|---|
| POST | `/api/analise/texto` | Analisa texto ou URL com o modelo em nuvem |
| POST | `/api/analise/texto/local` | Analisa texto ou URL com o Llama 3 local |
| GET | `/api/analise/historico?pagina=0&tamanho=10&veredito=FALSO` | Histórico paginado (RF03) |
| GET | `/api/analise/historico/{id}` | Detalhe de uma análise do histórico |

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
