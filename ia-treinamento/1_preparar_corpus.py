"""
Passo 1 — Baixa o Fake.Br Corpus e gera dados/base.jsonl com a divisão treino/validação/teste.

Decisões metodológicas (vale citar no TFG):
- Usa os textos de tamanho normalizado (size_normalized_texts): na versão completa as
  notícias verdadeiras são bem mais longas, e o modelo aprenderia a usar o tamanho como atalho.
- O corpus é pareado: a notícia falsa N e a verdadeira N tratam do mesmo assunto. A divisão é
  feita por PAR, para que o mesmo assunto nunca apareça ao mesmo tempo no treino e no teste.

Uso:
    python 1_preparar_corpus.py              # baixa o corpus do GitHub (~38 MB)
    python 1_preparar_corpus.py --zip X.zip  # usa um zip já baixado
"""

import argparse
import random
import urllib.request
import zipfile
from collections import Counter
from pathlib import Path

from comum import ARQUIVO_BASE, PASTA_DADOS, escrever_jsonl, higienizar

URL_ZIP = "https://github.com/roneysco/Fake.br-Corpus/archive/refs/heads/master.zip"
SEMENTE = 42


def baixar(destino: Path) -> Path:
    if destino.exists():
        print(f"Usando zip já baixado: {destino}")
        return destino
    print(f"Baixando {URL_ZIP} ...")
    destino.parent.mkdir(parents=True, exist_ok=True)
    urllib.request.urlretrieve(URL_ZIP, destino)
    return destino


def ler_corpus(caminho_zip: Path) -> dict:
    """Retorna {par: {"fake": texto, "true": texto, "categoria": str}}."""
    pares = {}
    with zipfile.ZipFile(caminho_zip) as z:
        for nome in z.namelist():
            partes = nome.split("/")
            # <raiz>/size_normalized_texts/{fake|true}/<n>.txt
            if len(partes) == 4 and partes[1] == "size_normalized_texts" and partes[3].endswith(".txt"):
                classe, par = partes[2], int(partes[3][:-4])
                texto = z.read(nome).decode("utf-8", errors="replace")
                pares.setdefault(par, {})[classe] = higienizar(texto)
            # <raiz>/full_texts/true-meta-information/<n>-meta.txt -> 3ª linha é a categoria
            elif len(partes) == 4 and partes[2] == "true-meta-information" and partes[3].endswith("-meta.txt"):
                par = int(partes[3].split("-")[0])
                linhas = z.read(nome).decode("utf-8", errors="replace").splitlines()
                pares.setdefault(par, {})["categoria"] = linhas[2].strip() if len(linhas) > 2 else ""
    return pares


def dividir(pares: list, proporcoes=(0.8, 0.1, 0.1)) -> dict:
    """Divisão estratificada por categoria, sempre mantendo os pares juntos."""
    rng = random.Random(SEMENTE)
    por_categoria = {}
    for par, dados in pares:
        por_categoria.setdefault(dados.get("categoria", ""), []).append(par)

    split_por_par = {}
    for cat in sorted(por_categoria):
        lista = sorted(por_categoria[cat])
        rng.shuffle(lista)
        n_treino = round(len(lista) * proporcoes[0])
        n_val = round(len(lista) * proporcoes[1])
        for i, par in enumerate(lista):
            split_por_par[par] = "treino" if i < n_treino else "validacao" if i < n_treino + n_val else "teste"
    return split_por_par


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--zip", type=Path, help="zip do Fake.Br já baixado")
    args = ap.parse_args()

    caminho_zip = args.zip or baixar(PASTA_DADOS / "Fake.br-Corpus.zip")
    pares = ler_corpus(caminho_zip)
    completos = [(p, d) for p, d in sorted(pares.items()) if d.get("fake") and d.get("true")]
    if not completos:
        raise SystemExit("Nenhum par fake/true encontrado no zip — estrutura inesperada.")

    split_por_par = dividir(completos)
    registros = []
    for par, dados in completos:
        for classe in ("fake", "true"):
            registros.append({
                "id": f"{classe}-{par}",
                "par": par,
                "classe": classe,
                "categoria": dados.get("categoria", ""),
                "split": split_por_par[par],
                "texto": dados[classe],
            })

    escrever_jsonl(ARQUIVO_BASE, registros)
    contagem = Counter((r["split"], r["classe"]) for r in registros)
    print(f"\n{len(registros)} notícias ({len(completos)} pares) salvas em {ARQUIVO_BASE}")
    for split in ("treino", "validacao", "teste"):
        print(f"  {split:<10} fake={contagem[(split, 'fake')]:>5}  true={contagem[(split, 'true')]:>5}")


if __name__ == "__main__":
    main()
