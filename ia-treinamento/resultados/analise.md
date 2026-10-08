# Análise complementar (200 notícias do teste avaliadas por todos os modelos)

## Acurácia com intervalo de confiança de 95% (Wilson)

| Modelo | Acertos | Acurácia | IC 95% |
|---|---|---|---|
| ollama:llama3.1:8b | 108/200 | 54,0% | 47,1% a 60,8% |
| ollama:meu-llama-tcc | 177/200 | 88,5% | 83,3% a 92,2% |
| groq:openai/gpt-oss-120b | 109/200 | 54,5% | 47,6% a 61,3% |

## Acurácia por categoria

| Categoria | n | ollama:llama3.1:8b | ollama:meu-llama-tcc | groq:openai/gpt-oss-120b |
|---|---|---|---|---|
| Política | 90 | 58,9% | 90,0% | 57,8% |
| TV e celebridades | 52 | 44,2% | 92,3% | 42,3% |
| Sociedade e cotidiano | 46 | 54,3% | 84,8% | 60,9% |
| Ciência e tecnologia | 8 | 62,5% | 75,0% | 62,5% |
| Religião | 4 | 50,0% | 75,0% | 50,0% |

## probabilidadeFake por classe esperada (mediana e faixa entre os percentis 10 e 90)

| Modelo | Notícias falsas | Notícias verdadeiras |
|---|---|---|
| ollama:llama3.1:8b | 80 (0 a 90) | 10 (0 a 80) |
| ollama:meu-llama-tcc | 82 (12 a 82) | 12 (8 a 18) |
| groq:openai/gpt-oss-120b | 85 (20 a 92) | 40 (10 a 85) |

## Teste de McNemar (exato) entre pares de modelos

b = notícias que só o primeiro acertou; c = só o segundo acertou. p < 0,05 indica diferença de acerto estatisticamente significativa.

| Comparação | b | c | valor-p | Diferença significativa (5%)? |
|---|---|---|---|---|
| ollama:llama3.1:8b × ollama:meu-llama-tcc | 4 | 73 | < 0,0001 | sim |
| ollama:llama3.1:8b × groq:openai/gpt-oss-120b | 34 | 35 | 1,0000 | não |
| ollama:meu-llama-tcc × groq:openai/gpt-oss-120b | 76 | 8 | < 0,0001 | sim |

## Latência e ambiente de execução

Notícias refeitas na CPU: a GPU AMD da máquina de avaliação às vezes degenerou a geração em "@@@@"; nesses casos a notícia foi processada de novo só na CPU, o que também eleva a latência dela.

| Modelo | Latência mediana (s) | Latência média (s) | Refeitas na CPU |
|---|---|---|---|
| ollama:llama3.1:8b | 41,6 | 44,3 | 1 |
| ollama:meu-llama-tcc | 35,9 | 37,7 | 1 |
| groq:openai/gpt-oss-120b | 6,7 | 6,9 | 0 |
