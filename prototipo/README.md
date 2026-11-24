# Protótipo navegável (canvas de design)

Estes arquivos são a fonte do canvas de design "SaneaMapa — protótipo v8": https://claude.ai/artifact/KkE3U81pwFxRimXYM589t2 (privado; o dono pode compartilhar pelo menu Share da página).

O canvas é o protótipo de telas que acompanha o Documento de Requisitos v8 (`requisitos/requisitos.md`). O produto em si é o app em `saneamapa-app/`.

## Pranchetas e requisitos

| Prancheta | O que mostra | Requisitos |
|---|---|---|
| `Main.dc.html` | Cobertura dos requisitos e os itens a definir resolvidos | todos |
| `Morador.dc.html` (+ `M2` a `M9`) | App do morador: termo de uso, tipo e evidências, local, nome e telefone, limite de relatos, protocolo e envio sem internet | RF001, RF002, RF003, RNF13, RNF17, RNF19 a RNF21, RS001, RS005 |
| `Fiscal.dc.html` (+ `F2` a `F6`) | App do fiscal: entrada, pontos atribuídos, vistoria com motivo do "não confirmado", fora dos 100 m e novo desfoque | RF003, RF005, RF007, RF018, RS002, RS006, RS008 |
| `P1` a `P7` | Painel da Prefeitura: entrada, mapa e fila, indicadores, usuários, camadas, Censo e auditoria | RF004, RF005, RF006, RF010, RF013, RF015, RF018 a RF020 |

As pranchetas `M*` e `F*` abrem o fluxo de `Morador.dc.html` e `Fiscal.dc.html` num estado específico.

## Como abrir

Os arquivos `.dc.html` só renderizam dentro do canvas, que fornece o `support.js` e o estilo em `ds/estilodopedro/`. Aberto direto no navegador, o arquivo mostra só a marcação. Para alterar o protótipo, edite o canvas e copie os arquivos de novo para esta pasta. `canvas.json` guarda a posição de cada prancheta.

Todos os dados das telas são fictícios.
