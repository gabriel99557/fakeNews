"""
Passo 6 — Análise complementar dos resultados do passo 5 (capítulo de Resultados do TFG).

Lê resultados/<modelo>.jsonl e gera resultados/analise.md com:
  - acurácia com intervalo de confiança de 95% (Wilson);
  - acurácia por categoria do Fake.Br;
  - distribuição da probabilidadeFake por classe esperada;
  - teste de McNemar (exato, bilateral) entre pares de modelos: as diferenças de acerto
    nas MESMAS notícias são maiores do que o acaso explicaria?

Uso:
    python 6_analisar_resultados.py --modelo ollama:llama3.1:8b --modelo ollama:meu-llama-tcc --modelo groq:openai/gpt-oss-120b
"""

import argparse
import itertools
import math
import re
import statistics

from comum import ARQUIVO_BASE, PASTA_RESULTADOS, extrair_json, ler_jsonl

NOMES_CATEGORIA = {"politica": "Política", "tv_celebridades": "TV e celebridades",
                   "sociedade_cotidiano": "Sociedade e cotidiano", "ciencia_tecnologia": "Ciência e tecnologia",
                   "religiao": "Religião", "economia": "Economia"}


def arquivo(alvo: str):
    return PASTA_RESULTADOS / (re.sub(r"[^\w.-]+", "_", alvo) + ".jsonl")


def wilson(acertos: int, n: int, z: float = 1.96) -> tuple:
    if n == 0:
        return 0.0, 0.0
    p = acertos / n
    centro = (p + z * z / (2 * n)) / (1 + z * z / n)
    margem = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / (1 + z * z / n)
    return centro - margem, centro + margem


def mcnemar_exato(b: int, c: int) -> float:
    """Valor-p bilateral do teste de McNemar exato (binomial com p = 0,5 nos pares discordantes)."""
    n = b + c
    if n == 0:
        return 1.0
    k = min(b, c)
    cauda = sum(math.comb(n, i) for i in range(k + 1)) / 2 ** n
    return min(1.0, 2 * cauda)


def probabilidade(r: dict):
    try:
        valor = extrair_json(r["resposta"]).get("probabilidadeFake")
        return max(0, min(100, int(valor)))
    except (ValueError, TypeError, AttributeError):
        return None


def fmt(x: float) -> str:
    return f"{x:.1%}".replace(".", ",")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--modelo", action="append", required=True)
    args = ap.parse_args()

    base = {r["id"]: r for r in ler_jsonl(ARQUIVO_BASE)}
    resultados = {alvo: {r["id"]: r for r in ler_jsonl(arquivo(alvo))} for alvo in args.modelo}
    ids = sorted(set.intersection(*(set(r) for r in resultados.values())))
    if not ids:
        raise SystemExit("Sem notícias avaliadas em comum. Rode antes o 5_avaliar.py.")

    def acertou(alvo, i):
        r = resultados[alvo][i]
        return r["previsto"] == r["esperado"]

    linhas = [f"# Análise complementar ({len(ids)} notícias do teste avaliadas por todos os modelos)", ""]

    linhas += ["## Acurácia com intervalo de confiança de 95% (Wilson)", "",
               "| Modelo | Acertos | Acurácia | IC 95% |", "|---|---|---|---|"]
    for alvo in args.modelo:
        a = sum(acertou(alvo, i) for i in ids)
        lo, hi = wilson(a, len(ids))
        linhas.append(f"| {alvo} | {a}/{len(ids)} | {fmt(a / len(ids))} | {fmt(lo)} a {fmt(hi)} |")

    categorias = sorted({base[i]["categoria"] for i in ids}, key=lambda c: -sum(base[i]["categoria"] == c for i in ids))
    linhas += ["", "## Acurácia por categoria", "",
               "| Categoria | n | " + " | ".join(args.modelo) + " |", "|---|---|" + "---|" * len(args.modelo)]
    for cat in categorias:
        doc = [i for i in ids if base[i]["categoria"] == cat]
        valores = [fmt(sum(acertou(alvo, i) for i in doc) / len(doc)) for alvo in args.modelo]
        linhas.append(f"| {NOMES_CATEGORIA.get(cat, cat)} | {len(doc)} | " + " | ".join(valores) + " |")

    linhas += ["", "## probabilidadeFake por classe esperada (mediana e faixa entre os percentis 10 e 90)", "",
               "| Modelo | Notícias falsas | Notícias verdadeiras |", "|---|---|---|"]
    for alvo in args.modelo:
        celulas = []
        for esperado in ("FALSO", "VERDADEIRO"):
            ps = sorted(p for i in ids if resultados[alvo][i]["esperado"] == esperado
                        and (p := probabilidade(resultados[alvo][i])) is not None)
            if ps:
                p10, p90 = ps[int(0.1 * (len(ps) - 1))], ps[int(0.9 * (len(ps) - 1))]
                celulas.append(f"{statistics.median(ps):.0f} ({p10} a {p90})")
            else:
                celulas.append("sem dados")
        linhas.append(f"| {alvo} | " + " | ".join(celulas) + " |")

    linhas += ["", "## Teste de McNemar (exato) entre pares de modelos", "",
               "b = notícias que só o primeiro acertou; c = só o segundo acertou. "
               "p < 0,05 indica diferença de acerto estatisticamente significativa.", "",
               "| Comparação | b | c | valor-p | Diferença significativa (5%)? |", "|---|---|---|---|---|"]
    for m1, m2 in itertools.combinations(args.modelo, 2):
        b = sum(acertou(m1, i) and not acertou(m2, i) for i in ids)
        c = sum(acertou(m2, i) and not acertou(m1, i) for i in ids)
        p = mcnemar_exato(b, c)
        valor_p = "< 0,0001" if p < 0.0001 else f"{p:.4f}".replace(".", ",")
        linhas.append(f"| {m1} × {m2} | {b} | {c} | {valor_p} | {'sim' if p < 0.05 else 'não'} |")

    linhas += ["", "## Latência e ambiente de execução", "",
               "Notícias refeitas na CPU: a GPU AMD da máquina de avaliação às vezes degenerou a geração em \"@@@@\"; "
               "nesses casos a notícia foi processada de novo só na CPU, o que também eleva a latência dela.", "",
               "| Modelo | Latência mediana (s) | Latência média (s) | Refeitas na CPU |", "|---|---|---|---|"]
    for alvo in args.modelo:
        lat = [resultados[alvo][i]["latencia_s"] for i in ids]
        cpu = sum(bool(resultados[alvo][i].get("refeito_na_cpu")) for i in ids)
        mediana, media = (f"{v:.1f}".replace(".", ",") for v in (statistics.median(lat), statistics.mean(lat)))
        linhas.append(f"| {alvo} | {mediana} | {media} | {cpu} |")

    texto = "\n".join(linhas) + "\n"
    PASTA_RESULTADOS.mkdir(parents=True, exist_ok=True)
    (PASTA_RESULTADOS / "analise.md").write_text(texto, encoding="utf-8")
    print(texto + f"\nSalvo em {PASTA_RESULTADOS / 'analise.md'}")


if __name__ == "__main__":
    main()
