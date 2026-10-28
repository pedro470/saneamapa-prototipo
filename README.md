# SaneaMapa — protótipo

Protótipo navegável do SaneaMapa, Sistema de Registro e Notificação de Saneamento Básico, feito para validar o fluxo antes do desenvolvimento.

## Como abrir

1. Baixe o repositório (botão **Code › Download ZIP**) e descompacte.
2. Abra o `index.html` no navegador.

Se as telas aparecerem em branco ao abrir direto do disco, sirva a pasta localmente e abra http://localhost:8000:

```bash
python -m http.server 8000
```

## Telas

- `2-app-morador.html`: o morador registra um problema em três passos, com foto, vídeo ou áudio e a localização, se identifica (marcado por padrão) e recebe o protocolo.
- `3-app-fiscal.html`: o fiscal segue a rota do dia e registra a vistoria.
- `1-painel-secretaria.html`: a Secretaria vê o mapa e a fila por prioridade e gera o ofício à concessionária.

## Requisitos

`Documento de Requisitos - SaneaMapa v4.docx` traz a revisão feita a partir da ata da entrevista, em controle de alterações: multimídia, identificação por padrão, precisão do GPS e grupos de risco.

Os dados são fictícios. Nada do que for digitado é enviado ou guardado.
