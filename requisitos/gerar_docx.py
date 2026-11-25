"""Gera o Documento de Requisitos (.docx) a partir de requisitos.md.

Uso:
    py -3 gerar_docx.py            gera o .docx
    py -3 gerar_docx.py --word     gera, atualiza o sumário pelo Word e exporta um PDF de conferência

O requisitos.md é a fonte da verdade. O modelo.docx (versão 4) fornece capa,
cabeçalho, rodapé e estilos; o corpo do documento é refeito a cada geração.
"""
import html
import re
import struct
import subprocess
import sys
import zipfile
from pathlib import Path

PASTA = Path(__file__).resolve().parent
FONTE = PASTA / "requisitos.md"
MODELO = PASTA / "modelo.docx"

LARGURA_TABELA = 9322  # dxa, a mesma das tabelas do modelo
FONTE_TEMA = '<w:rFonts w:asciiTheme="minorHAnsi" w:hAnsiTheme="minorHAnsi" w:cs="Arial"/>'
NUM_MARCADOR = 900  # lista com marcadores criada em numbering.xml pelo gerador
CINZA = '<w:shd w:val="clear" w:color="auto" w:fill="D9D9D9"/>'
EMU_MAX_LARGURA = 5_600_000  # ~15,5 cm (largura útil na página A4)
EMU_MAX_ALTURA = 3_600_000   # 10,0 cm (evita telas verticais gigantes)


# ---------- texto e runs ----------

def esc(texto):
    return html.escape(texto, quote=False)


def esc_attr(texto):
    return html.escape(texto, quote=True)


