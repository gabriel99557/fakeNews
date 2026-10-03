"""
Passo 2 — Usa a Groq como "professor" para escrever justificativa, indicadores e
probabilidade de cada notícia de treino/validação (destilação com rótulo conhecido).

O veredito vem SEMPRE do corpus (rotulado por humanos); a Groq só explica o porquê.
Respostas que contradizem o rótulo ou fogem do formato são descartadas.

Pode ser interrompido (Ctrl+C) e retomado: o que já foi anotado em dados/anotacoes.jsonl
é pulado. O plano gratuito da Groq tem limite diário de tokens; quando ele estoura,
o script para sozinho e é só rodar de novo depois.

Uso:
    python 2_gerar_justificativas.py                 # até 3000 notícias anotadas
    python 2_gerar_justificativas.py --total 200     # para testar com poucas
    python 2_gerar_justificativas.py --modelo qwen/qwen3-32b
"""

import argparse
import json
import random
import re
import time
import urllib.error
import urllib.request

from comum import (ARQUIVO_ANOTACOES, ARQUIVO_BASE, GROQ_URL, PASTA_DADOS, VEREDITO_POR_CLASSE,
                   anexar_jsonl, chave_groq, extrair_json, ler_jsonl)

ARQUIVO_FALHAS = PASTA_DADOS / "falhas_anotacao.jsonl"
SEMENTE = 42

PROMPT_PROFESSOR = """Você é um anotador especialista em checagem de fatos (fact-checking).
Você receberá uma notícia em português e o veredito CORRETO dela, definido por especialistas humanos.
Escreva a análise que um bom verificador faria dessa notícia, chegando a esse veredito.

Regras:
- "veredito": exatamente o veredito informado.
- "probabilidadeFake": inteiro calibrado pela força dos sinais no texto. Use toda a faixa:
    FALSO      -> 60-74 sinais sutis (texto bem escrito, poucos indícios);
                  75-89 sinais claros; 90-100 sinais gritantes (alarmismo, absurdos, apelo a compartilhar).
    VERDADEIRO -> 0-10 jornalismo exemplar (fontes nomeadas, dados, tom neutro);
                  11-25 bom, com alguma lacuna; 26-40 correto, mas com sinais que gerariam dúvida.
  Escolha um número específico (ex.: 63, 78, 94, 7, 22, 31); não use sempre o mesmo valor.
- "justificativa": até 3 frases curtas e didáticas, em português, baseadas em evidências
  observáveis NO TEXTO (tom, presença ou ausência de fontes, linguagem sensacionalista,
  apelo emocional, coerência, precisão de dados). Não diga que o veredito foi informado,
  não fale em rótulo, gabarito ou corpus, e não invente fatos que não estão no texto.
- "indicadores": de 2 a 4 indicadores linguísticos curtos (até 8 palavras cada), em português.
  Cada um descreve uma CARACTERÍSTICA do texto, não apenas copia um nome dele.
  Certo: "cita autoridade nomeada (delegado)", "linguagem alarmista", "sem fonte para os números".
  Errado: "coronel Edson Costa", "TV Anhanguera".

Responda SOMENTE com um JSON neste formato:
{"veredito": "...", "probabilidadeFake": 0, "justificativa": "...", "indicadores": ["...", "..."]}"""

TERMOS_PROIBIDOS = ("rótulo", "rotulo", "gabarito", "corpus", "veredito informado", "veredito correto",
                    "foi informado", "especialistas humanos")


class LimiteDiario(Exception):
    pass


def chamar_groq(chave: str, modelo: str, texto: str, veredito: str, cfg: dict) -> dict:
    corpo = {
        "model": modelo,
        "messages": [
            {"role": "system", "content": PROMPT_PROFESSOR},
            {"role": "user", "content": f"Veredito correto: {veredito}\n\nNotícia:\n{texto}"},
        ],
        "temperature": 0.4,
    }
    if cfg["json_mode"]:
        corpo["response_format"] = {"type": "json_object"}
    if "gpt-oss" in modelo:
        corpo["reasoning_effort"] = "low"  # economiza tokens do limite diário

    for tentativa in range(6):
        req = urllib.request.Request(
            GROQ_URL, data=json.dumps(corpo).encode("utf-8"), method="POST",
            headers={"Authorization": f"Bearer {chave}", "Content-Type": "application/json",
                     "User-Agent": "crivonews-treinamento"})
        try:
            with urllib.request.urlopen(req, timeout=120) as resp:
                cfg["restantes"] = resp.headers.get("x-ratelimit-remaining-requests")
                dados = json.load(resp)
                return extrair_json(dados["choices"][0]["message"]["content"] or "")
        except urllib.error.HTTPError as e:
            corpo_erro = e.read().decode("utf-8", errors="replace")
            if e.code == 429:
                espera = float(e.headers.get("retry-after") or 30)
                if espera > cfg["espera_maxima"]:
                    try:
                        motivo = json.loads(corpo_erro)["error"]["message"].split(". Please")[0]
                    except (ValueError, KeyError, TypeError):
                        motivo = "limite da Groq atingido"
                    raise LimiteDiario(f"{motivo} (liberação em ~{espera / 60:.0f} min)")
                print(f"    limite da Groq, aguardando {espera:.0f}s para recarregar...")
                time.sleep(espera + 1)
            elif e.code == 400 and "response_format" in corpo_erro and cfg["json_mode"]:
                print("    modelo não aceita response_format=json_object; seguindo sem ele.")
                cfg["json_mode"] = False
                corpo.pop("response_format", None)
            elif e.code == 400 and "json_validate_failed" in corpo_erro:
                raise ValueError("modelo gerou JSON inválido")
            elif e.code in (401, 403):
                raise SystemExit(f"Groq recusou a chave (HTTP {e.code}). Confira GROQ_API_KEY.")
            elif e.code >= 500:
                time.sleep(5 * (tentativa + 1))
            else:
                raise ValueError(f"HTTP {e.code}: {corpo_erro[:200]}")
        except (urllib.error.URLError, TimeoutError) as e:
            print(f"    falha de rede ({e}), tentando de novo...")
            time.sleep(5 * (tentativa + 1))
    raise ValueError("sem resposta da Groq após várias tentativas")


