# CrivoNews

Plataforma para verificação de fatos baseada no ajuste fino do modelo Llama 3 otimizado com LoRA.
Trabalho Final de Graduação — Ciência da Computação, Universidade Franciscana (UFN).

## Estrutura

| Pasta | Descrição |
|---|---|
| `fakeNews/` | Backend — API REST em Spring Boot (Java), orquestra scraping, inferência de IA e persistência |
| `crivo-frontend/` | Frontend — SPA em Vue 3 + Quasar (Vite) |
| `ia-treinamento/` | Fine-tuning do Llama 3.1 8B com LoRA que gera o modelo local `meu-llama-tcc` (ver README da pasta) |

## Como rodar

### Backend (porta 8080)

Defina a chave da Groq em uma variável de ambiente (nunca no código):

```bash
setx GROQ_API_KEY "sua-chave"
```

Ou crie `fakeNews/application.properties` (ignorado pelo git) com
`spring.ai.openai.api-key=sua-chave`. O modelo padrão é `openai/gpt-oss-120b`; troque com `GROQ_MODEL`.

**Autenticação (JWT).** Defina `JWT_SECRET` com pelo menos 32 caracteres. Sem ele, o backend gera
um segredo aleatório a cada inicialização e todos precisam entrar de novo após reiniciar.
Opcionais: `JWT_EXPIRACAO_HORAS` (padrão 8) e `CORS_ORIGENS` (padrão `http://localhost:9000`).

**Banco de dados (RF03 — histórico).** Por padrão o backend usa PostgreSQL em
`localhost:5432`, banco `crivonews`, usuário/senha `postgres`/`postgres`. Para mudar, defina
`DB_URL`, `DB_USERNAME` e `DB_PASSWORD`. As tabelas (`usuarios`, `analises`, `notas`) são criadas automaticamente.

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

Principais endpoints (🔒 = exige `Authorization: Bearer <token>`):

| Método | Caminho | Descrição |
|---|---|---|
| POST | `/api/auth/cadastro` | Cria uma conta e devolve o token |
| POST | `/api/auth/login` | Entra com e-mail e senha e devolve o token |
| GET 🔒 | `/api/auth/me` | Dados do usuário logado |
| POST | `/api/analise/texto` | Analisa texto ou URL com o modelo em nuvem (com login, entra no histórico) |
| POST | `/api/analise/texto/local` | Analisa texto ou URL com o Llama 3 local |
| GET 🔒 | `/api/analise/historico?pagina=0&tamanho=10&veredito=FALSO&avaliacao=DISCORDO` | Histórico pessoal paginado (RF03) |
| GET 🔒 | `/api/analise/historico/{id}` | Detalhe de uma análise |
| PUT 🔒 | `/api/analise/historico/{id}/validacao` | Concorda/discorda do veredito da IA (Seção IV.H) |
| DELETE 🔒 | `/api/analise/historico/{id}/validacao` | Remove a validação |
| POST 🔒 | `/api/analise/historico/{id}/notas` | Adiciona uma anotação pessoal (texto + link de evidência opcional) |
| DELETE 🔒 | `/api/analise/historico/{id}/notas/{notaId}` | Exclui uma anotação |

No Swagger, use o botão **Authorize** com o token recebido no login para testar as rotas 🔒.

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
