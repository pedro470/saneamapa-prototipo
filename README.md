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

- `Main.dc.html`: a cobertura dos requisitos, com o que cada tela atende, o que não cabe numa tela e os itens a definir que aparecem nas telas.
- `Morador.dc.html`: o morador aceita o termo de uso, registra um problema em três passos, com foto, vídeo ou áudio e a localização, se identifica (marcado por padrão) e recebe o protocolo. Os arquivos `M2` a `M9` abrem o mesmo fluxo num estado: vídeo recusado, GPS, posição à mão, identificação, aviso do anônimo, limite de relatos, enviado e guardado sem internet.
- `Fiscal.dc.html`: o fiscal entra, vê só os pontos atribuídos a ele e registra a vistoria, com foto só pela câmera e a até 100 m do ponto. Os arquivos `F2` a `F6` mostram a conta bloqueada, a lista, a vistoria, a recusa fora dos 100 m e o novo desfoque.
- `P1-Entrada.dc.html` a `P7-Auditoria.dc.html`: o painel da Prefeitura, com entrada, mapa e fila, indicadores, usuários, camadas de vulnerabilidade, Censo do IBGE e auditoria, ligados pelo menu lateral.

## Requisitos

`Documento de Requisitos - SaneaMapa v6.docx` traz os requisitos do sistema, no formato do modelo da disciplina: requisitos funcionais em tabela com os campos de cada cadastro, requisitos não funcionais e requisitos de segurança. Os perfis são Morador, Fiscal e Prefeitura, com os perfis internos da Prefeitura ainda a definir (TBD). O escopo segue a entrevista com o cliente; funções que nenhum pedido exige, como o ofício à concessionária, a rota do fiscal e a exportação, ficaram fora.

O protótipo segue a versão 6 dos requisitos. O que ainda depende do cliente aparece entre colchetes nas telas, como "[prazo a definir]" e "[perfil interno a definir]".

O documento é gerado a partir de `requisitos/requisitos.md`, que é a fonte da verdade: edite o md e rode `py -3 gerar_docx.py` dentro de `requisitos/` (com `--word`, o Word atualiza o sumário e exporta um PDF). O `modelo.docx` fornece a capa, o cabeçalho e os estilos, e `entrevista.md` traz as anotações da entrevista com o cliente.

Os dados são fictícios. Nada do que for digitado é enviado ou guardado.
