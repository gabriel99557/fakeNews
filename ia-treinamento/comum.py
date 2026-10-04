"""
Utilitários compartilhados pelos scripts de treinamento do modelo local.

O prompt NÃO é copiado à mão: ele é lido direto do AnaliseTextoService.java,
para que o formato usado no treino seja exatamente o que o backend envia ao
Ollama na hora da inferência (qualquer diferença degrada o modelo).
"""

import json
import os
import re
from pathlib import Path

PASTA = Path(__file__).resolve().parent
RAIZ_REPO = PASTA.parent
PASTA_DADOS = PASTA / "dados"
PASTA_RESULTADOS = PASTA / "resultados"

ARQUIVO_SERVICE = (RAIZ_REPO / "fakeNews" / "src" / "main" / "java" / "com" / "example"
                   / "fakeNews" / "service" / "AnaliseTextoService.java")

ARQUIVO_BASE = PASTA_DADOS / "base.jsonl"
ARQUIVO_ANOTACOES = PASTA_DADOS / "anotacoes.jsonl"

GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"
OLLAMA_URL = "http://localhost:11434/api/generate"

# Classes do corpus -> veredito esperado no JSON do CrivoNews
VEREDITO_POR_CLASSE = {"fake": "FALSO", "true": "VERDADEIRO"}


def _bloco_texto_java(conteudo_bruto: str) -> str:
    """Reproduz as regras de text block do Java (JEP 378): remove a indentação
    comum (contando a linha do fechamento), espaços no fim de cada linha e
    trata os escapes básicos."""
    linhas = conteudo_bruto.split("\n")[1:]  # a 1ª linha (logo após """) é descartada
    significativas = [l for l in linhas[:-1] if l.strip()] + [linhas[-1]]
    indent = min(len(l) - len(l.lstrip(" ")) for l in significativas)
    resultado = "\n".join(l[indent:].rstrip() for l in linhas)
    return (resultado.replace('\\"', '"').replace("\\n", "\n")
            .replace("\\t", "\t").replace("\\\\", "\\"))


def carregar_prompt_sistema() -> str:
    codigo = ARQUIVO_SERVICE.read_text(encoding="utf-8")
    m = re.search(r'PROMPT_SISTEMA\s*=\s*"""(.*?)""";', codigo, re.DOTALL)
    if not m:
        raise RuntimeError(f"PROMPT_SISTEMA não encontrado em {ARQUIVO_SERVICE}")
    return _bloco_texto_java(m.group(1))


PROMPT_SISTEMA = carregar_prompt_sistema()


def higienizar(texto: str) -> str:
    """Mesmo tratamento do AnaliseTextoService.higienizar()."""
    return re.sub(r"\s+", " ", texto).strip()


def prompt_local(texto: str) -> str:
    """Exatamente o 'prompt' que AnaliseTextoService.chamarIaLocal() envia ao Ollama."""
    return PROMPT_SISTEMA + "\n\nTexto para análise:\n" + higienizar(texto)


def _normalizar_hifens(texto: str) -> str:
    """O modelo professor às vezes usa hífen inseparável (U+2011) ou U+2010 em palavras como
    "trata-se"; troca pelo hífen comum para o modelo treinado não aprender esse artefato."""
    return texto.replace("‑", "-").replace("‐", "-").strip()


def resposta_canonica(veredito: str, probabilidade: int, justificativa: str, indicadores: list) -> str:
    """JSON de saída no formato e na ordem de campos do PROMPT_SISTEMA."""
    return json.dumps({
        "veredito": veredito,
        "probabilidadeFake": int(probabilidade),
        "justificativa": _normalizar_hifens(justificativa),
        "indicadores": [_normalizar_hifens(i) for i in indicadores],
    }, ensure_ascii=False)


def extrair_json(texto: str) -> dict:
    """Mesma tolerância do IaResponseParserService: aceita ```json ... ```
    e texto extra em volta, pegando do primeiro '{' ao último '}'."""
    limpo = texto.strip()
    if limpo.startswith("```"):
        limpo = re.sub(r"^```(json)?", "", limpo)
        limpo = re.sub(r"```$", "", limpo).strip()
    inicio, fim = limpo.find("{"), limpo.rfind("}")
    if inicio == -1 or fim < inicio:
        raise ValueError("Nenhum JSON encontrado na resposta.")
    return json.loads(limpo[inicio:fim + 1])


def normalizar_veredito(valor) -> str:
    v = str(valor or "").strip().upper()
    if v in ("VERDADEIRO", "TRUE", "REAL"):
        return "VERDADEIRO"
    if v in ("FALSO", "FALSE", "FAKE"):
        return "FALSO"
    return "INCONCLUSIVO"


def chave_groq() -> str:
    """Variável de ambiente GROQ_API_KEY ou, como o backend, os application.properties
    locais (ignorados ou não pelo git). A chave nunca é impressa."""
    if os.environ.get("GROQ_API_KEY"):
        return os.environ["GROQ_API_KEY"].strip()
    for arq in (RAIZ_REPO / "fakeNews" / "config" / "application.properties",
                RAIZ_REPO / "fakeNews" / "application.properties"):
        if arq.exists():
            for linha in arq.read_text(encoding="utf-8").splitlines():
                if linha.strip().startswith("spring.ai.openai.api-key="):
                    valor = linha.split("=", 1)[1].strip()
                    if valor and not valor.startswith("${") and valor != "COLE_A_CHAVE_AQUI":
                        return valor
    raise SystemExit("Chave da Groq não encontrada: defina GROQ_API_KEY ou "
                     "spring.ai.openai.api-key em fakeNews/application.properties.")


def ler_jsonl(caminho: Path) -> list:
    if not caminho.exists():
        return []
    with caminho.open(encoding="utf-8") as f:
        return [json.loads(l) for l in f if l.strip()]


def escrever_jsonl(caminho: Path, registros) -> None:
    caminho.parent.mkdir(parents=True, exist_ok=True)
    with caminho.open("w", encoding="utf-8", newline="\n") as f:
        for r in registros:
            f.write(json.dumps(r, ensure_ascii=False) + "\n")


def anexar_jsonl(caminho: Path, registro: dict) -> None:
    caminho.parent.mkdir(parents=True, exist_ok=True)
    with caminho.open("a", encoding="utf-8", newline="\n") as f:
        f.write(json.dumps(registro, ensure_ascii=False) + "\n")
        f.flush()
