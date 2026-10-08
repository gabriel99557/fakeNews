# Avaliação no conjunto de teste (Fake.Br, size-normalized)

| Modelo | n | JSON válido | Acurácia | Precisão (fake) | Revocação (fake) | F1 (fake) | F1 macro | Inconclusivo | Latência média (s) |
|---|---|---|---|---|---|---|---|---|---|
| ollama:llama3.1:8b | 200 | 98.5% | 54.0% | 65.3% | 62.0% | 0.636 | 0.597 | 18.5% | 44.3 |
| ollama:meu-llama-tcc | 200 | 99.5% | 88.5% | 97.6% | 80.0% | 0.879 | 0.887 | 0.0% | 37.7 |
| groq:openai/gpt-oss-120b | 200 | 100.0% | 54.5% | 68.0% | 66.0% | 0.670 | 0.614 | 24.5% | 6.9 |

**ollama:llama3.1:8b** — matriz de confusão (linhas = esperado, colunas = previsto)

| | FALSO | VERDADEIRO | INCONCLUSIVO | SEM JSON |
|---|---|---|---|---|
| FALSO | 62 | 19 | 17 | 2 |
| VERDADEIRO | 33 | 46 | 20 | 1 |

**ollama:meu-llama-tcc** — matriz de confusão (linhas = esperado, colunas = previsto)

| | FALSO | VERDADEIRO | INCONCLUSIVO | SEM JSON |
|---|---|---|---|---|
| FALSO | 80 | 20 | 0 | 0 |
| VERDADEIRO | 2 | 97 | 0 | 1 |

**groq:openai/gpt-oss-120b** — matriz de confusão (linhas = esperado, colunas = previsto)

| | FALSO | VERDADEIRO | INCONCLUSIVO | SEM JSON |
|---|---|---|---|---|
| FALSO | 66 | 11 | 23 | 0 |
| VERDADEIRO | 31 | 43 | 26 | 0 |
