"""
Passo 5 — Avalia os modelos no conjunto de TESTE (que nunca entrou no treino), chamando
cada um exatamente como o backend chama:
  - ollama:<modelo>  -> /api/generate com PROMPT_SISTEMA + "Texto para análise:" (chamarIaLocal)
  - groq:<modelo>    -> system=PROMPT_SISTEMA, user=texto (chamarIaNuvem)

Métricas: JSON válido (%), acurácia, precisão/revocação/F1 da classe FALSO (fake = positivo),
F1 macro, matriz de confusão, % de INCONCLUSIVO e latência. Resultados por notícia ficam em
resultados/<modelo>.jsonl (o script retoma de onde parou) e o resumo em resultados/resumo.md.

Uso:
    python 5_avaliar.py --modelo ollama:meu-llama-tcc --modelo ollama:llama3.1:8b --modelo groq:openai/gpt-oss-120b
    python 5_avaliar.py --modelo ollama:meu-llama-tcc --limite 100
"""

import argparse
import json
import re
import statistics
import time
import urllib.error
import urllib.request

from comum import (ARQUIVO_BASE, GROQ_URL, OLLAMA_URL, PASTA_RESULTADOS, PROMPT_SISTEMA, VEREDITO_POR_CLASSE,
                   anexar_jsonl, chave_groq, extrair_json, higienizar, ler_jsonl, normalizar_veredito,
                   prompt_local)


