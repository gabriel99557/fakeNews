# Treinamento do modelo local (Llama 3.1 8B + LoRA)

Gera o modelo `meu-llama-tcc` que o backend usa em `POST /api/analise/texto/local`.

> **Progresso atual e como continuar:** veja [ESTADO.md](ESTADO.md).

```
Fake.Br Corpus ──► 1_preparar_corpus ──► 2_gerar_justificativas (Groq) ──► 3_montar_dataset
                                                                                │
   Ollama ◄── Modelfile ◄── crivo.Q4_K_M.gguf ◄── 4_treinar_lora.ipynb (Colab/Kaggle, GPU)
     │
     └──► 5_avaliar (teste: modelo base × modelo treinado × Groq)
```

O treino roda na nuvem porque as bibliotecas de fine-tuning (Unsloth/bitsandbytes) exigem GPU NVIDIA;
o PC local só executa o modelo pronto. Os scripts dos passos 1, 2, 3 e 5 usam apenas a biblioteca
padrão do Python (sem `pip install`). Rode todos de dentro desta pasta.

## Garantia de consistência com o backend

O prompt de treino **não é copiado à mão**: `comum.py` lê o `PROMPT_SISTEMA` direto de
`AnaliseTextoService.java` e monta a entrada igual a `chamarIaLocal()` (prompt + `"\n\nTexto para análise:\n"` +
texto higienizado). Se o prompt mudar no Java, basta rodar de novo os passos 3 e 4.

## Passo a passo

### 1. Preparar o corpus

```bash
python 1_preparar_corpus.py
```

Baixa o [Fake.Br Corpus](https://github.com/roneysco/Fake.br-Corpus) (~38 MB; 3.600 notícias falsas e 3.600
verdadeiras) e gera `dados/base.jsonl` com divisão 80/10/10 (treino/validação/teste).

- Usa `size_normalized_texts`: na versão completa as notícias verdadeiras são bem mais longas, e o modelo
  aprenderia a usar o tamanho como atalho.
- A notícia falsa *N* e a verdadeira *N* tratam do mesmo assunto; a divisão é feita **por par** e
  estratificada por categoria, para que nenhum assunto do teste apareça no treino.

### 2. Gerar justificativas com a Groq

```bash
python 2_gerar_justificativas.py --total 20     # teste rápido
python 2_gerar_justificativas.py                # meta padrão: 3000 notícias
```

O corpus só tem o rótulo; o JSON do CrivoNews também precisa de `justificativa`, `indicadores` e
`probabilidadeFake`. A Groq recebe a notícia **e o rótulo correto** e escreve a explicação
(destilação com rótulo conhecido). Respostas que contradizem o rótulo, citam o "gabarito" ou fogem
do formato são descartadas e registradas em `dados/falhas_anotacao.jsonl`.

A chave vem de `GROQ_API_KEY` ou de `fakeNews/application.properties`, igual ao backend. O plano gratuito
tem limite diário de tokens: quando estoura, o script para sozinho, e é só rodar o mesmo comando depois
(ele retoma de onde parou). Limites da sua conta: <https://console.groq.com/settings/limits>.
Se o limite do `openai/gpt-oss-120b` estiver apertado, troque o professor com `--modelo`.

### 3. Montar o dataset

```bash
python 3_montar_dataset.py
```

Gera `dados/treino.jsonl` e `dados/validacao.jsonl`. Inclui os exemplos de `inconclusivos_manual.jsonl`,
porque o Fake.Br não tem casos INCONCLUSIVO. O arquivo tem 28 exemplos escritos para o projeto (com
auxílio de IA, revisados em 06/10/2026): opinião, boato sem confirmação, versões conflitantes,
previsões, alegações sem comprovação, estimativas divergentes e sátira declarada. Reportagens corretas sobre
assuntos ainda em andamento (acidente, investigação, estudo preliminar bem descrito) **não** entram aqui: no
Fake.Br elas seriam VERDADEIRO, e rotulá-las como INCONCLUSIVO ensinaria o modelo a duvidar de notícias reais. Não citam pessoas reais.
O tamanho dos textos foi aproximado ao do corpus (mediana ~750 vs. ~950 caracteres) para que o
modelo não aprenda "texto curto = inconclusivo". Para acrescentar mais, use o mesmo formato, um JSON por linha.

### 4. Treinar (Google Colab ou Kaggle)

1. Abra `4_treinar_lora.ipynb` no [Colab](https://colab.research.google.com) (*Arquivo → Fazer upload de notebook*)
   e escolha **GPU T4** — ou no [Kaggle](https://www.kaggle.com) com **GPU T4 x2/P100** (30 h grátis por semana, mais estável).
2. Execute as células em ordem e envie `treino.jsonl` e `validacao.jsonl` quando pedir.
3. Configuração: Llama 3.1 8B Instruct em 4 bits (QLoRA), LoRA `r=16, alpha=16` em todas as projeções,
   2 épocas, `lr=2e-4`, lote efetivo 8, erro calculado só sobre a resposta JSON.
4. No fim, baixe `crivo.Q4_K_M.gguf` (~4,9 GB), `historico_treino.csv` (curva de *loss*) e a pasta
   `crivo-lora/` (o adaptador, para guardar).

Se alguma célula falhar por mudança de versão das bibliotecas, compare com o notebook oficial do Unsloth
para Llama 3.1 (o fluxo é o mesmo).

### 5. Importar no Ollama

Salve o GGUF **fora do OneDrive** (para não sincronizar 5 GB), por exemplo `C:\modelos\crivo\`, copie
o `Modelfile` desta pasta para lá e rode nessa pasta:

```bash
ollama create meu-llama-tcc -f Modelfile
```

```bash
ollama run meu-llama-tcc "teste"
```

Depois, no frontend, escolha **"Llama 3 local"** e analise uma notícia. Se o Ollama não usar a GPU AMD,
ele roda na CPU: funciona, só mais devagar.

### 6. Avaliar (capítulo de resultados do TFG)

```bash
ollama pull llama3.1:8b
```

```bash
python 5_avaliar.py --modelo ollama:llama3.1:8b --modelo ollama:meu-llama-tcc --modelo groq:openai/gpt-oss-120b
```

Usa o conjunto de teste (nunca visto no treino; 200 notícias por padrão, `--limite 0` para as 720) e
chama cada modelo exatamente como o backend. Gera `resultados/resumo.md` com JSON válido (%), acurácia,
precisão/revocação/F1 da classe FALSO, F1 macro, % de INCONCLUSIVO, latência e matriz de confusão.
A comparação **base × treinado** é o que mostra o ganho do fine-tuning.

## Arquivos

| Arquivo | Versionado | Conteúdo |
|---|---|---|
| `comum.py` | sim | Prompt lido do Java, higienização, parser igual ao backend |
| `1_` a `5_` | sim | Os passos acima |
| `Modelfile` | sim | Importação no Ollama |
| `inconclusivos_manual.jsonl` | sim | Exemplos INCONCLUSIVO escritos à mão |
| `dados/`, `resultados/`, `*.gguf` | não | Gerados pelos scripts |
