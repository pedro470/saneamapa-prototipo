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

- `2-app-morador.html`: o morador registra um problema em três passos, com foto, vídeo ou áudio e a localização, se identifica (marcado por padrão, com telefone validado), e recebe o protocolo.
- `3-app-fiscal.html`: o fiscal vê só os pontos atribuídos a ele e registra a vistoria; confirmar um ponto exige ao menos uma foto.
- `1-painel-secretaria.html`: a Prefeitura vê o mapa, os indicadores por status e por bairro e a fila de vistoria, atribui pontos a fiscais e compara as denúncias com o Censo do IBGE.

## Requisitos

`Documento de Requisitos - SaneaMapa v5.docx` traz os requisitos que este protótipo segue, redigidos conforme a ISO/IEC/IEEE 29148: perfis Morador, Fiscal e Prefeitura, com os perfis internos da Prefeitura ainda a definir (TBD). O escopo segue a entrevista com o cliente; funções que nenhum pedido exige, como o ofício à concessionária, a rota do fiscal e a exportação, ficaram fora desta versão. O documento é gerado a partir de `requisitos/requisitos.md`, fora deste repositório.

Os dados são fictícios. Nada do que for digitado é enviado ou guardado.