def dimensoes_png(dados):
    if dados[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("Apenas imagens PNG são suportadas no momento.")
    return struct.unpack(">II", dados[16:24])


def registrar_imagem(caminho_rel, alt, imagens, base=PASTA):
    arquivo = (base / caminho_rel).resolve()
    if not arquivo.is_file():
        raise FileNotFoundError(f"Imagem não encontrada: {arquivo}")
    dados = arquivo.read_bytes()
    largura_px, altura_px = dimensoes_png(dados)
    escala = min(EMU_MAX_LARGURA / (largura_px * 9525), EMU_MAX_ALTURA / (altura_px * 9525))
    cx = int(round(largura_px * 9525 * escala))
    cy = int(round(altura_px * 9525 * escala))
    indice = len(imagens) + 1
    ext = arquivo.suffix.lstrip(".").lower()
    alvo = f"media/img{indice}.{ext}"
    imagens.append((indice, alvo, dados, ext))
    return indice, cx, cy, arquivo.name, alt


def paragrafo_imagem(rid, doc_id, nome, alt, cx, cy):
    alt_esc = esc_attr(alt or nome)
    nome_esc = esc_attr(nome)
    return (
        '<w:p><w:pPr><w:spacing w:before="40" w:after="140"/><w:jc w:val="center"/></w:pPr>'
        '<w:r><w:rPr><w:noProof/></w:rPr><w:drawing>'
        '<wp:inline distT="0" distB="0" distL="0" distR="0">'
        f'<wp:extent cx="{cx}" cy="{cy}"/>'
        '<wp:effectExtent l="0" t="0" r="0" b="0"/>'
        f'<wp:docPr id="{doc_id}" name="{nome_esc}" descr="{alt_esc}"/>'
        '<wp:cNvGraphicFramePr>'
        '<a:graphicFrameLocks xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" noChangeAspect="1"/>'
        '</wp:cNvGraphicFramePr>'
        '<a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">'
        '<a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">'
        '<pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">'
        f'<pic:nvPicPr><pic:cNvPr id="{doc_id}" name="{nome_esc}"/><pic:cNvPicPr/></pic:nvPicPr>'
        f'<pic:blipFill><a:blip r:embed="{rid}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>'
        f'<pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="{cx}" cy="{cy}"/></a:xfrm>'
        '<a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr>'
        '</pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>'
    )


def runs(texto, tamanho=None, negrito=False):
    """Converte texto com **negrito** e ^sobrescrito^ em runs do Word."""
    saida, em_negrito, sobrescrito = [], False, False
    for parte in re.split(r"(\*\*|\^)", texto):
        if parte == "**":
            em_negrito = not em_negrito
            continue
        if parte == "^":
            sobrescrito = not sobrescrito
            continue
        if not parte:
            continue
        props = FONTE_TEMA
        if negrito or em_negrito:
            props += "<w:b/>"
        if sobrescrito:
            props += '<w:vertAlign w:val="superscript"/>'
        if tamanho:
            props += f'<w:sz w:val="{tamanho}"/><w:szCs w:val="{tamanho}"/>'
        saida.append(f'<w:r><w:rPr>{props}</w:rPr><w:t xml:space="preserve">{esc(parte)}</w:t></w:r>')
    return "".join(saida)


def paragrafo(texto, estilo=None, alinhamento="both", tamanho=None, negrito=False, depois=None, marcador=False,
              junto=False):
    ppr = ""
    if estilo:
        ppr += f'<w:pStyle w:val="{estilo}"/>'
    if junto:
        ppr += "<w:keepNext/>"
    if marcador:
        ppr += f'<w:numPr><w:ilvl w:val="0"/><w:numId w:val="{NUM_MARCADOR}"/></w:numPr>'
    if depois is not None:
        ppr += f'<w:spacing w:before="0" w:after="{depois}"/>'
    if alinhamento:
        ppr += f'<w:jc w:val="{alinhamento}"/>'
    return f"<w:p><w:pPr>{ppr}</w:pPr>{runs(texto, tamanho, negrito)}</w:p>"


def quebra_de_pagina():
    return '<w:p><w:r><w:br w:type="page"/></w:r></w:p>'


# ---------- tabelas ----------

def tabela(linhas, percentuais):
    colunas = len(linhas[0])
    if not percentuais or len(percentuais) != colunas:
        percentuais = [100 / colunas] * colunas
    larguras = [int(LARGURA_TABELA * p / sum(percentuais)) for p in percentuais]
    larguras[-1] = LARGURA_TABELA - sum(larguras[:-1])
    # matriz (muitas colunas estreitas): fonte menor, margem menor e células centralizadas
    matriz = colunas > 8
    tamanho, margem = (14, 30) if matriz else (20, 80)

    bordas = "".join(
        f'<w:{lado} w:val="single" w:sz="4" w:space="0" w:color="auto"/>'
        for lado in ("top", "left", "bottom", "right", "insideH", "insideV")
    )
    xml = [
        "<w:tbl><w:tblPr>"
        f'<w:tblW w:w="{LARGURA_TABELA}" w:type="dxa"/><w:tblBorders>{bordas}</w:tblBorders>'
        '<w:tblLayout w:type="fixed"/>'
        f'<w:tblCellMar><w:left w:w="{margem}" w:type="dxa"/><w:right w:w="{margem}" w:type="dxa"/></w:tblCellMar>'
        '<w:tblLook w:val="04A0" w:firstRow="1" w:lastRow="0" w:firstColumn="1" w:lastColumn="0" w:noHBand="0" w:noVBand="1"/>'
        "</w:tblPr><w:tblGrid>"
        + "".join(f'<w:gridCol w:w="{w}"/>' for w in larguras)
        + "</w:tblGrid>"
    ]
    for n, linha in enumerate(linhas):
        cabecalho = n == 0
        trpr = "<w:trPr><w:cantSplit/>" + ("<w:tblHeader/>" if cabecalho else "") + "</w:trPr>"
        celulas = []
        for largura, texto in zip(larguras, linha):
            tcpr = f'<w:tcW w:w="{largura}" w:type="dxa"/>' + (CINZA if cabecalho else "")
            celulas.append(
                f"<w:tc><w:tcPr>{tcpr}</w:tcPr>"
                + paragrafo(texto, alinhamento="center" if cabecalho or matriz else "left",
                            tamanho=tamanho, negrito=cabecalho, depois=40)
                + "</w:tc>"
            )
        xml.append(f"<w:tr>{trpr}{''.join(celulas)}</w:tr>")
    xml.append("</w:tbl>")
    # parágrafo vazio depois da tabela, para separá-la do texto seguinte
    xml.append('<w:p><w:pPr><w:spacing w:before="0" w:after="0"/></w:pPr></w:p>')
    return "".join(xml)


def tabela_de_requisitos(requisitos):
    """Tabela ID | Descrição | Prioridade, com uma linha por requisito.

    Cada linha da descrição vira um parágrafo da célula; uma linha em branco
    abre espaço antes do parágrafo seguinte.
    """
    percentuais = [11, 75, 14]
    larguras = [int(LARGURA_TABELA * p / 100) for p in percentuais]
    larguras[-1] = LARGURA_TABELA - sum(larguras[:-1])
    bordas = "".join(
        f'<w:{lado} w:val="single" w:sz="4" w:space="0" w:color="auto"/>'
        for lado in ("top", "left", "bottom", "right", "insideH", "insideV")
    )
    xml = [
        "<w:tbl><w:tblPr>"
        f'<w:tblW w:w="{LARGURA_TABELA}" w:type="dxa"/><w:tblBorders>{bordas}</w:tblBorders>'
        '<w:tblLayout w:type="fixed"/>'
        '<w:tblCellMar><w:left w:w="80" w:type="dxa"/><w:right w:w="80" w:type="dxa"/></w:tblCellMar>'
        "</w:tblPr><w:tblGrid>"
        + "".join(f'<w:gridCol w:w="{w}"/>' for w in larguras)
        + "</w:tblGrid>"
    ]

    def celula(largura, paragrafos, cabecalho=False):
        tcpr = f'<w:tcW w:w="{largura}" w:type="dxa"/>' + (CINZA if cabecalho else "")
        return f"<w:tc><w:tcPr>{tcpr}</w:tcPr>{''.join(paragrafos)}</w:tc>"

    def p_celula(texto, antes=0, alinhamento="left", negrito=False, junto=False):
        return (
            f'<w:p><w:pPr>{"<w:keepNext/>" if junto else ""}'
            f'<w:spacing w:before="{antes}" w:after="20"/><w:jc w:val="{alinhamento}"/></w:pPr>'
            f"{runs(texto, 20, negrito)}</w:p>"
        )

    cabecalho = ["ID", "Descrição", "Prioridade"]
    xml.append(
        "<w:tr><w:trPr><w:cantSplit/><w:tblHeader/></w:trPr>"
        # o cabeçalho fica na página do primeiro requisito
        + "".join(celula(l, [p_celula(t, alinhamento="center", negrito=True, junto=True)], True)
                  for l, t in zip(larguras, cabecalho))
        + "</w:tr>"
    )
    for ident, prioridade, linhas in requisitos:
        paragrafos, antes = [], 0
        for linha in linhas:
            if not linha.strip():
                antes = 120
                continue
            paragrafos.append(p_celula(linha.strip(), antes))
            antes = 0
        xml.append(
            "<w:tr><w:trPr><w:cantSplit/></w:trPr>"
            + celula(larguras[0], [p_celula(ident, negrito=True)])
            + celula(larguras[1], paragrafos)
            + celula(larguras[2], [p_celula(prioridade, alinhamento="center")])
            + "</w:tr>"
        )
    xml.append("</w:tbl>")
    xml.append('<w:p><w:pPr><w:spacing w:before="0" w:after="0"/></w:pPr></w:p>')
    return "".join(xml)


def celulas(linha):
    return [c.strip() for c in linha.strip().strip("|").split("|")]


# ---------- markdown -> corpo ----------

def corpo_do_markdown(md, imagens=None, primeiro_rid=19):
    if imagens is None:
        imagens = []
    md = re.sub(r"<!--(?!\s*cols:).*?-->", "", md, flags=re.S)
    linhas = md.splitlines()
    xml, i, percentuais = [], 0, None
    while i < len(linhas):
        linha = linhas[i].rstrip()
        if not linha.strip():
            i += 1
            continue
        img = re.match(r"\s*!\[([^\]]*)\]\(([^)]+)\)\s*$", linha)
        if img:
            indice, cx, cy, nome, alt = registrar_imagem(img.group(2).strip(), img.group(1).strip(), imagens)
            rid = f"rId{primeiro_rid + indice - 1}"
            xml.append(paragrafo_imagem(rid, 1000 + indice, nome, alt, cx, cy))
            i += 1
            continue
        cols = re.match(r"\s*<!--\s*cols:\s*([\d,\s]+)-->", linha)
        if cols:
            percentuais = [float(x) for x in cols.group(1).split(",") if x.strip()]
            i += 1
            continue
        titulo = re.match(r"(#{1,3})\s+(.*)", linha)
        if titulo:
            nivel = len(titulo.group(1))
            xml.append(paragrafo(titulo.group(2), estilo=f"Ttulo{nivel}", alinhamento=None))
            i += 1
            continue
        # blocos "::: RFnnn | Prioridade" ... ":::" seguidos formam uma única tabela de requisitos
        if re.match(r":::\s*\S", linha):
            requisitos = []
            while i < len(linhas):
                abertura = re.match(r":::\s*([^|]+?)\s*\|\s*(.+?)\s*$", linhas[i])
                if not abertura:
                    if not linhas[i].strip():
                        i += 1
                        continue
                    break
                i += 1
                conteudo = []
                while linhas[i].strip() != ":::":
                    conteudo.append(linhas[i])
                    i += 1
                i += 1
                while conteudo and not conteudo[-1].strip():
                    conteudo.pop()
                requisitos.append((abertura.group(1), abertura.group(2), conteudo))
            xml.append(tabela_de_requisitos(requisitos))
            continue
        if linha.strip() == "[[quebra]]":
            xml.append(quebra_de_pagina())
            i += 1
            continue
        if linha.lstrip().startswith("|"):
            bloco = []
            while i < len(linhas) and linhas[i].lstrip().startswith("|"):
                bloco.append(celulas(linhas[i]))
                i += 1
            bloco = [b for b in bloco if not all(re.fullmatch(r":?-{3,}:?", c) for c in b)]
            xml.append(tabela(bloco, percentuais))
            percentuais = None
            continue
        if linha.lstrip().startswith("- "):
            xml.append(paragrafo(linha.lstrip()[2:], estilo="Commarcadores", marcador=True))
            i += 1
            continue
        # parágrafo: junta linhas seguidas até uma linha vazia ou outro bloco
        texto = [linha.strip()]
        i += 1
        while i < len(linhas) and linhas[i].strip() and not re.match(r"\s*(#|\||- |<!--|\[\[|:::|!\[)", linhas[i]):
            texto.append(linhas[i].strip())
            i += 1
        # parágrafo todo em negrito (título do requisito, rótulo "Campos:") fica na página do que vem depois
        texto = " ".join(texto)
        xml.append(paragrafo(texto, depois=120, junto=bool(re.fullmatch(r"\*\*[^*]+\*\*", texto))))
    return "".join(xml)


# ---------- montagem a partir do modelo ----------

def aceitar_revisoes(xml):
    xml = re.sub(r"<w:del\b[^>]*/>", "", xml)
    xml = re.sub(r"<w:del\b[^>]*>.*?</w:del>", "", xml, flags=re.S)
    xml = re.sub(r"<w:ins\b[^>]*/>", "", xml)
    xml = re.sub(r"</?w:ins\b[^>]*>", "", xml)
    return xml


def sumario():
    """Campo de sumário vazio; o Word o preenche ao atualizar os campos."""
    return (
        '<w:p><w:pPr><w:pStyle w:val="Sumrio1"/></w:pPr>'
        '<w:r><w:fldChar w:fldCharType="begin"/></w:r>'
        '<w:r><w:instrText xml:space="preserve"> TOC \\o "1-3" \\h \\z </w:instrText></w:r>'
        '<w:r><w:fldChar w:fldCharType="separate"/></w:r>'
        f'{runs("Atualize o sumário: clique aqui com o botão direito e escolha Atualizar campo.")}'
        '<w:r><w:fldChar w:fldCharType="end"/></w:r></w:p>'
    )


def montar(md):
    versao = re.search(r"<!--\s*versao:\s*(.*?)\s*-->", md)
    cliente = re.search(r"<!--\s*cliente:\s*(.*?)\s*-->", md)
    with zipfile.ZipFile(MODELO) as z:
        arquivos = {n: z.read(n) for n in z.namelist()}
    doc = aceitar_revisoes(arquivos["word/document.xml"].decode("utf8"))

    inicio_corpo = doc.index("<w:body>") + len("<w:body>")
    blocos = re.findall(r"<w:p[ >].*?</w:p>|<w:tbl>.*?</w:tbl>", doc[inicio_corpo:], flags=re.S)
    sect_final = re.search(r"<w:sectPr\b(?:(?!<w:sectPr).)*</w:sectPr>\s*</w:body>", doc, flags=re.S).group(0)

    capa = []
    for bloco in blocos:
        if 'w:val="Sumrio1"' in bloco:
            break
        if versao and len(capa) == 2:  # entre o nome do sistema e o da Secretaria
            capa.append(paragrafo(versao.group(1), estilo="versao", alinhamento=None))
        # a linha de cliente da capa do modelo vem do comentário "cliente:" do markdown
        if cliente and "Secretaria Municipal de Infraestrutura e Saneamento" in bloco:
            bloco = bloco.replace("Secretaria Municipal de Infraestrutura e Saneamento", esc(cliente.group(1)))
        capa.append(bloco)
    # o último parágrafo antes da Introdução traz o sectPr da seção do sumário
    inicio_texto = next(i for i, b in enumerate(blocos) if 'w:val="Ttulo1"' in b)
    fim_capa = next(b for b in reversed(blocos[:inicio_texto]) if "<w:sectPr" in b)

    rels = arquivos["word/_rels/document.xml.rels"].decode("utf8")
    rids = [int(x) for x in re.findall(r'Id="rId(\d+)"', rels)]
    primeiro_rid = max(rids) + 1 if rids else 1
    imagens = []

    novo = (
        doc[:inicio_corpo]
        + "".join(capa)
        + sumario()
        + fim_capa
        + corpo_do_markdown(md, imagens=imagens, primeiro_rid=primeiro_rid)
        + sect_final
        + "</w:document>"
    )
    arquivos["word/document.xml"] = novo.encode("utf8")

    if imagens:
        novas_rels = []
        extensoes = set()
        for indice, alvo, dados, ext in imagens:
            arquivos[f"word/{alvo}"] = dados
            rid = f"rId{primeiro_rid + indice - 1}"
            novas_rels.append(
                f'<Relationship Id="{rid}" '
                'Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" '
                f'Target="{alvo}"/>'
            )
            extensoes.add(ext)
        arquivos["word/_rels/document.xml.rels"] = rels.replace(
            "</Relationships>", "".join(novas_rels) + "</Relationships>"
        ).encode("utf8")

        tipos = arquivos["[Content_Types].xml"].decode("utf8")
        novos_tipos = []
        for ext in sorted(extensoes):
            if f'Extension="{ext}"' not in tipos:
                mime = "image/png" if ext == "png" else f"image/{ext}"
                novos_tipos.append(f'<Default Extension="{ext}" ContentType="{mime}"/>')
        if novos_tipos:
            arquivos["[Content_Types].xml"] = tipos.replace(
                "</Types>", "".join(novos_tipos) + "</Types>"
            ).encode("utf8")

    # lista com marcadores (o estilo de lista do modelo é numerado)
    numeracao = arquivos["word/numbering.xml"].decode("utf8")
    if f'w:numId="{NUM_MARCADOR}"' not in numeracao:
        abstrato = (
            f'<w:abstractNum w:abstractNumId="{NUM_MARCADOR}"><w:multiLevelType w:val="singleLevel"/>'
            '<w:lvl w:ilvl="0"><w:start w:val="1"/><w:numFmt w:val="bullet"/><w:lvlText w:val="•"/>'
            '<w:lvlJc w:val="left"/><w:pPr><w:ind w:left="397" w:hanging="227"/></w:pPr></w:lvl></w:abstractNum>'
        )
        numeracao = numeracao.replace("<w:num ", abstrato + "<w:num ", 1)
        numeracao = numeracao.replace(
            "</w:numbering>",
            f'<w:num w:numId="{NUM_MARCADOR}"><w:abstractNumId w:val="{NUM_MARCADOR}"/></w:num></w:numbering>',
        )
    arquivos["word/numbering.xml"] = numeracao.encode("utf8")

    # pede ao Word que atualize os campos (sumário) ao abrir
    settings = arquivos["word/settings.xml"].decode("utf8")
    if "w:updateFields" not in settings:
        settings = settings.replace("<w:zoom", '<w:updateFields w:val="true"/><w:zoom', 1)
    arquivos["word/settings.xml"] = settings.encode("utf8")
    return arquivos


def salvar(arquivos, destino):
    with zipfile.ZipFile(destino, "w", zipfile.ZIP_DEFLATED) as z:
        for nome, dados in arquivos.items():
            z.writestr(nome, dados)


def atualizar_no_word(docx, pdf):
    script = f"""
$w = New-Object -ComObject Word.Application
$w.Visible = $false
$d = $w.Documents.Open('{docx}')
$d.TablesOfContents | ForEach-Object {{ $_.Update() }}
$d.Fields.Update() | Out-Null
$d.Save()
$d.SaveAs2('{pdf}', 17)
$d.Close()
$w.Quit()
"""
    subprocess.run(["powershell", "-NoProfile", "-Command", script], check=True)


def main():
    md = FONTE.read_text(encoding="utf8")
    versao = re.search(r"<!--\s*versao:\s*Versão\s*(\d+)", md)
    numero = versao.group(1) if versao else "x"
    destino = PASTA.parent / f"Documento de Requisitos - SaneaMapa v{numero}.docx"
    salvar(montar(md), destino)
    print(f"gerado: {destino}")
    if "--word" in sys.argv:
        pdf = destino.with_suffix(".pdf")
        atualizar_no_word(str(destino), str(pdf))
        print(f"sumário atualizado e PDF exportado: {pdf}")


if __name__ == "__main__":
    main()