def _gerar_ollama(corpo: dict) -> str:
    req = urllib.request.Request(OLLAMA_URL, data=json.dumps(corpo).encode("utf-8"),
                                 headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(req, timeout=900) as resp:
        return json.load(resp).get("response") or ""


def chamar_ollama(modelo: str, texto: str) -> tuple:
    """Retorna (resposta, usou_cpu).

    Na máquina de avaliação (GPU AMD via Vulkan), o Ollama às vezes degenera a geração em "@@@@..."
    e aborta com HTTP 500 ("token repeat limit reached"). A mesma notícia processada só na CPU sai
    normal, então é um defeito do backend de GPU, não do modelo: nesse caso a notícia é refeita na CPU.
    """
    corpo = {"model": modelo, "prompt": prompt_local(texto), "stream": False}
    try:
        return _gerar_ollama(corpo), False
    except urllib.error.HTTPError as e:
        if e.code != 500:
            raise
        print(f"    GPU falhou ({e.read().decode('utf-8', 'replace')[:80]}); refazendo na CPU...")
    return _gerar_ollama({**corpo, "options": {"num_gpu": 0}}), True


def chamar_groq(modelo: str, texto: str, chave: str) -> str:
    corpo = {"model": modelo, "messages": [{"role": "system", "content": PROMPT_SISTEMA},
                                           {"role": "user", "content": higienizar(texto)}]}
    for _ in range(10):
        req = urllib.request.Request(GROQ_URL, data=json.dumps(corpo).encode("utf-8"), method="POST",
                                     headers={"Authorization": f"Bearer {chave}", "Content-Type": "application/json",
                                              "User-Agent": "crivonews-avaliacao"})
        try:
            with urllib.request.urlopen(req, timeout=120) as resp:
                return json.load(resp)["choices"][0]["message"]["content"] or ""
        except urllib.error.HTTPError as e:
            if e.code != 429:
                raise
            espera = float(e.headers.get("retry-after") or 30)
            if espera > 120:
                raise SystemExit(f"Limite diário da Groq atingido (~{espera / 60:.0f} min). Rode de novo depois.")
            time.sleep(espera + 1)
    raise RuntimeError("Groq continuou recusando por limite de taxa")


def avaliar_modelo(alvo: str, itens: list) -> list:
    tipo, _, modelo = alvo.partition(":")
    if tipo not in ("ollama", "groq") or not modelo:
        raise SystemExit(f"Modelo inválido: {alvo} (use ollama:<nome> ou groq:<nome>)")
    chave = chave_groq() if tipo == "groq" else None

    arquivo = PASTA_RESULTADOS / (re.sub(r"[^\w.-]+", "_", alvo) + ".jsonl")
    resultados = {r["id"]: r for r in ler_jsonl(arquivo)}
    pendentes = [i for i in itens if i["id"] not in resultados]
    print(f"\n== {alvo}: {len(itens) - len(pendentes)} já avaliadas, {len(pendentes)} pendentes")

    for n, item in enumerate(pendentes, 1):
        inicio = time.perf_counter()
        usou_cpu = False
        try:
            if tipo == "ollama":
                bruta, usou_cpu = chamar_ollama(modelo, item["texto"])
            else:
                bruta = chamar_groq(modelo, item["texto"], chave)
            erro = None
        except urllib.error.URLError as e:
            if tipo == "ollama" and getattr(e, "code", None) == 404:
                raise SystemExit(f"O Ollama não tem o modelo '{modelo}'. Crie com 'ollama create' ou baixe com 'ollama pull'.")
            if tipo == "ollama":
                raise SystemExit(f"Não consegui falar com o Ollama em {OLLAMA_URL}: {e}. Ele está aberto?")
            bruta, erro = "", str(e)
        latencia = time.perf_counter() - inicio

        try:
            dados = extrair_json(bruta)
            veredito, json_ok = normalizar_veredito(dados.get("veredito")), True
        except (ValueError, json.JSONDecodeError):
            veredito, json_ok = None, False

        r = {"id": item["id"], "esperado": VEREDITO_POR_CLASSE[item["classe"]], "previsto": veredito,
             "json_valido": json_ok, "latencia_s": round(latencia, 2), "resposta": bruta, "erro": erro,
             "refeito_na_cpu": usou_cpu}
        anexar_jsonl(arquivo, r)
        resultados[item["id"]] = r
        if n % 10 == 0 or n == len(pendentes):
            print(f"  {n}/{len(pendentes)} (última: {latencia:.1f}s)")

    return [resultados[i["id"]] for i in itens]


def metricas(resultados: list) -> dict:
    n = len(resultados)
    tp = sum(r["esperado"] == "FALSO" and r["previsto"] == "FALSO" for r in resultados)
    fp = sum(r["esperado"] == "VERDADEIRO" and r["previsto"] == "FALSO" for r in resultados)
    fn = sum(r["esperado"] == "FALSO" and r["previsto"] != "FALSO" for r in resultados)
    tn = sum(r["esperado"] == "VERDADEIRO" and r["previsto"] == "VERDADEIRO" for r in resultados)
    vp_ver = tn
    fp_ver = sum(r["esperado"] == "FALSO" and r["previsto"] == "VERDADEIRO" for r in resultados)
    fn_ver = sum(r["esperado"] == "VERDADEIRO" and r["previsto"] != "VERDADEIRO" for r in resultados)

    def f1(p, r):
        return 2 * p * r / (p + r) if p + r else 0.0

    prec = tp / (tp + fp) if tp + fp else 0.0
    rev = tp / (tp + fn) if tp + fn else 0.0
    prec_v = vp_ver / (vp_ver + fp_ver) if vp_ver + fp_ver else 0.0
    rev_v = vp_ver / (vp_ver + fn_ver) if vp_ver + fn_ver else 0.0
    latencias = [r["latencia_s"] for r in resultados]

    matriz = {(e, p): sum(r["esperado"] == e and (r["previsto"] or "SEM JSON") == p for r in resultados)
              for e in ("FALSO", "VERDADEIRO") for p in ("FALSO", "VERDADEIRO", "INCONCLUSIVO", "SEM JSON")}
    return {
        "n": n,
        "json_valido": sum(r["json_valido"] for r in resultados) / n,
        "acuracia": (tp + tn) / n,
        "precisao_fake": prec, "revocacao_fake": rev, "f1_fake": f1(prec, rev),
        "f1_macro": (f1(prec, rev) + f1(prec_v, rev_v)) / 2,
        "inconclusivo": sum(r["previsto"] == "INCONCLUSIVO" for r in resultados) / n,
        "latencia_media": statistics.mean(latencias), "latencia_mediana": statistics.median(latencias),
        "matriz": matriz,
    }


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--modelo", action="append", required=True, help="ollama:<nome> ou groq:<nome> (repita para vários)")
    ap.add_argument("--limite", type=int, default=200,
                    help="quantas notícias do teste usar (pares inteiros; padrão 200, 0 = todas)")
    args = ap.parse_args()

    teste = sorted((r for r in ler_jsonl(ARQUIVO_BASE) if r["split"] == "teste"), key=lambda r: (r["par"], r["classe"]))
    if not teste:
        raise SystemExit("dados/base.jsonl não encontrado. Rode antes: python 1_preparar_corpus.py")
    if args.limite:
        teste = teste[:args.limite - args.limite % 2]  # mantém o mesmo número de fake e true

    linhas = ["| Modelo | n | JSON válido | Acurácia | Precisão (fake) | Revocação (fake) | F1 (fake) "
              "| F1 macro | Inconclusivo | Latência média (s) |", "|" + "---|" * 10]
    matrizes = []
    for alvo in args.modelo:
        m = metricas(avaliar_modelo(alvo, teste))
        linhas.append(f"| {alvo} | {m['n']} | {m['json_valido']:.1%} | {m['acuracia']:.1%} | {m['precisao_fake']:.1%} "
                      f"| {m['revocacao_fake']:.1%} | {m['f1_fake']:.3f} | {m['f1_macro']:.3f} "
                      f"| {m['inconclusivo']:.1%} | {m['latencia_media']:.1f} |")
        mz = m["matriz"]
        matrizes += [f"\n**{alvo}** — matriz de confusão (linhas = esperado, colunas = previsto)\n",
                     "| | FALSO | VERDADEIRO | INCONCLUSIVO | SEM JSON |", "|---|---|---|---|---|"]
        for e in ("FALSO", "VERDADEIRO"):
            matrizes.append(f"| {e} | " + " | ".join(str(mz[(e, p)])
                                                     for p in ("FALSO", "VERDADEIRO", "INCONCLUSIVO", "SEM JSON")) + " |")

    resumo = "\n".join(["# Avaliação no conjunto de teste (Fake.Br, size-normalized)", "", *linhas, *matrizes, ""])
    PASTA_RESULTADOS.mkdir(parents=True, exist_ok=True)
    (PASTA_RESULTADOS / "resumo.md").write_text(resumo, encoding="utf-8")
    print("\n" + resumo + f"\nResumo salvo em {PASTA_RESULTADOS / 'resumo.md'}")


if __name__ == "__main__":
    main()
