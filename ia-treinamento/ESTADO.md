# Estado do treinamento — atualizado em 06/10/2026

Anotações para continuar o trabalho em outro computador. O passo a passo completo está no [README](README.md).

## Onde paramos

| Passo | Situação |
|---|---|
| 1. Preparar corpus | ✅ Feito. `dados/base.jsonl` versionado (7.200 notícias: treino 5.762, validação 720, teste 718) |
| 2. Justificativas (Groq) | ✅ **1.200 de 1.200** em `dados/anotacoes.jsonl` (0 falhas; 600 falsas / 600 verdadeiras) |
| 3. Montar dataset | ✅ `treino.jsonl` 1.106 exemplos (539 FALSO / 539 VERDADEIRO / 28 INCONCLUSIVO) e `validacao.jsonl` 129 (61 / 61 / 7) |
| 4. Treinar no Colab | ⏳ **Próximo passo.** Notebook já está no Google Drive (ver abaixo) |
| 5. Ollama + avaliação | Depois do treino |

Histórico do passo 2: dia 3 (05/10) +316 anotações até o limite diário; 06/10 +102 para completar as 1.200.

## Próximo passo: treinar

1. **Revise os 35 exemplos de `inconclusivos_manual.jsonl`** (pendência abaixo). Se alterar algum, rode de novo
   `python 3_montar_dataset.py` (leva segundos).
2. Envie `dados/treino.jsonl` e `dados/validacao.jsonl` para a pasta **crivonews** do Google Drive.
   Esses dois arquivos **não** são versionados; em outro computador, gere-os com:

   ```bash
   git clone https://github.com/gabriel99557/fakeNews.git
   cd fakeNews
   git checkout feature/autenticacao-anotacoes
   cd ia-treinamento
   python 3_montar_dataset.py
   ```

3. Abra o notebook no Colab com **GPU T4** e execute as células em ordem (passo 4 do README).
4. Baixe `crivo.Q4_K_M.gguf`, importe no Ollama como `meu-llama-tcc` (passo 5) e rode a avaliação (passo 6).

- Precisa só de **Python 3** (os scripts usam apenas a biblioteca padrão, sem `pip install`).
- **Não rode os passos 1 e 2 de novo**: `dados/base.jsonl` e `dados/anotacoes.jsonl` estão completos no repositório.
  Regravar o passo 1 poderia misturar notícias do teste com as do treino.

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

- **Revisar os 35 exemplos de `inconclusivos_manual.jsonl`** (escritos com auxílio de IA) antes do treino.
- A probabilidade gerada para as notícias falsas ficou concentrada em 78–82; aceitável, já que o veredito é o principal.
- O modelo treinado deve ser importado no Ollama com o nome exato `meu-llama-tcc` (fixo no `AnaliseTextoService`).
- Hardware do PC original: GPU AMD RX 6650 XT (sem CUDA), por isso o treino é no Colab. Se o outro computador tiver
  GPU NVIDIA com 12 GB+ de VRAM, dá para treinar localmente com o mesmo notebook (pule as células do Drive).
