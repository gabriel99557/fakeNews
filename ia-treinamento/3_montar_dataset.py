"""
Passo 3 — Monta dados/treino.jsonl e dados/validacao.jsonl no formato de conversa
usado pelo notebook de treino:

    {"messages": [{"role": "user", "content": <prompt idêntico ao do backend>},
                  {"role": "assistant", "content": <JSON do veredito>}]}

Junta as anotações do passo 2 com os exemplos escritos à mão em
inconclusivos_manual.jsonl (o Fake.Br não tem casos INCONCLUSIVO).

Uso:
    python 3_montar_dataset.py
"""

import random
from collections import Counter

from comum import (ARQUIVO_ANOTACOES, ARQUIVO_BASE, PASTA, PASTA_DADOS, escrever_jsonl, ler_jsonl,
                   prompt_local, resposta_canonica)

ARQUIVO_MANUAL = PASTA / "inconclusivos_manual.jsonl"
SEMENTE = 42


def exemplo(texto: str, r: dict) -> dict:
    return {"messages": [
        {"role": "user", "content": prompt_local(texto)},
        {"role": "assistant", "content": resposta_canonica(
            r["veredito"], r["probabilidadeFake"], r["justificativa"], r["indicadores"])},
    ]}


def main():
    textos = {r["id"]: r["texto"] for r in ler_jsonl(ARQUIVO_BASE)}
    anotacoes = ler_jsonl(ARQUIVO_ANOTACOES)
    if not anotacoes:
        raise SystemExit("Nenhuma anotação em dados/anotacoes.jsonl. Rode antes: python 2_gerar_justificativas.py")

    conjuntos = {"treino": [], "validacao": []}
    vistos = set()
    for a in anotacoes:
        if a["id"] in vistos or a["split"] not in conjuntos or a["id"] not in textos:
            continue
        vistos.add(a["id"])
        conjuntos[a["split"]].append((a["resposta"]["veredito"], exemplo(textos[a["id"]], a["resposta"])))

    # Exemplos manuais: ~90% treino, ~10% validação (sorteio fixo)
    rng = random.Random(SEMENTE)
    for m in ler_jsonl(ARQUIVO_MANUAL):
        split = "validacao" if rng.random() < 0.1 else "treino"
        conjuntos[split].append((m["veredito"], exemplo(m["texto"], m)))

    for nome, itens in conjuntos.items():
        rng.shuffle(itens)
        escrever_jsonl(PASTA_DADOS / f"{nome}.jsonl", [ex for _, ex in itens])
        contagem = Counter(v for v, _ in itens)
        print(f"dados/{nome}.jsonl: {len(itens)} exemplos  "
              + "  ".join(f"{v}={contagem[v]}" for v in ("FALSO", "VERDADEIRO", "INCONCLUSIVO")))

    print("\nPróximo passo: enviar dados/treino.jsonl e dados/validacao.jsonl para o notebook 4_treinar_lora.ipynb")


if __name__ == "__main__":
    main()
