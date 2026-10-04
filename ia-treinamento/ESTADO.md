# Estado do treinamento — atualizado em 04/10/2026

Anotações para continuar o trabalho em outro computador. O passo a passo completo está no [README](README.md).

## Onde paramos

| Passo | Situação |
|---|---|
| 1. Preparar corpus | ✅ Feito. `dados/base.jsonl` versionado (7.200 notícias: treino 5.762, validação 720, teste 718) |
| 2. Justificativas (Groq) | ⏳ **782 de 1.200** em `dados/anotacoes.jsonl` (0 falhas). Falta 1 rodada |
| 3. Montar dataset | Roda em segundos depois do passo 2 |
| 4. Treinar no Colab | Notebook já está no Google Drive (ver abaixo) |
| 5. Ollama + avaliação | Depois do treino |

## Próximo passo: dia 3

No outro computador:

```bash
git clone https://github.com/gabriel99557/fakeNews.git
cd fakeNews
git checkout feature/autenticacao-anotacoes
cd ia-treinamento
python 2_gerar_justificativas.py --total 1200
```

- Precisa só de **Python 3** (os scripts usam apenas a biblioteca padrão, sem `pip install`).
- A chave da Groq é lida de `fakeNews/application.properties` (versionado nesta branch) ou da variável `GROQ_API_KEY`.
- **Não rode o passo 1 de novo**: `dados/base.jsonl` já está no repositório e as anotações dependem dessa divisão.
  Regravá-lo poderia misturar notícias do teste com as do treino.

Depois que chegar a 1.200:

```bash
python 3_montar_dataset.py
```

Isso gera `dados/treino.jsonl` e `dados/validacao.jsonl`. Envie os dois para a pasta **crivonews** do Google Drive e
rode o notebook (passo 4 do README). Lembre de commitar o `dados/anotacoes.jsonl` atualizado.

## Limite da Groq (importante)

- Plano gratuito do `openai/gpt-oss-120b`: **200.000 tokens por dia** (TPD). Cada notícia gasta ~530 tokens → **~376 por rodada**.
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