def limpar(texto) -> str:
    """Troca espaços Unicode exóticos (U+202F, U+00A0...) por espaço comum."""
    return re.sub(r"\s+", " ", str(texto)).strip()


def validar(resposta: dict, veredito: str) -> dict:
    if str(resposta.get("veredito", "")).strip().upper() != veredito:
        raise ValueError(f"veredito divergente: {resposta.get('veredito')}")
    prob = int(resposta.get("probabilidadeFake"))
    if not (60 <= prob <= 100 if veredito == "FALSO" else 0 <= prob <= 40):
        raise ValueError(f"probabilidade incoerente: {prob}")
    justificativa = limpar(resposta.get("justificativa", ""))
    if len(justificativa) < 20:
        raise ValueError("justificativa vazia ou curta demais")
    if any(t in justificativa.lower() for t in TERMOS_PROIBIDOS):
        raise ValueError("justificativa vaza o rótulo")
    indicadores = [limpar(i) for i in resposta.get("indicadores") or [] if limpar(i)]
    if not 2 <= len(indicadores) <= 5:
        raise ValueError(f"{len(indicadores)} indicadores")
    return {"veredito": veredito, "probabilidadeFake": prob,
            "justificativa": justificativa, "indicadores": indicadores[:4]}


def ordem_balanceada(base: list, splits: set) -> list:
    """Embaralha os pares e alterna fake/true, para que qualquer ponto de parada
    deixe o conjunto equilibrado entre as classes."""
    por_par = {}
    for r in base:
        if r["split"] in splits:
            por_par.setdefault(r["par"], []).append(r)
    pares = sorted(por_par)
    random.Random(SEMENTE).shuffle(pares)
    return [r for p in pares for r in sorted(por_par[p], key=lambda r: r["classe"])]


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--total", type=int, default=3000, help="quantas notícias anotadas ao todo (padrão 3000)")
    ap.add_argument("--modelo", default="openai/gpt-oss-120b", help="modelo da Groq usado como professor")
    ap.add_argument("--intervalo", type=float, default=2.5, help="segundos entre requisições")
    ap.add_argument("--espera-maxima", type=float, default=120,
                    help="acima disso (em s) o limite é tratado como diário e o script para")
    args = ap.parse_args()

    base = ler_jsonl(ARQUIVO_BASE)
    if not base:
        raise SystemExit("dados/base.jsonl não encontrado. Rode antes: python 1_preparar_corpus.py")

    feitos = {a["id"] for a in ler_jsonl(ARQUIVO_ANOTACOES)}
    pendentes = [r for r in ordem_balanceada(base, {"treino", "validacao"}) if r["id"] not in feitos]
    faltam = max(0, args.total - len(feitos))
    print(f"Já anotadas: {len(feitos)} | meta: {args.total} | nesta execução: até {faltam}")
    if not faltam:
        return

    chave = chave_groq()
    cfg = {"json_mode": True, "espera_maxima": args.espera_maxima, "restantes": None}
    novos = falhas = 0
    try:
        for r in pendentes:
            if novos >= faltam:
                break
            veredito = VEREDITO_POR_CLASSE[r["classe"]]
            try:
                resposta = None
                for _ in range(3):  # o professor às vezes erra o formato; tenta de novo
                    try:
                        resposta = validar(chamar_groq(chave, args.modelo, r["texto"], veredito, cfg), veredito)
                        break
                    except (ValueError, TypeError, json.JSONDecodeError) as e:
                        erro = e
                if resposta is None:
                    raise ValueError(str(erro))
            except ValueError as e:
                falhas += 1
                anexar_jsonl(ARQUIVO_FALHAS, {"id": r["id"], "erro": str(e)})
                print(f"  [falha] {r['id']}: {e}")
                continue

            anexar_jsonl(ARQUIVO_ANOTACOES, {"id": r["id"], "split": r["split"], "classe": r["classe"],
                                             "modelo_professor": args.modelo, "resposta": resposta})
            novos += 1
            if novos % 10 == 0 or novos == faltam:
                extra = f" | requisições restantes hoje: {cfg['restantes']}" if cfg["restantes"] else ""
                print(f"  {len(feitos) + novos}/{args.total} anotadas ({falhas} falhas){extra}")
            time.sleep(args.intervalo)
    except LimiteDiario as e:
        print(f"\nParando: {e}. Rode o mesmo comando mais tarde para continuar.")
    except KeyboardInterrupt:
        print("\nInterrompido. Rode o mesmo comando para continuar de onde parou.")

    print(f"\nNesta execução: {novos} novas, {falhas} falhas. Total anotado: {len(feitos) + novos}.")


if __name__ == "__main__":
    main()
