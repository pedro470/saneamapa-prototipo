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

- `2-app-morador.html`: o morador registra um problema em três passos, com foto, vídeo ou áudio e a localização, se identifica (marcado por padrão, com telefone validado), recebe o protocolo e acompanha o relato por ele.
- `3-app-fiscal.html`: o fiscal vê só os pontos atribuídos a ele, segue a rota do dia e registra a vistoria; confirmar um ponto exige ao menos uma foto.
- `1-painel-secretaria.html`: a Prefeitura vê o mapa e a fila de vistoria, atribui pontos a fiscais, gera o ofício com os pontos confirmados e registra o envio. O que depende do contrato de concessão aparece marcado como "a validar".

## Requisitos

`Documento de Requisitos - SaneaMapa v5.docx` traz os requisitos que este protótipo segue, redigidos conforme a ISO/IEC/IEEE 29148: perfis Morador, Fiscal e Prefeitura, com os perfis internos da Prefeitura e o uso do contrato de concessão ainda a definir (TBD). O documento é gerado a partir de `requisitos/requisitos.md`, fora deste repositório.

Os dados são fictícios. Nada do que for digitado é enviado ou guardado.
