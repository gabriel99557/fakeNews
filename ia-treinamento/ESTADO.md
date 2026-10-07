# Estado do treinamento — atualizado em 07/10/2026

Anotações para continuar o trabalho em outro computador. O passo a passo completo está no [README](README.md).

## Onde paramos

| Passo | Situação |
|---|---|
| 1. Preparar corpus | ✅ Feito. `dados/base.jsonl` versionado (7.200 notícias: treino 5.762, validação 720, teste 718) |
| 2. Justificativas (Groq) | ✅ **1.200 de 1.200** em `dados/anotacoes.jsonl` (0 falhas; 600 falsas / 600 verdadeiras) |
| 3. Montar dataset | ✅ `treino.jsonl` 1.099 exemplos (539 FALSO / 539 VERDADEIRO / 21 INCONCLUSIVO) e `validacao.jsonl` 129 (61 / 61 / 7) |
| 4. Treinar no Colab | ✅ Feito em 06–07/10 (ver "Resultado do treino") |
| 5. Ollama | ✅ Importado em 07/10 como `meu-llama-tcc` (modelo antigo removido) |
| 6. Avaliação | ⏳ **Próximo passo** |

Histórico do passo 2: dia 3 (05/10) +316 anotações até o limite diário; 06/10 +102 para completar as 1.200.

## Resultado do treino (06–07/10/2026)

Colab gratuito, GPU T4, conta **gabrielmatheus9845@gmail.com** (pasta `Meu Drive/crivonews` dessa conta).

| | |
|---|---|
| Exemplos | 1.095 de treino (4 descartados por passarem de 2.048 tokens) e 129 de validação |
| Configuração | QLoRA 4 bits, `r=16`, `alpha=16`, 2 épocas, lote efetivo 8, `lr=2e-4` → 274 passos |
| Parâmetros treinados | 41,9 mi de 8,07 bi (0,52%) |
| Duração | 1h19min de treino + ~50 min de exportação (merge 16 bits + GGUF) |
| *Loss* (treino / validação) | passo 100: 0,511 / 0,526 · passo 200: 0,391 / 0,481 · **final (274): 0,383 / 0,468** |
| Teste rápido (1 exemplo da validação) | gerou `FALSO`, 82, igual ao esperado; JSON válido |

A *loss* de validação caiu em todas as medições: sem sinal de *overfitting*.

Arquivos gerados, em `Meu Drive/crivonews`:

- `crivo.Q4_K_M.gguf` (4,92 GB): modelo para o Ollama
- `historico_treino.csv`: curva de *loss* (gráfico do capítulo de resultados)
- `crivo-lora/`: apenas o adaptador LoRA (pequeno; guardar)

## Importação no Ollama (07/10/2026)

- GGUF baixado do Drive para `C:\modelos\crivo\crivo.Q4_K_M.gguf` (4.920.734.976 bytes), com o `Modelfile` ao lado.
- `ollama rm meu-llama-tcc` (classificador binário antigo) e `ollama create meu-llama-tcc -f Modelfile`.
- Teste com o mesmo prompt do backend: 3 de 3 corretos e JSON válido em todos.

| Caso | Veredito | % fake | Tempo |
|---|---|---|---|
| Notícia falsa inventada (cura do câncer) | FALSO | 82 | 41 s (inclui carregar o modelo) |
| Notícia verdadeira em estilo de jornal (IBGE) | VERDADEIRO | 8 | 22 s |
| Boato sem confirmação (falta de água) | INCONCLUSIVO | 50 | 19 s |

- Pelo site (Llama 3 local), o boato da poupança saiu FALSO, 82%. O frontend passou a esperar até 180 s
  pelo modelo local (antes, 30 s, o que estourava). Nesta máquina parte do modelo roda na CPU.

## Próximo passo: avaliação (capítulo de resultados)

```bash
ollama pull llama3.1:8b
```

```bash
python 5_avaliar.py --modelo ollama:llama3.1:8b --modelo ollama:meu-llama-tcc --modelo groq:openai/gpt-oss-120b
```

- São 200 notícias do teste por modelo (`--limite 0` para as 718). Com ~20 s por notícia no modelo local, conte
  ~1h10 para cada modelo do Ollama. A parte da Groq consome a cota diária (~200 notícias cabem nela).
- Observe a **% de INCONCLUSIVO** no resumo: o teste não tem casos inconclusivos, então previsões
  INCONCLUSIVO ali indicam atalho de estilo.

## Limite da Groq (importante)

- Plano gratuito do `openai/gpt-oss-120b`: **200.000 tokens por dia** (TPD). Cada notícia gasta ~530–630 tokens → **~316 a 376 por rodada**
  (no dia 3 rendeu 316).
- Os tokens só voltam ~24 h depois de gastos. Rodar de novo no mesmo dia quase não rende; faça uma rodada por dia.
- O script para sozinho ao atingir o limite e retoma de onde parou na próxima execução.
- É a mesma cota da análise "Nuvem (Groq)" do site: durante a rodada, ela pode falhar.

## Google Drive / Colab

- Pasta **crivonews** na conta `matheuspichler94@gmail.com`, compartilhada como editor com `gabriel.pinheirogsp2001@gmail.com`.
- Notebook: https://colab.research.google.com/drive/17dyDFAzjhXymhl7DZfgVBQTX1AmIaSFm
- Na conta `gabriel.pinheirogsp2001`: em **Compartilhados comigo**, botão direito na pasta → **Organizar → Adicionar atalho → Meu Drive**.
  O notebook procura os arquivos em `Meu Drive/crivonews`.

## Pendências

- Na avaliação (passo 5), observar a **% de INCONCLUSIVO no conjunto de teste**: ele não tem casos inconclusivos,
  então previsões INCONCLUSIVO ali indicam que o modelo aprendeu um atalho de estilo (os exemplos manuais usam
  mais "informou que", "especialistas" e "segundo" do que as notícias do Fake.Br).
- A probabilidade gerada para as notícias falsas ficou concentrada em 78–82; aceitável, já que o veredito é o principal.
- O modelo treinado deve ser importado no Ollama com o nome exato `meu-llama-tcc` (fixo no `AnaliseTextoService`).
- Hardware do PC original: GPU AMD RX 6650 XT (sem CUDA), por isso o treino é no Colab. Se o outro computador tiver
  GPU NVIDIA com 12 GB+ de VRAM, dá para treinar localmente com o mesmo notebook (pule as células do Drive).
