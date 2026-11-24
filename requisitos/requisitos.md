<!--
Fonte da verdade do Documento de Requisitos do SaneaMapa.
Edite este arquivo e gere o .docx com:  py -3 gerar_docx.py
Convenções de marcação aceitas pelo gerador:
  # / ## / ###           títulos de nível 1, 2 e 3 (numerados pelo modelo)
  parágrafo              texto corrido, justificado
  - item                 lista com marcadores
  | a | b |              tabela (a primeira linha é o cabeçalho)
  comentário "cols: 15,60,25" antes de uma tabela: largura das colunas em %
  **texto**              negrito
  ^texto^                sobrescrito (número da nota de um campo)
  ::: RFnnn | Prioridade início de um requisito; cada linha até ":::" vira um parágrafo da
  ...                    célula "Descrição". Requisitos seguidos formam uma única tabela
  :::                    ID | Descrição | Prioridade
  [[quebra]]             quebra de página
-->
<!-- versao: Versão 8 – novembro de 2026 -->
<!-- cliente: Demanda da comunidade ao CESMAC -->

# Introdução

Este documento especifica os requisitos do sistema SaneaMapa – Sistema de Registro e Notificação de Saneamento Básico, fornecendo aos desenvolvedores as informações necessárias para o projeto e a implementação, assim como para a realização dos testes e a homologação do sistema.

## Visão geral do documento

Além desta seção introdutória, as seções seguintes estão organizadas como descrito abaixo.

- **Seção 2 – Descrição geral do sistema:** apresenta a origem da demanda, o que a entrevista pediu e o que o analista acrescentou, e os perfis de usuário.
- **Seção 3 – Requisitos funcionais:** especifica os requisitos funcionais, agrupados por área funcional, numa tabela com o identificador, a descrição e a prioridade de cada requisito.
- **Seção 4 – Requisitos não funcionais:** especifica as qualidades e restrições do sistema como um todo, divididas em requisitos de produto, organizacionais e externos.
- **Seção 5 – Requisitos de segurança:** identifica os perigos ligados aos requisitos funcionais e as soluções adotadas para cada um.
- **Seção 6 – Matriz de avaliação de risco:** classifica os perigos da Seção 5 pela gravidade e pela probabilidade de ocorrência.
- **Seção 7 – Matriz de rastreabilidade:** registra a ligação entre os requisitos funcionais dependentes.
- **Seção 8 – Itens a definir:** lista as informações que a entrevista não levantou e como cada uma foi resolvida.
- **Seção 9 – Alterações em relação às versões anteriores:** registra o que mudou nesta versão e as inconsistências da versão 4, com a decisão adotada para cada uma.
- **Seção 10 – Referências:** apresenta os documentos utilizados na elaboração deste documento.
- **Seção 11 – Anotações da entrevista:** transcreve as anotações da entrevista com o cliente e as respostas dele às dúvidas levantadas depois, que são a fonte dos requisitos marcados como "Entrevista".
- **Seção 12 – Anexos:** apresenta o ciclo de vida do ponto, a matriz de permissões e as hipóteses que ficaram fora do escopo.

## Convenções, termos e abreviações

A correta interpretação deste documento exige o conhecimento de algumas convenções e termos específicos, que são descritos a seguir.

### Identificação dos requisitos

Cada requisito funcional tem um identificador único, no formato RFnnn, que não é reutilizado se o requisito for removido. A numeração RF001 a RF016 mantém a da versão 4, para preservar a rastreabilidade com o protótipo, e os requisitos criados depois começam em RF017. Os requisitos não funcionais usam o formato RNFnn; os requisitos de segurança, o formato RSnnn; e os itens a definir, o formato TBD-nn.

A referência a um requisito pode citar a subseção em que ele está descrito, no formato [subseção.identificador]. Por exemplo, [Registro de ocorrência.RF001] é o requisito RF001, descrito na subseção "Registro de ocorrência".

### Estrutura dos requisitos

Os requisitos funcionais são apresentados em tabelas com três colunas: o identificador, a descrição e a prioridade. A descrição de cada requisito traz, nesta ordem, os itens a seguir; os que não se aplicam ao requisito são omitidos.

- **Enunciado:** a frase principal do requisito, no imperativo, na forma "O sistema deve…".
- **Atores e pré-condições:** os perfis de usuário, ou o próprio sistema, que executam a função, e o que precisa ser verdade para que ela possa ser usada.
- **Campos:** os dados que a função recebe ou mantém, um por linha, no formato "Nome: Tipo; (máscara); obrigatoriedade". O número sobrescrito ao fim da linha remete à nota de mesmo número em "Regra e observações do preenchimento dos campos".
- **Regra e observações do preenchimento dos campos:** as notas numeradas, com as validações e o comportamento de cada campo.
- **Observações, Alteração, Exclusão e Pesquisa:** as regras do requisito que não se prendem a um campo, o que acontece quando um registro é alterado ou excluído e os critérios de busca.
- **Fonte:** a origem de cada parte do requisito. "Entrevista" indica um pedido registrado nas anotações da entrevista com o cliente ou nas respostas dele às dúvidas (Seção 11). "Legislação (LGPD)" indica a Lei Geral de Proteção de Dados (Lei nº 13.709/2018). "Derivado" indica o que não foi pedido literalmente, mas sem o qual um pedido da entrevista não funciona ou o sistema não opera. "Analista" indica hipótese do analista, uma escolha entre alternativas possíveis que ainda precisa ser validada com o cliente.

Os requisitos não funcionais são apresentados numa tabela com o identificador, a categoria, a descrição, a prioridade e a fonte. A categoria segue a divisão em requisitos de produto (como o sistema se comporta: facilidade de uso, desempenho, confiabilidade, portabilidade, espaço e segurança), organizacionais (decorrentes de quem desenvolve e mantém o sistema) e externos (decorrentes de leis e normas). Cada descrição traz um valor verificável.

Os requisitos de segurança são apresentados numa tabela com o identificador, o perigo e as soluções. O perigo indica o requisito funcional em que foi identificado, e as soluções remetem aos requisitos que o tratam.

Os tipos de campo dos requisitos funcionais são:

- **String(n):** texto com até n caracteres.
- **Integer:** número inteiro. Também é usado nos campos de seleção, que guardam o código da opção escolhida.
- **Decimal:** número com casas decimais.
- **Date:** data, no formato dd/mm/aaaa.
- **DateTime:** data e hora, no formato dd/mm/aaaa hh:mm.
- **Boolean:** sim ou não.
- **Arquivo:** foto, vídeo ou áudio.

Os verbos "deve" e "não deve" indicam, respectivamente, obrigação e proibição.

### Itens a definir

Quando uma informação necessária não foi obtida, o requisito indica a lacuna com a marca [TBD-nn], e a Seção 8 registra o que falta, quem responde e como a lacuna será resolvida. Um requisito com TBD está especificado em tudo, exceto no item marcado.

### Prioridades dos requisitos

Cada requisito recebe uma única prioridade, que vale para o requisito inteiro: se uma parte do requisito é essencial, todo o requisito é essencial. As prioridades são as seguintes.

- **Essencial** é o requisito sem o qual o sistema não entra em funcionamento. Requisitos essenciais são imprescindíveis e têm que ser implementados impreterivelmente.
- **Importante** é o requisito sem o qual o sistema entra em funcionamento, mas de forma não satisfatória. Requisitos importantes devem ser implementados, mas, se não forem, o sistema poderá ser implantado e usado mesmo assim.
- **Desejável** é o requisito que não compromete as funcionalidades básicas do sistema, isto é, o sistema pode funcionar de forma satisfatória sem ele. Requisitos desejáveis podem ser deixados para versões posteriores, caso não haja tempo hábil para implementá-los na versão que está sendo especificada.

[[quebra]]

# Descrição geral do sistema

## Origem da demanda e escopo

**Origem da demanda.** O SaneaMapa responde a uma demanda de caráter social e educativo, sem fins lucrativos, apresentada como uma solicitação da comunidade ao CESMAC. O cliente não tem ligação com a prefeitura nem com qualquer secretaria municipal, e seus interesses ainda não estão bem definidos. Os requisitos se apoiam numa entrevista com esse cliente, registrada por dois integrantes do grupo, e nas respostas do cliente às dúvidas levantadas depois dela. As anotações e as respostas estão transcritas na Seção 11 e são a única fonte do que este documento atribui à "Entrevista".

**O que a entrevista pediu.** Segundo as anotações, o problema é que, "no papel", a rua está saneada, mas na prática não está. O objetivo é dar à própria comunidade o poder de questionar as áreas declaradas como saneadas. Para isso, o cliente pediu:

- um aplicativo gratuito, em que o morador denuncia com áudio, vídeo ou imagem e com a geolocalização, com precisão exata;
- denúncia sem anonimato, com a pessoa se identificando;
- transcrição dos áudios por inteligência artificial;
- uma tela, ou painel, em que a prefeitura acompanha o status das denúncias e a quantidade de denúncias por localização;
- a validação da denúncia, feita por um fiscal da prefeitura em vistoria no local, como o cliente confirmou depois da entrevista;
- prioridade para grupos e situações de risco;
- integração com o Censo do IBGE.

O cliente comentou que já existem fontes alternativas, mas difíceis de usar e pouco ágeis. Ele espera que o morador tenha celular com acesso à internet. As duas anotações divergiam quanto ao uso sem conexão, e o cliente confirmou depois que, sem internet, o aplicativo guarda a denúncia e a envia quando a conexão voltar.

**O que o analista acrescentou.** Além dos pedidos, o documento traz dois tipos de requisito. Os "derivados" são consequência natural de um pedido: sem eles, o pedido não funciona. Por exemplo, para a prefeitura ver as denúncias, alguém precisa ter acesso ao sistema, e esse acesso precisa de cadastro e autenticação. As "hipóteses do analista" são escolhas entre alternativas possíveis, ainda não validadas com o cliente. As principais são:

- os quatro tipos de ocorrência (casa sem ligação de água, água fora do padrão, rua sem rede de esgoto e esgoto a céu aberto);
- a fila de vistoria, ordenada por urgência, com a atribuição de cada ponto a um fiscal.

As funções que nenhum pedido exige ficaram fora desta versão e estão listadas na seção 12.3.

**Sistemas relacionados.** As informações a seguir são do levantamento do analista, não vieram da entrevista e não foram conferidas com o cliente. Na avaliação do analista, as bases públicas sobre saneamento não servem para apontar uma rua sem atendimento. O SNIS, substituído pelo SINISA, é alimentado uma vez por ano pelos próprios prestadores e divulga indicadores por município. O Censo do IBGE detalha o acesso até o setor censitário, mas só a cada dez anos e a partir da declaração do morador sobre a ligação à rede geral, que não distingue a rede disponível do serviço que de fato chega. A PNAD Contínua é amostral e não desce ao bairro. Essas limitações são compatíveis com o que a entrevista chamou de fontes difíceis de usar e pouco ágeis, e com a ideia de que a rua está saneada "no papel".

**Restrições.** A Lei Geral de Proteção de Dados se aplica ao sistema, porque ele trata nome, telefone, localização da casa, imagens e voz de moradores. A partir dela, o analista definiu que fotos e vídeos exibidos não mostram rosto nem placa de veículo, que os áudios ficam restritos à equipe interna e que a localização de uma casa não é exposta além do trecho de rua. Os limites de duração de vídeo e áudio (RF001) são hipóteses do analista para reduzir o consumo de dados e de espaço.

## Perfis de usuário

O sistema distingue três perfis de usuário.

- **Morador:** qualquer pessoa que registra uma ocorrência pelo aplicativo. Não tem senha. Toda denúncia é identificada, com nome e telefone, como pede a entrevista.
- **Prefeitura:** usuário da prefeitura que acompanha as denúncias no painel, distribui as vistorias e administra o sistema. Acessa o sistema com credencial individual. É um perfil único, com acesso a todas as funções internas, inclusive ao nome e ao telefone do morador, como o cliente confirmou depois da entrevista.
- **Fiscal:** fiscal da prefeitura que valida a denúncia em vistoria no local, como o cliente confirmou depois da entrevista. Acessa o sistema com credencial individual.

Fiscal e Prefeitura formam a equipe interna. A matriz da seção 12.2 mostra o que cada perfil pode fazer. Os papéis "coordenador da fiscalização", "gestão" e "administrador", usados na versão 4, foram retirados por não terem fonte na entrevista.

[[quebra]]

# Requisitos funcionais

## Registro de ocorrência

::: RF001 | Essencial
O sistema deve permitir que o morador registre pelo celular uma ocorrência de saneamento, sem cadastro prévio nem autenticação.
**Atores:** morador.

**Cadastro de Relato:**
Protocolo: String(20); gerado automaticamente ^1^
Data e hora do registro: DateTime; (dd/mm/aaaa hh:mm); gerado automaticamente
Tipo de ocorrência: Integer; obrigatório ^2^
Evidências: Arquivo; obrigatório ^3^
Localização: conforme RF002; obrigatório
Descrição: String(300); opcional ^4^
Nome do morador: String(120); obrigatório ^5^
Telefone do morador: String(15); ((xx) xxxx-xxxx ou (xx) xxxxx-xxxx); obrigatório ^5, 6^
Situação do envio: Integer; gerado automaticamente ^7^

**Regra e observações do preenchimento dos campos:**
^1^ O protocolo é gerado no aparelho e exibido ao morador ao concluir o registro, com a data e a hora.
^2^ Seleção única entre "casa sem ligação de água", "água fora do padrão", "rua sem rede de esgoto" e "esgoto a céu aberto". Não há a opção "outro". Abaixo dos tipos, o aplicativo avisa que o SaneaMapa trata só de água e esgoto e orienta o morador a procurar a prefeitura em caso de poço contaminado, vazamento ou lixo.
^3^ Ao menos uma foto, vídeo ou áudio, em qualquer combinação, obtidos pela câmera, pelo microfone ou pela galeria do aparelho. O sistema deve recusar vídeo com mais de 30 segundos e áudio com mais de 60 segundos.
^4^ Texto livre. A transcrição dos áudios (RF003) é acrescentada à descrição.
^5^ Toda denúncia é identificada: o sistema não deve aceitar relato sem nome e telefone. Antes do envio, o sistema deve informar ao morador para que servem esses dados (contato na vistoria e controle de denúncias repetidas) e quem os vê (a equipe interna). O aplicativo preenche os dois campos com os valores do último relato enviado, e o morador pode alterá-los.
^6^ Número com DDD, de 10 ou 11 dígitos.
^7^ "Pendente" ou "enviado". Quando o envio falhar por falta de conexão, o sistema deve guardar o relato no aparelho, mostrar ao morador que o envio está pendente e reenviá-lo automaticamente quando a conexão voltar, sem ação do morador.

Obs1: nome e telefone do morador ficam restritos à equipe interna e nunca aparecem no mapa.
Obs2: o sistema deve aceitar no máximo 5 relatos por telefone a cada 24 horas (RS001).
Obs3: depois do envio, o aplicativo não guarda o relato nem mantém histórico dos relatos enviados; guarda apenas o nome e o telefone do morador, para preencher o próximo relato (RS005).

**Fonte:** Entrevista (evidências, localização, identificação obrigatória e envio sem conexão); Analista (tipos de ocorrência e orientação para problema fora do escopo, durações, descrição, limite de relatos e preenchimento com o último relato); Legislação (LGPD) (aviso da finalidade e ocultação no mapa); Derivado (demais regras).
:::

## Localização

::: RF002 | Essencial
O sistema deve capturar a localização exata do relato e permitir que o morador a ajuste num mapa.
**Atores:** morador.

**Localização do Relato:**
Latitude: Decimal; obrigatório ^1^
Longitude: Decimal; obrigatório ^1^
Precisão: Integer; em metros; gerado automaticamente ^1^
Logradouro: String(150); opcional ^2^
Bairro: String(80); opcional ^2^
Ponto de referência: String(100); opcional ^3^
Posição informada manualmente: Boolean; gerado automaticamente ^3^

**Regra e observações do preenchimento dos campos:**
^1^ Capturadas pelo GPS do aparelho durante o registro. O sistema deve aguardar até 30 segundos que a precisão alcance 15 metros e então exibir a posição num mapa, para que o morador a confirme ou arraste o marcador. A localização gravada é sempre a coordenada confirmada pelo morador.
^2^ Preenchidos por geocodificação reversa e editáveis pelo morador.
^3^ Quando a precisão não for alcançada em 30 segundos ou o GPS estiver indisponível, o morador indica o ponto no mapa ou informa um ponto de referência, e o sistema marca o relato como "posição informada manualmente".

Obs1: fora da equipe interna, a localização é exibida apenas como trecho de rua, sem número do imóvel nem coordenada.
Obs2: cada acesso à coordenada exata é registrado em log (RF019).

**Fonte:** Entrevista (geolocalização com precisão exata); Legislação (LGPD) (exibição por trecho de rua e log de acesso); Derivado (demais regras).
:::

## Tratamento das evidências

::: RF003 | Essencial
O sistema deve tratar as fotos, os vídeos e os áudios recebidos, de modo que nenhuma pessoa seja identificada, e transcrever os áudios por inteligência artificial.
**Atores:** sistema; fiscal; equipe interna.

**Cadastro de Evidência:**
Relato: Integer; (identificador do relato, RF001); obrigatório
Tipo de evidência: Integer; obrigatório ^1^
Origem: Integer; gerado automaticamente ^6^
Evidência divergente: Boolean; gerado automaticamente ^6^
Arquivo de uso: Arquivo; gerado automaticamente ^2, 3^
Data e hora de captura: DateTime; (dd/mm/aaaa hh:mm); gerado automaticamente ^4^
Latitude e longitude de captura: Decimal; gerado automaticamente ^4^
Transcrição: String(2000); gerado automaticamente ^5^

**Regra e observações do preenchimento dos campos:**
^1^ Seleção única entre "foto", "vídeo" e "áudio".
^2^ Ao receber uma foto, o sistema deve detectar rostos e placas de veículo, aplicar desfoque sobre eles e só então gravar o arquivo de uso. A imagem original é descartada. Nos vídeos, o desfoque é aplicado em todos os quadros e o som é removido.
^3^ Quando a detecção automática falhar, o fiscal pode marcar uma região da foto para receber novo desfoque. O sistema guarda apenas a versão vigente e descarta as anteriores.
^4^ Os metadados de fotos e vídeos são removidos, com exceção da data, da hora e da coordenada.
^5^ Somente para áudio. Cada áudio é transcrito automaticamente por inteligência artificial, e a transcrição é acrescentada à descrição do relato. Ao serviço de transcrição é enviado só o áudio, sem nome nem telefone, e o serviço deve apagá-lo após a transcrição (RS007).
^6^ A origem é "câmera", "microfone" ou "galeria". Quando a evidência vier da galeria e a sua data ou coordenada de captura diferir mais de 7 dias ou 500 metros das do relato, o sistema deve marcá-la como divergente, e a marcação é exibida ao fiscal (RS002).

Obs1: os áudios só podem ser reproduzidos pela equipe interna e nunca aparecem no mapa.

**Alteração:** a equipe interna pode corrigir a transcrição, e o fiscal pode marcar novo desfoque numa foto. Cada edição de foto é registrada com usuário, data e hora (RF019).

**Fonte:** Entrevista (transcrição por inteligência artificial); Legislação (LGPD) (desfoque, descarte do original, metadados, restrição dos áudios e registro das edições); Analista (marcação de evidência divergente); Derivado (novo desfoque, remoção do som e correção da transcrição).
:::

## Agrupamento de relatos

::: RF004 | Essencial
O sistema deve reunir num único ponto os relatos de um mesmo problema num mesmo local, de modo que a quantidade de relatos indique a gravidade do problema.
**Atores:** sistema.

**Cadastro de Ponto:**
Identificador: Integer; gerado automaticamente
Tipo de ocorrência: Integer; herdado do primeiro relato ^1^
Latitude e longitude consolidadas: Decimal; herdadas do primeiro relato ^1^
Quantidade de relatos: Integer; gerado automaticamente ^2^
Data do primeiro relato: Date; (dd/mm/aaaa); gerado automaticamente
Data do último relato: Date; (dd/mm/aaaa); gerado automaticamente
Status: Integer; gerado automaticamente ^3^
Reincidência: Boolean; gerado automaticamente ^4^

**Regra e observações do preenchimento dos campos:**
^1^ Corrigíveis pelo fiscal na vistoria (RF007).
^2^ Incrementada a cada relato vinculado ao ponto.
^3^ "Aguardando vistoria", "em vistoria", "confirmado" ou "não confirmado", conforme o ciclo de vida da seção 12.1.
^4^ Marcada quando um relato é vinculado a um ponto "confirmado", sem alterar o status do ponto e sem exigir nova vistoria. O relato soma à quantidade de relatos do ponto.

**Agrupamento:** ao receber um relato, o sistema deve vinculá-lo ao ponto mais próximo do mesmo tipo situado a até 50 metros. As evidências, a descrição e a data de cada relato são preservadas. Quando não houver ponto do mesmo tipo nesse raio, o sistema deve criar um ponto com status "aguardando vistoria", com o tipo e a coordenada do relato. Um relato vinculado a um ponto "não confirmado" devolve o ponto ao status "aguardando vistoria".

**Fonte:** Derivado.
:::

## Fila de vistoria

::: RF005 | Essencial
O sistema deve organizar os pontos pendentes numa fila ordenada por urgência e permitir que a Prefeitura distribua as vistorias aos fiscais, que validam a denúncia no local (RF007).
**Atores:** Prefeitura; fiscal; sistema.
**Pré-condição:** usuário autenticado (RF018).

**Item da Fila:**
Ponto: Integer; (identificador do ponto, RF004); obrigatório
Data de entrada na fila: DateTime; (dd/mm/aaaa hh:mm); gerado automaticamente
Pontuação: Decimal; gerado automaticamente ^1^
Fiscal atribuído: Integer; (usuário ativo com perfil Fiscal, RF013); opcional ^2^

**Regra e observações do preenchimento dos campos:**
^1^ Soma de quatro fatores: 6 pontos por relato vinculado ao ponto, contando no máximo 10 relatos; 1,5 ponto por dia de espera desde o primeiro relato, sem limite; 15 pontos por unidade do peso de urgência do ponto (RF006), de 0 a 3; e 2 pontos por ponto na fila no mesmo bairro, contando no máximo 10 pontos. Os limites impedem que uma grande quantidade de relatos passe à frente dos pontos urgentes (RS001), e a espera sem limite impede que um ponto fique parado na fila (RS009). A pontuação é recalculada quando um relato é vinculado ao ponto, quando o nível de urgência muda e uma vez por dia.
^2^ A atribuição a um fiscal muda o status do ponto para "em vistoria", e a retirada da atribuição o devolve para "aguardando vistoria".

Obs1: a fila contém todo ponto com status "aguardando vistoria" ou "em vistoria", ordenado da maior para a menor pontuação.
Obs2: o fiscal vê somente os pontos atribuídos a ele.

**Fonte:** Entrevista (validação por fiscal no local); Analista (atribuição a fiscais, visão restrita do fiscal e pesos da pontuação, adotados sem validação do cliente); Derivado (demais regras).
:::

## Classificação de urgência

::: RF006 | Importante
O sistema deve permitir o cadastro das camadas de sensibilidade social e classificar a urgência dos pontos situados nelas, para dar prioridade aos grupos e às situações de risco.
**Atores:** Prefeitura; sistema.
**Pré-condição:** usuário autenticado (RF018).

**Cadastro de Camada:**
Nome: String(120); obrigatório
Categoria: Integer; obrigatório ^1^
Geometria: ponto com raio de influência (Integer, em metros) ou polígono; obrigatório ^2^
Peso de urgência: Integer; (1 a 3); obrigatório ^3^
Fonte do dado: String(200); obrigatório
Data de atualização: Date; (dd/mm/aaaa); obrigatório
Ativa: Boolean; obrigatório

**Regra e observações do preenchimento dos campos:**
^1^ Seleção única entre "público infantil e escolar", "idosos e cuidados", "gestantes e recém-nascidos", "comunidades tradicionais e vulneráveis" e "vulnerabilidade epidemiológica".
^2^ As camadas descrevem equipamentos e territórios, nunca pessoas. Por isso, a categoria "vulnerabilidade epidemiológica" aceita somente polígono.
^3^ Preenchido com o peso sugerido para a categoria, que a Prefeitura pode alterar: 3 para "público infantil e escolar", "gestantes e recém-nascidos" e "vulnerabilidade epidemiológica", os grupos mais afetados pelas doenças de veiculação hídrica; 2 para "idosos e cuidados" e "comunidades tradicionais e vulneráveis".

**Classificação:** cada ponto recebe como peso de urgência o maior peso entre as camadas ativas cuja geometria contém a sua localização, ou 0 quando nenhuma o contém. O nível de urgência é "normal" para o peso 0, "elevado" para os pesos 1 e 2 e "crítico" para o peso 3, e aparece no detalhe do ponto, no mapa e na fila, com a camada que o motivou. O morador não vê o nível de urgência.

**Alteração:** quando uma camada é criada, alterada, ativada ou desativada, o sistema deve recalcular o nível de urgência dos pontos afetados.

**Fonte:** Entrevista (prioridade para grupos e situações de risco); Analista (categorias e pesos sugeridos, adotados sem validação do cliente); Legislação (LGPD) (camadas sem dados de pessoas e ocultação do nível ao morador); Derivado (demais regras).
:::

## Vistoria

::: RF007 | Essencial
O sistema deve permitir que o fiscal registre a vistoria em campo, que valida a denúncia e muda o status do ponto.
**Atores:** fiscal.
**Pré-condições:** usuário autenticado com o perfil Fiscal (RF018); ponto atribuído ao fiscal (RF005).

**Cadastro de Vistoria:**
Ponto: Integer; (identificador do ponto, RF004); obrigatório
Data e hora da vistoria: DateTime; (dd/mm/aaaa hh:mm); gerado automaticamente
Fiscal responsável: Integer; gerado automaticamente
Resultado: Integer; obrigatório ^1^
Motivo da não confirmação: Integer; obrigatório quando o resultado for "não confirmado" ^5^
Tipo de ocorrência: Integer; obrigatório ^2^
Latitude e longitude: Decimal; obrigatório ^2^
Fotos e vídeos da vistoria: Arquivo; obrigatório quando o resultado for "confirmado" ^3^
Latitude e longitude do fiscal: Decimal; gerado automaticamente ^4^

**Regra e observações do preenchimento dos campos:**
^1^ Seleção única entre "confirmado" e "não confirmado". Ao salvar, o status do ponto muda para o resultado registrado.
^2^ Preenchidos com os dados do ponto e corrigíveis pelo fiscal. A correção atualiza o ponto (RF004).
^3^ Capturados somente pela câmera do aparelho, nunca da galeria, e tratados conforme RF003 (RS008).
^4^ Capturadas pelo GPS no momento de salvar. O sistema não deve aceitar a vistoria se a posição do fiscal estiver a mais de 100 metros da localização do ponto (RS008).
^5^ Seleção única entre "problema não encontrado no local" e "problema existe, mas não é de saneamento". A segunda opção registra que o morador apontou um problema real fora do escopo do sistema, como poço contaminado, vazamento ou lixo, para que a Prefeitura o encaminhe por outros meios. O motivo fica no histórico do ponto e aparece no painel.

Obs1: ao abrir o ponto, o fiscal vê todas as evidências e todos os relatos vinculados.
Obs2: cada mudança de status fica no histórico do ponto, com usuário, data e hora (RF019).

**Fonte:** Entrevista (validação da denúncia por fiscal da prefeitura, no local); Analista (vistoria restrita aos pontos atribuídos, evidência obrigatória na confirmação, motivo da não confirmação, foto só pela câmera e posição do fiscal); Derivado (demais regras).
:::

## Mapa e painel

::: RF010 | Essencial
O sistema deve exibir à Prefeitura, numa tela com mapa, o status das denúncias e a quantidade de denúncias por localização.
**Atores:** Prefeitura.
**Pré-condição:** usuário autenticado (RF018).

**Camadas do mapa**, que o usuário liga e desliga:
Pontos: marcador por tipo de ocorrência, com a quantidade de relatos do ponto.
Calor: densidade de relatos.
Vulnerabilidade: camadas ativas do RF006.
Censo: proporção de domicílios ligados à rede geral (RF020).

**Detalhe do ponto:** ao selecionar um ponto, o sistema deve exibir suas evidências, seus relatos vinculados, seu histórico e seu nível de urgência.

**Pesquisa:** será permitido filtrar os pontos por tipo de ocorrência, status, bairro, período, quantidade de relatos e nível de urgência.

Obs1: quando um relato ou uma vistoria alterar um ponto, o mapa deve refletir a alteração em até 60 segundos, sem recarga manual, enquanto estiver aberto.

**Fonte:** Entrevista (status e quantidade de denúncias por localização numa tela); Derivado (detalhe do ponto e atualização automática).
:::

## Cadastro de usuários

::: RF013 | Essencial
O sistema deve permitir o cadastro dos usuários da equipe interna, para que cada acesso seja individual e o histórico identifique quem fez cada ação.
**Atores:** Prefeitura.
**Pré-condição:** usuário autenticado (RF018).

**Cadastro de Usuário:**
CPF: String(14); (xxx.xxx.xxx-xx); obrigatório ^1^
Nome completo: String(120); obrigatório
E-mail: String(200); obrigatório ^2^
Celular: String(15); ((xx) xxxxx-xxxx); obrigatório
Perfil: Integer; obrigatório ^3^
Ativo: Boolean; marcado por padrão ^4^

**Regra e observações do preenchimento dos campos:**
^1^ O CPF deve ter os dígitos verificadores validados, é único entre os usuários e não pode ser alterado depois do cadastro. Quando o CPF já estiver cadastrado, o sistema deve exibir "CPF já cadastrado" e não salvar.
^2^ Único entre os usuários. Quando o e-mail já estiver cadastrado, o sistema deve exibir "E-mail já cadastrado" e não salvar.
^3^ Seleção única entre Fiscal e Prefeitura.
^4^ Usuário desativado não consegue se autenticar (RF018).

**Alteração:** cada alteração de cadastro é registrada com o autor, os campos alterados, os valores anterior e novo, a data e a hora (RF019).

**Exclusão:** não será realizada a exclusão. O usuário é desativado pelo campo "Ativo", para que o histórico continue íntegro.

**Fonte:** Analista (formatos do CPF e do celular); Legislação (LGPD) (registro das alterações); Derivado (demais regras).
:::

::: RF021 | Essencial
O sistema deve criar o primeiro usuário interno na instalação, para que o cadastro de usuários (RF013) e a autenticação (RF018) possam ser usados, e impedir que a equipe interna fique sem nenhum usuário capaz de cadastrar outros.
**Atores:** sistema.
**Pré-condição:** nenhum usuário interno cadastrado.

**Primeiro Usuário:**
Dados do usuário: conforme o Cadastro de Usuário do RF013, com o perfil Prefeitura; obrigatório ^1^
Link de definição de senha: String(200); gerado automaticamente ^2^

**Regra e observações do preenchimento dos campos:**
^1^ Informados na configuração da instalação do sistema, fora das telas do painel, e validados com as mesmas regras do RF013. O perfil é sempre Prefeitura.
^2^ Enviado ao e-mail do primeiro usuário, válido por 24 horas e de uso único. O sistema não define nem exibe senha inicial; o usuário define a sua senha pelo link, conforme a política do RNF16.

Obs1: a criação ocorre uma única vez. Quando já houver um usuário interno cadastrado, a configuração da instalação é ignorada.
Obs2: o sistema não deve permitir desativar o último usuário ativo com o perfil Prefeitura (RF013).
Obs3: a criação do primeiro usuário é registrada no log de auditoria (RF019).

**Fonte:** Derivado.
:::

## Indicadores do painel

::: RF015 | Importante
O sistema deve exibir à Prefeitura, em números, o status das denúncias e a quantidade de denúncias por localização.
**Atores:** Prefeitura.
**Pré-condição:** usuário autenticado (RF018).

**Indicadores:**
Quantidade de pontos por status.
Quantidade de pontos por tipo de ocorrência.
Quantidade de pontos por nível de urgência (RF006).
Quantidade de pontos por bairro.

**Pesquisa:** será permitido filtrar todos os indicadores por período e por bairro.

**Fonte:** Entrevista (status e quantidade de denúncias por localização num painel); Derivado (filtros).
:::

## Autenticação

::: RF018 | Essencial
O sistema deve exigir a autenticação individual dos usuários dos perfis Fiscal e Prefeitura, para que o acesso a dados pessoais e as ações que alteram pontos sejam atribuídos a um usuário identificado.
**Atores:** fiscal; Prefeitura.

**Acesso:**
E-mail: String(200); obrigatório
Senha: String; obrigatório ^1^

**Regra e observações do preenchimento dos campos:**
^1^ A autenticação usa credencial própria do sistema, sem integração com login institucional. A senha segue a política do RNF16 e é armazenada criptografada.

Obs1: o sistema não deve autenticar usuário desativado (RF013).
Obs2: o usuário pode redefinir a senha por meio de mensagem enviada ao e-mail cadastrado e encerrar a própria sessão.

**Fonte:** Derivado.
:::

## Auditoria

::: RF019 | Essencial
O sistema deve registrar quem acessou e quem alterou cada dado pessoal, e quando, como exige a LGPD. O controlador desses dados é a prefeitura que operar o sistema (RNF19).
**Atores:** sistema; Prefeitura, na pesquisa.

**Registro de Log:**
Usuário: Integer; gerado automaticamente
Data e hora: DateTime; (dd/mm/aaaa hh:mm:ss); gerado automaticamente
Tipo de evento: Integer; gerado automaticamente ^1^
Objeto afetado: Integer; (identificador do relato, ponto, evidência ou usuário); gerado automaticamente

**Regra e observações do preenchimento dos campos:**
^1^ Acesso a nome, telefone, coordenada exata ou áudio de morador; edição de foto (RF003); atribuição de ponto a fiscal (RF005); mudança de status de ponto (RF007); alteração de cadastro de usuário (RF013); criação do primeiro usuário (RF021); importação de dados do Censo (RF020); e eliminação de nome e telefone ao fim do prazo de guarda (RNF20).

**Alteração e exclusão:** os registros de log não podem ser alterados nem excluídos.

**Pesquisa:** será permitido buscar registros por usuário, período e tipo de evento.

**Fonte:** Legislação (LGPD).
:::

## Censo do IBGE

::: RF020 | Importante
O sistema deve importar os dados do Censo do IBGE e exibi-los no mapa, para mostrar o que está saneado "no papel" e permitir que a comunidade questione essas áreas com as denúncias.
**Atores:** Prefeitura, na importação; sistema.
**Pré-condição:** usuário autenticado (RF018).

**Cadastro de Setor Censitário:**
Código do setor: String(15); obrigatório
Geometria: polígono; obrigatório
Domicílios ligados à rede geral de água: Decimal; (0 a 100, em %); obrigatório
Domicílios ligados à rede geral de esgoto: Decimal; (0 a 100, em %); obrigatório

Obs1: os setores são importados de arquivo CSV ou GeoJSON. Na importação, o sistema deve validar o formato e os códigos dos setores e manter a importação anterior, para permitir voltar a ela (RS013).
Obs2: o mapa oferece uma camada com a proporção de domicílios ligados à rede geral de água e de esgoto em cada setor (RF010), e o detalhe de cada ponto mostra a proporção do seu setor para o serviço correspondente ao tipo da ocorrência.

**Fonte:** Entrevista (integração com o Censo do IBGE); Analista (validação e versão anterior da importação); Derivado (proporção no detalhe do ponto).
:::

[[quebra]]

# Requisitos não funcionais

Os valores desta seção são hipóteses do analista, exceto quando a fonte indica a entrevista ou a legislação, e devem ser validados com o cliente.

## Requisitos de produto

<!-- cols: 9,19,46,13,13 -->
| ID | Requisito | Descrição | Prioridade | Fonte |
|---|---|---|---|---|
| RNF01 | Facilidade de uso | O aplicativo deve permitir concluir o registro de uma ocorrência em no máximo três telas. | Essencial | Derivado |
| RNF02 | Facilidade de uso | O aplicativo deve apresentar os tipos de ocorrência com ícone e texto curto, sem termos técnicos. | Importante | Analista |
| RNF03 | Facilidade de uso | Quando uma ação não puder ser concluída, o sistema deve exibir uma mensagem que diga o motivo e o que fazer. | Importante | Derivado |
| RNF04 | Acessibilidade | O aplicativo e o painel devem atender ao nível AA da WCAG 2.1. | Desejável | Analista |
| RNF05 | Portabilidade | O aplicativo deve funcionar no navegador do celular, no Chrome do Android 8.0 ou superior e no Safari do iOS 15 ou superior, sem instalação por loja de aplicativos. | Essencial | Analista |
| RNF06 | Portabilidade | O painel da Prefeitura deve funcionar nas duas últimas versões do Chrome, do Edge e do Firefox. | Essencial | Analista |
| RNF07 | Espaço | O aplicativo instalado deve ocupar no máximo 50 MB, sem contar os relatos pendentes. | Importante | Analista |
| RNF08 | Eficiência | O aplicativo deve comprimir cada foto para no máximo 1 MB antes do envio. | Importante | Analista |
| RNF09 | Desempenho | Com conexão 4G, o envio de um relato com uma foto deve terminar em até 10 segundos. | Importante | Analista |
| RNF10 | Desempenho | O painel deve exibir o mapa com até 5.000 pontos em até 5 segundos. | Importante | Analista |
| RNF11 | Confiabilidade | O servidor deve estar disponível em 99% do tempo de cada mês. | Importante | Analista |
| RNF12 | Confiabilidade | O sistema deve fazer uma cópia de segurança diária dos dados, criptografada e guardada por 30 dias. | Essencial | Derivado |
| RNF13 | Confiabilidade | O aplicativo deve guardar até 20 relatos pendentes quando estiver sem conexão. | Importante | Entrevista |
| RNF14 | Segurança | Toda comunicação entre aplicativo, painel e servidor deve usar HTTPS com TLS 1.2 ou superior. | Essencial | Legislação (LGPD) |
| RNF15 | Segurança | Nome, telefone, coordenada exata e áudios devem ser armazenados criptografados no servidor e, enquanto o relato estiver pendente, no aparelho. | Essencial | Legislação (LGPD) |
| RNF16 | Segurança | A senha dos usuários internos deve ter ao menos 8 caracteres, com letras e números. A conta deve ser bloqueada por 15 minutos após 5 tentativas erradas seguidas, e a sessão deve expirar após 30 minutos sem uso. | Essencial | Derivado |

## Requisitos organizacionais

<!-- cols: 9,19,46,13,13 -->
| ID | Requisito | Descrição | Prioridade | Fonte |
|---|---|---|---|---|
| RNF17 | Custo | O aplicativo deve ser gratuito para o morador, sem cobrança, anúncios ou compras internas. O sistema deve usar só componentes sem custo de licença; hospedagem e transcrição são pagas por quem operar o sistema. | Essencial | Entrevista |
| RNF18 | Idioma | O aplicativo e o painel devem estar em português do Brasil. | Essencial | Derivado |

## Requisitos externos

<!-- cols: 9,19,46,13,13 -->
| ID | Requisito | Descrição | Prioridade | Fonte |
|---|---|---|---|---|
| RNF19 | Legal – consentimento | Na primeira abertura, o aplicativo deve exibir o termo de uso e a política de privacidade, com a finalidade, os dados coletados, o prazo de guarda (RNF20) e o nome e o contato do controlador e do encarregado de dados, e exigir o aceite antes do primeiro registro. O controlador é a prefeitura que operar o sistema, e esses dados são configurados na instalação. | Essencial | Legislação (LGPD) |
| RNF20 | Legal – prazo de guarda | O sistema deve eliminar o nome e o telefone do morador 180 dias depois da primeira vistoria do ponto feita após o relato e manter o relato sem identificação. Enquanto o ponto aguarda vistoria, os dados ficam guardados, porque o fiscal pode precisar do contato. | Essencial | Legislação (LGPD) |
| RNF21 | Legal – direitos do titular | O sistema deve permitir atender, em até 15 dias, ao pedido do morador para acessar, corrigir ou eliminar os seus dados. | Essencial | Legislação (LGPD) |
| RNF22 | Legal – transferência de dados | A hospedagem e o serviço de transcrição devem manter os dados no Brasil ou em país com proteção equivalente, com contrato que proíba o seu uso para outro fim. | Importante | Legislação (LGPD) |

[[quebra]]

# Requisitos de segurança

<!-- cols: 10,40,50 -->
| ID | Perigo | Soluções |
|---|---|---|
| RS001 | No RF001, alguém pode enviar denúncias falsas ou repetidas em massa para inflar um ponto ou prejudicar um bairro. | Exigir nome e telefone em todo relato e limitar a 5 relatos por telefone a cada 24 horas (RF001). Só a vistoria confirma o ponto (RF007). |
| RS002 | No RF001, a foto ou o vídeo pode não ser do local, por ter sido baixado da internet ou ser antigo. | Marcar como "evidência divergente" a evidência da galeria cuja data ou coordenada de captura difira mais de 7 dias ou 500 metros das do relato, e exibir a marcação ao fiscal (RF003). |
| RS003 | No RF001 e no RF002, o morador identificado pode sofrer retaliação de vizinhos ou de quem causou o problema. | Nome, telefone e coordenada exata visíveis só para a equipe interna (Fiscal e Prefeitura); fora dela, localização só como trecho de rua (RF002); registro de todo acesso (RF019); aviso do risco no termo de uso (RNF19). |
| RS004 | No RF001 ao RF003, uma invasão pode vazar dados pessoais de moradores. | Criptografia na comunicação e no armazenamento (RNF14, RNF15); acesso por perfil (RF018); registro de acesso (RF019); comunicação do incidente à ANPD e aos moradores afetados, como exige a LGPD. |
| RS005 | No RF001, o celular perdido ou roubado pode expor relatos pendentes. | Relatos pendentes criptografados no aparelho (RNF15) e apagados após o envio; o aplicativo não guarda histórico dos relatos enviados, só o nome e o telefone do morador (RF001). |
| RS006 | No RF003, o desfoque automático pode falhar e deixar um rosto ou uma placa visível. | Nenhuma evidência é exibida antes do tratamento; o fiscal marca novo desfoque (RF003); o sistema não tem exibição pública de evidências. |
| RS007 | No RF003, o áudio enviado ao serviço de transcrição pode ser reutilizado ou vazar. | Contrato e localização dos dados conforme o RNF22; envio só do áudio, sem nome nem telefone; exigência de que o serviço apague o áudio após a transcrição (RF003). |
| RS008 | No RF007, o fiscal pode registrar a vistoria sem ir ao local ou com foto de outro ponto. | Exigir que a coordenada do GPS no momento da vistoria esteja a até 100 metros do ponto e aceitar fotos e vídeos da vistoria só pela câmera, nunca da galeria (RF007). |
| RS009 | No RF005 e no RF007, um ponto pode ser deixado de lado de propósito ou marcado como "não confirmado" sem motivo. | Os dias de espera aumentam a pontuação (RF005); o fiscal informa o motivo da não confirmação (RF007); um relato novo devolve o ponto "não confirmado" à fila (RF004); toda atribuição e toda mudança de status ficam registradas (RF019). |
| RS010 | No RF018, a senha de um usuário interno pode ser descoberta ou compartilhada. | Política de senha, bloqueio e expiração da sessão (RNF16); usuário individual; desativação de quem sai da equipe (RF013). |
| RS011 | No RF019, um usuário interno pode apagar o rastro do que fez. | Os registros de log não podem ser alterados nem excluídos, e nenhum perfil tem essa permissão (RF019); o log entra na cópia de segurança (RNF12). |
| RS012 | No RF006, uma camada de vulnerabilidade pode acabar expondo pessoas, como a casa de um paciente. | As camadas descrevem só equipamentos e territórios, a categoria epidemiológica aceita somente polígono e o morador não vê o nível de urgência (RF006). |
| RS013 | No RF020, um arquivo do Censo errado ou adulterado pode distorcer a camada do mapa. | Validar o formato e os códigos dos setores na importação, registrar quem importou (RF019) e manter a importação anterior para poder voltar a ela (RF020). |

[[quebra]]

# Matriz de avaliação de risco

A matriz posiciona cada perigo da Seção 5 pela gravidade do dano, se ele ocorrer, e pela probabilidade de ocorrência com as soluções da Seção 5 já aplicadas. As duas classificações são hipóteses do analista e devem ser revistas se alguma decisão da seção 8 mudar.

- **Gravidade:** catastrófico (1), dano grave a uma pessoa ou exposição de dados pessoais de muitos moradores; crítico (2), dano a uma pessoa ou decisão errada sobre um ponto; marginal (3), distorção corrigível da fila ou do mapa; insignificante (4), sem efeito sobre pessoas ou decisões.
- **Probabilidade:** frequente (A), esperado várias vezes por mês; provável (B), esperado algumas vezes por ano; ocasional (C), pode ocorrer uma vez por ano; remoto (D), pouco provável durante a vida do sistema; improvável (E), só por falha simultânea de mais de uma solução; eliminado (F), as soluções tornam o perigo impossível.

<!-- cols: 20,20,20,20,20 -->
| Gravidade / probabilidade | Catastrófico (1) | Crítico (2) | Marginal (3) | Insignificante (4) |
|---|---|---|---|---|
| Frequente (A) | | | | |
| Provável (B) | | | | |
| Ocasional (C) | | RS009 | RS001, RS002, RS013 | |
| Remoto (D) | RS003, RS004 | RS006, RS007, RS008, RS010 | | |
| Improvável (E) | | RS011, RS012 | RS005 | |
| Eliminado (F) | | | | |

Os perigos de maior atenção são o RS003 e o RS004, por serem catastróficos, e o RS009, o mais provável entre os críticos. Como o perfil Prefeitura é único e vê os dados do morador e distribui as vistorias, as soluções do RS003 e do RS009 dependem sobretudo do registro de acessos e de ações (RF019).

[[quebra]]

# Matriz de rastreabilidade

A matriz registra a ligação entre os requisitos funcionais. A letra "D" indica que o requisito da linha depende do requisito da coluna, isto é, não pode ser implementado sem ele. A letra "R" indica que os dois requisitos estão relacionados, porque um usa ou registra informações do outro, mas cada um pode ser implementado sem o outro. A célula vazia indica que não há ligação.

<!-- cols: 10,6,6,6,6,6,6,6,6,6,6,6,6,6,6 -->
| ID | RF001 | RF002 | RF003 | RF004 | RF005 | RF006 | RF007 | RF010 | RF013 | RF015 | RF018 | RF019 | RF020 | RF021 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| RF001 | — | D | R | R | R |  |  |  |  |  |  | R |  |  |
| RF002 | D | — |  | R |  |  |  |  |  |  |  | D |  |  |
| RF003 | D |  | — |  |  |  | R | R |  |  |  | D |  |  |
| RF004 | D | D |  | — | R | R | R | R |  | R |  |  | R |  |
| RF005 | R |  |  | D | — | R | R |  | D |  | D | R |  |  |
| RF006 |  |  |  | D | R | — |  | R |  | R | D |  |  |  |
| RF007 |  |  | D | D | D |  | — | R |  |  | D | D |  |  |
| RF010 |  |  | R | D |  | R | R | — |  | R | D |  | R |  |
| RF013 |  |  |  |  | R |  |  |  | — |  | D | D |  | R |
| RF015 |  |  |  | D |  | R |  | R |  | — | D |  |  |  |
| RF018 |  |  |  |  | R | R | R | R | D | R | — | R | R | R |
| RF019 | R | R | R |  | R |  | R |  | R |  | D | — | R | R |
| RF020 |  |  |  | R |  |  |  | R |  |  | D | D | — |  |
| RF021 |  |  |  |  |  |  |  |  | D |  | R | D |  | — |

Nenhum requisito essencial depende de um requisito importante: o RF005, o RF010 e o RF015 usam o nível de urgência do RF006, e o RF010 usa a camada do Censo do RF020, quando eles existem, mas funcionam sem eles. Duas dependências são mútuas. A do RF001 com o RF002 é esperada, porque a localização faz parte do registro. A do RF013 com o RF018 é resolvida pelo RF021: o cadastro de usuários exige um usuário autenticado, e a autenticação exige um usuário cadastrado, por isso o primeiro usuário é criado na instalação, fora das telas do painel.

[[quebra]]

# Itens a definir

Cada item abaixo é uma informação que a primeira entrevista não levantou. Nesta versão não há item em aberto. Uns foram resolvidos pelas respostas do cliente (seção 11.4), outros por decisão do grupo, sem validação do cliente (seção 9), e alguns saíram com o requisito que os usava. Os itens continuam na tabela, com a resposta, para que seus identificadores não sejam reutilizados.

## Perfis internos

<!-- cols: 10,40,16,20,14 -->
| ID | O que falta definir | Responsável | Como resolver | Requisitos |
|---|---|---|---|---|
| TBD-01 | Resolvido: o perfil Prefeitura, único, consulta a fila completa e atribui pontos a fiscais. | — | — | RF005 |
| TBD-02 | Removido: o requisito saiu do escopo (seção 12.3). | — | — | — |
| TBD-03 | Resolvido: o perfil Prefeitura cadastra os usuários internos. | — | — | RF013, RF021 |
| TBD-04 | Resolvido: o perfil Prefeitura mantém as camadas de vulnerabilidade e importa os dados do Censo. | — | — | RF006, RF020 |
| TBD-05 | Removido: o requisito saiu do escopo (seção 12.3). | — | — | — |
| TBD-06 | Removido: o requisito saiu do escopo (seção 12.3). | — | — | — |
| TBD-07 | Resolvido: o perfil Prefeitura consulta o mapa, o painel e os indicadores. | — | — | RF010, RF015 |
| TBD-08 | Resolvido: Fiscal e Prefeitura acessam nome, telefone, coordenada exata e áudio do morador. | — | — | RF001, RF002, RF003 |
| TBD-09 | Resolvido: o perfil Prefeitura consulta o log de auditoria. | — | — | RF019 |

## Regras de negócio e valores

<!-- cols: 10,40,16,20,14 -->
| ID | O que falta definir | Responsável | Como resolver | Requisitos |
|---|---|---|---|---|
| TBD-10 | Resolvido: toda denúncia é identificada, e o envio anônimo foi retirado. | — | — | RF001, RF005 |
| TBD-11 | Removido: o requisito saiu do escopo (seção 12.3). | — | — | — |
| TBD-12 | Removido: dependia de um requisito não funcional, retirado nesta revisão. | — | — | — |
| TBD-13 | Resolvido: a imagem original é descartada após o desfoque. A prova da denúncia é a vistoria do fiscal (RF007), e guardar as originais acumularia rostos e placas sem finalidade. | — | — | RF003 |
| TBD-14 | Substituído pelo TBD-22: a restrição do ofício por marcação contratual depende de o contrato fazer parte do escopo. | — | — | RF009 |
| TBD-15 | Removido: o requisito saiu do escopo (seção 12.3). | — | — | — |
| TBD-16 | Removido: o requisito saiu do escopo (seção 12.3). | — | — | — |
| TBD-17 | Resolvido: os pesos da fila (RF005) e os pesos sugeridos por categoria de camada (RF006), propostos pelo analista, foram adotados como definitivos, sem validação do cliente. | — | — | RF005, RF006 |
| TBD-18 | Resolvido: o relato novo num ponto confirmado não exige nova vistoria; o ponto recebe a marcação de reincidência e soma o relato. | — | — | RF004 |
| TBD-19 | Resolvido: a autenticação usa credencial própria do sistema (e-mail e senha), sem login institucional. | — | — | RF018, RF021 |
| TBD-20 | Resolvido: o mapa reflete a alteração de um ponto em até 60 segundos. | — | — | RF010 |
| TBD-21 | Removido: o requisito saiu do escopo (seção 12.3). | — | — | — |
| TBD-22 | Removido: o requisito saiu do escopo (seção 12.3). | — | — | — |
| TBD-23 | Resolvido: sem conexão, o aplicativo guarda a denúncia e a envia quando a conexão voltar. | — | — | RF001, RNF13 |
| TBD-24 | Removido: a integração com o Censo do IBGE e a transcrição por inteligência artificial foram tratadas como pedidos da entrevista. | — | — | RF003, RF020 |
| TBD-25 | Resolvido: um fiscal da prefeitura valida a denúncia em vistoria no local. | — | — | RF005, RF007 |
| TBD-26 | Resolvido: não há tipo "outro". O aplicativo orienta o morador a procurar a prefeitura quando o problema está fora do escopo, e o fiscal, ao registrar "não confirmado", informa se o problema não foi encontrado ou se existe, mas não é de saneamento. | — | — | RF001, RF007 |
| TBD-27 | Resolvido: o aplicativo roda no navegador do celular, no Android 8.0 ou superior e no iOS 15 ou superior. | — | — | RNF05 |
| TBD-28 | Resolvido: a gratuidade vale para o morador. O sistema usa só componentes sem custo de licença, e quem operar o sistema paga a hospedagem e a transcrição. | — | — | RNF17 |
| TBD-29 | Resolvido: nome e telefone são eliminados 180 dias depois da primeira vistoria do ponto feita após o relato. | — | — | RNF20 |
| TBD-30 | Resolvido: o controlador é a prefeitura que operar o sistema. O nome e o contato dela e do encarregado de dados são configurados na instalação e aparecem no termo de uso. | — | — | RF019, RNF19 |

[[quebra]]

# Alterações em relação às versões anteriores

## Mudanças da versão 8

- Foram incorporadas as respostas do cliente às quatro dúvidas enviadas depois da entrevista (seção 11.4), e os itens a definir correspondentes foram resolvidos.
- Validação (TBD-25): um fiscal da prefeitura valida a denúncia em vistoria no local. O RF005 e o RF007 deixaram de tratar a vistoria como hipótese, e a fonte dessa parte passou a "Entrevista".
- Perfis (TBD-01, TBD-03, TBD-04, TBD-07, TBD-08 e TBD-09): a Prefeitura é um perfil único, com acesso a todas as funções internas, inclusive ao nome e ao telefone do morador. As marcas de TBD saíram dos requisitos e da matriz de permissões.
- Anonimato (TBD-10): toda denúncia é identificada. O campo "Identificar-se" e o relato anônimo com prioridade reduzida saíram do RF001; o fator "relato identificado" saiu da pontuação da fila (RF005); o RS001 passou a se apoiar na identificação obrigatória. Na avaliação do analista, a LGPD não exige que o sistema ofereça o envio anônimo: a coleta de nome e telefone é compatível com a lei desde que tenha finalidade declarada, se limite aos dados necessários e tenha acesso restrito e registrado, regras que o RF001, o RF019 e o RNF19 trazem. O RF001 passou a informar a finalidade antes do envio e a preencher nome e telefone com os do último relato.
- Sem conexão (TBD-23): mantido o envio pendente, com reenvio automático. A marca de TBD saiu do RF001 e do RNF13.
- Imagem original (TBD-13): mantido o descarte da imagem original após o desfoque (RF003). A prova da denúncia é a vistoria do fiscal no local, com fotos tiradas só pela câmera (RF007), e guardar as originais criaria um acervo de rostos e placas sem finalidade, contra o princípio da necessidade da LGPD, e aumentaria o perigo do RS004.
- Reincidência (TBD-18): o relato novo num ponto "confirmado" não exige nova vistoria. Como o sistema não tem o status "resolvido", o ponto confirmado continua sendo um problema existente, e o relato novo só o reforça: o ponto recebe a marcação de reincidência e soma o relato (RF004).
- Autenticação (TBD-19): os usuários internos entram com credencial própria do sistema, por e-mail e senha. O cliente não tem ligação com a prefeitura, e não há informação sobre um login institucional que pudesse ser integrado. Saiu a Obs4 do RF021, que tratava desse caso.
- Atualização do mapa (TBD-20): o mapa reflete a alteração de um ponto em até 60 segundos (RF010). O trabalho de vistoria se mede em dias, e uma consulta periódica ao servidor atende ao prazo sem conexão permanente.
- Pesos (TBD-17): a pontuação da fila (RF005) passou a ter valores: 6 pontos por relato, até 10 relatos; 1,5 ponto por dia de espera; 15 pontos por unidade do peso de urgência; e 2 pontos por ponto na fila no mesmo bairro, até 10 pontos. O RF006 passou a sugerir o peso de cada camada pela categoria. Por decisão do grupo, esses valores foram adotados como definitivos, sem validação do cliente, e o TBD-17 foi resolvido.
- Os itens TBD-26 a TBD-30 foram resolvidos por decisão do grupo, sem validação do cliente. A seção 8 ficou sem item em aberto.
- Problema fora do escopo (TBD-26): o tipo "outro" não foi criado. Um relato que nenhum fiscal pode resolver ocuparia a fila à toa. O RF001 passou a orientar o morador a procurar a prefeitura em caso de poço contaminado, vazamento ou lixo, e o RF007 ganhou o campo "Motivo da não confirmação", que separa o problema não encontrado do problema real fora do escopo. O RS009 cita o motivo como solução.
- Plataforma (TBD-27): o aplicativo roda no navegador do celular. Assim um só código atende Android e iOS, sem custo de publicação em loja (RNF05). A fonte do RNF05 passou a "Analista", porque a entrevista pediu um aplicativo, mas não disse em quais aparelhos.
- Gratuidade (TBD-28): a "solução gratuita" da entrevista vale para o morador. Operar sem custo nenhum não é possível, porque o servidor precisa de hospedagem. O RNF17 exige só componentes sem custo de licença e deixa a hospedagem e a transcrição com quem operar o sistema.
- Prazo de guarda (TBD-29): nome e telefone servem ao contato do fiscal e ao limite de relatos por telefone (RF001). Depois da vistoria, perdem a finalidade. O RNF20 fixou a eliminação em 180 dias depois da primeira vistoria do ponto feita após o relato, prazo que cobre um relato novo no mesmo ponto e um pedido do morador sobre os seus dados (RNF21). A eliminação entra no log (RF019).
- Controlador (TBD-30): pela LGPD, controlador é quem decide sobre o tratamento dos dados. Quem decide é a prefeitura que adota e opera o sistema, não o grupo que o desenvolveu. O nome e o contato dela e do encarregado de dados são configurados na instalação e aparecem no termo de uso (RNF19).

## Mudanças da versão 7

- Foram incluídas a matriz de avaliação de risco (Seção 6), que classifica os perigos RS001 a RS013 pela gravidade e pela probabilidade, e a matriz de rastreabilidade (Seção 7), que registra a ligação entre os requisitos funcionais, no formato do modelo de documento de requisitos da disciplina. As seções seguintes foram renumeradas de 8 a 12.
- A matriz de rastreabilidade mostrou que nenhum requisito definia a criação do primeiro usuário interno, necessária para usar o RF013 e o RF018. Foi criado o RF021 (primeiro usuário), que também impede desativar o último usuário ativo com o perfil Prefeitura; o RF019 passou a registrar essa criação, e a matriz de permissões (seção 12.2) ganhou a ação correspondente.
- Fora isso, os requisitos funcionais, não funcionais e de segurança não mudaram em relação à versão 6.

## Mudanças da versão 6

- Os requisitos funcionais passaram a ser apresentados em tabelas com identificador, descrição e prioridade, no formato do modelo de documento de requisitos da disciplina. Cada descrição começa pelo enunciado no imperativo ("O sistema deve…") e traz os atores, os campos com tipo, tamanho, máscara e obrigatoriedade, as notas numeradas de preenchimento e, quando se aplicam, as regras de alteração, exclusão e pesquisa. A divisão de cada requisito em itens numerados (RF001.1, RF001.2 e assim por diante), adotada na versão 5, foi retirada.
- Cada requisito passou a ter uma única prioridade, que vale para o requisito inteiro. Os requisitos que tinham partes essenciais e partes importantes (RF001, RF002, RF003, RF004 e RF019) ficaram essenciais.
- Foram definidos, como hipótese do analista, os tamanhos e tipos que faltavam: nome do morador (120 caracteres), logradouro (150), bairro (80), fonte do dado das camadas (200), transcrição do áudio (2.000), os campos dos setores censitários (RF020) e os campos de evidência, fila e log.
- O ciclo de vida do ponto, a matriz de permissões e as hipóteses fora do escopo, antes na Seção 2, passaram para os anexos (Seção 12).
- Foram incluídos os requisitos não funcionais (Seção 4), divididos em requisitos de produto, organizacionais e externos, e os requisitos de segurança (Seção 5), no formato perigo e soluções do modelo. O limite de três telas do registro saiu do RF001 e passou a ser o RNF01.
- As soluções dos requisitos de segurança acrescentaram regras aos requisitos funcionais, como hipótese do analista: limite de relatos por telefone e ausência de histórico no aparelho (RF001), marcação de evidência divergente e cuidados com o serviço de transcrição (RF003), foto só pela câmera e posição do fiscal na vistoria (RF007), registro da atribuição de pontos e da importação do Censo (RF019) e validação da importação do Censo (RF020).
- Foram criados os itens a definir TBD-27 a TBD-30.
- Fora as regras acima, o conteúdo e o escopo dos requisitos funcionais não mudaram em relação à versão 5.

## Mudanças da versão 5

As subseções a seguir registram as mudanças de estrutura e as inconsistências da versão 4, com a decisão adotada em cada caso. Todas as decisões foram revisadas e aprovadas pelo autor e ainda podem mudar na validação com o cliente. Algumas tratam de requisitos que depois saíram do escopo (decisão 19); elas ficam registradas como histórico.

### Mudanças de estrutura

- Os requisitos foram reescritos conforme a ISO/IEC/IEEE 29148:2018, com cada parágrafo de regras da versão 4 dividido em itens numerados. A versão 6 desfez essa divisão.
- O título "Requisitos funcionais (casos de uso)" passou a "Requisitos funcionais", porque o documento não descreve casos de uso.
- Os papéis "coordenador da fiscalização", "gestão" e "administrador" foram substituídos pelo perfil Prefeitura com TBD, porque não foram confirmados na entrevista.
- Foram incluídos o ciclo de vida do ponto (seção 12.1), a matriz de permissões (seção 12.2), os itens a definir (Seção 8) e as referências (Seção 10).
- Foram criados os grupos RF017 (consulta de protocolo), RF018 (autenticação) e RF019 (auditoria), que a versão 4 pressupunha sem especificar; o RF017 depois saiu do escopo (seção 12.3).
- Os requisitos não funcionais foram retirados desta revisão.
- O escopo foi reduzido aos pedidos da entrevista e ao que eles exigem; as funções retiradas estão na seção 12.3.
- A fonte passou a ser indicada em cada requisito, e não mais no grupo, e prioridade, fonte e verificação são escritas por extenso nas tabelas.
- A frase da versão 4 sobre o treinamento dos fiscais para reconhecer rosto ou placa não desfocados foi retirada, porque descreve um processo de trabalho e não um requisito do sistema.

### Inconsistências resolvidas

<!-- cols: 6,47,47 -->
| # | Situação na versão 4 | Decisão adotada |
|---|---|---|
| 1 | O RF005 dizia que o fiscal vê apenas os pontos atribuídos, mas o RF007 permitia vistoriar ponto "disponível na fila" e o RF010 dava ao fiscal o mapa inteiro. | O fiscal vê e vistoria somente os pontos atribuídos a ele (RF005 e RF007) e não usa o mapa do RF010. Decisão do analista, aprovada pelo autor. |
| 2 | O relato anônimo "não grava dado que identifique", mas a coordenada exata do RF002 é tratada como dado pessoal. | A proibição vale para os dados do autor: nome, telefone e identificador do aparelho (RF001). A coordenada é do problema e é necessária à vistoria. Decisão do analista, aprovada pelo autor. |
| 3 | Para foto da galeria, não estava definido se valia a posição do GPS no envio ou a coordenada dos metadados. | Vale a posição confirmada pelo morador no mapa (RF002). A coordenada dos metadados é guardada como informação auxiliar (RF002). Decisão do analista, aprovada pelo autor. |
| 4 | O descarte da imagem original conflita com o respaldo probatório que motivou a identificação por padrão. | Mantido o descarte da versão 4 (RF003), com a questão aberta para o cliente [TBD-13]. |
| 5 | O protocolo era exibido ao final do registro, mas o relato pode ficar guardado no aparelho sem conexão. | O protocolo é gerado no aparelho (RF001), e a consulta informa quando o relato ainda não foi recebido (RF017). Decisão do analista, aprovada pelo autor. |
| 6 | Nada definia quando o ponto passa a "em vistoria", nem o que acontece com o ponto notificado que recebe relato novo. | O ponto passa a "em vistoria" na atribuição a um fiscal (RF005). O ponto notificado recebe a marcação de reincidência sem mudar de status, até a resposta do TBD-18. Decisão do analista, aprovada pelo autor. |
| 7 | A quantidade de relatos entrava na pontuação, e a densidade de denúncias entrava de novo como multiplicador, sem fórmula. | A pontuação é uma soma ponderada de cinco fatores, um deles a quantidade de pontos na fila no mesmo bairro (RF005). Decisão do analista, aprovada pelo autor. |
| 8 | Não estava definido como o peso de 1 a 3 das camadas vira o nível normal, elevado ou crítico. | A pontuação da fila usa o peso direto, de 0 (sem camada) a 3 (RF005 e RF006). O nível serve à exibição: 0 é "normal", 1 ou 2 é "elevado" e 3 é "crítico" (RF006). Decisão do analista, aprovada pelo autor. |
| 9 | O RF008 falava em "três situações", mas havia cinco situações de setor e a marcação "prazo contratual vencido". Ponto em setor atendido só pelo outro serviço ficava sem classificação. | Foram definidas cinco marcações, incluindo "sem enquadramento contratual" (RF008). Decisão do analista, aprovada pelo autor. |
| 10 | O RF009 exigia apenas status "confirmado", mas o RF008 dizia que ponto "fora da concessão" não gera notificação, e nada dizia sobre "dentro do prazo contratual". | Qualquer ponto confirmado pode ser notificado (RF009). Como o uso do contrato ficou a validar [TBD-22], a restrição por marcação contratual só volta se ele for confirmado. Decisão do analista, aprovada pelo autor. |
| 11 | O ofício tinha status "emitida ou enviada", sem regra para a mudança. | A Prefeitura registra o envio, com data e meio, e o status muda para "enviado" (RF009). Decisão do analista, aprovada pelo autor. |
| 12 | A correção do tipo de um ponto não dizia se o agrupamento era refeito. | Os vínculos não são refeitos automaticamente. O fiscal desvincula ou une manualmente (RF004). Decisão do analista, aprovada pelo autor. |
| 13 | O modo público mostrava pontos "sem fotos originais", mas as originais já são descartadas pelo RF003. | O modo público não exibe nenhuma evidência (RF010). Decisão do analista, aprovada pelo autor. |
| 14 | O limite de desvio da rota, os bairros e os órgãos de destino eram usados, mas não constavam da parametrização. | Foram incluídos no RF014. Decisão do analista, aprovada pelo autor. |
| 15 | Valores sem medida: "texto curto", "formato editável", "arquivo geográfico", "mesma foto", "tempo real". | Foram fixados como 100 caracteres (RF002), DOCX (RF009), GeoJSON ou CSV (RF008) e conteúdo idêntico (RF012). O prazo de atualização do painel ficou para o TBD-20 (RF010). Decisão do analista, aprovada pelo autor. |
| 16 | A vistoria aceitava evidências sem obrigatoriedade, embora o ofício dependa delas. | Passa a ser obrigatória ao menos uma foto ou um vídeo quando o resultado for "confirmado" (RF007), e as observações ficam limitadas a 1.000 caracteres (RF007). Decisão do analista, aprovada pelo autor. |
| 17 | A motivação do sistema era a fiscalização das metas do contrato de concessão pelo Marco Legal, assunto que não foi tratado na entrevista, cujo foco foi dar ao morador uma forma fácil de denunciar. | A seção 2.1 passou a ter o morador no centro. O cruzamento contratual (RF008) e os itens que dependem dele ficam a validar [TBD-22], com prioridade "importante". Decisão do analista, aprovada pelo autor. |
| 18 | A versão 4 tratava a entrevista como feita com a Secretaria Municipal de Infraestrutura e Saneamento, atribuía a ela informações sem registro e apresentava como conferidas informações levantadas pelo analista. | O cliente não tem ligação com a prefeitura. A Seção 2 e a fonte de cada requisito foram refeitas a partir das anotações transcritas na Seção 11; o que não está nelas passou a "Analista". A integração com o Censo do IBGE (RF020) e a transcrição por inteligência artificial (RF003), pedidas na entrevista, foram incluídas. |
| 19 | O documento tinha 205 requisitos funcionais, 163 deles hipóteses do analista, e 13 dos 20 grupos não atendiam a nenhum pedido da entrevista. | Ficaram os pedidos, os requisitos derivados deles (nova fonte "Derivado") e as hipóteses necessárias para especificá-los. Sete grupos e os requisitos isolados que nenhum pedido exige saíram do escopo (seção 12.3). Decisão do analista, aprovada pelo autor. |

# Referências

- ISO/IEC/IEEE 29148:2018 – Systems and software engineering – Life cycle processes – Requirements engineering.
- Brasil. Lei nº 13.709, de 14 de agosto de 2018 – Lei Geral de Proteção de Dados Pessoais (LGPD).
- Brasil. Lei nº 14.026, de 15 de julho de 2020 – Atualiza o Marco Legal do Saneamento Básico.
- Anotações da entrevista com o cliente, transcritas na Seção 11.
- Documento de Requisitos – SaneaMapa, versões 3, 4 e 5.

[[quebra]]

# Anotações da entrevista

O cliente não tem ligação com a Secretaria nem com a prefeitura: a demanda tem caráter social e educativo, sem fins lucrativos, como uma solicitação da comunidade ao CESMAC. Seus interesses ainda não estão bem definidos, mas apontam para dar poder à comunidade.

Os dois registros abaixo foram feitos por dois integrantes do grupo e estão transcritos sem alteração. Eles e as respostas do cliente às dúvidas, na seção 11.4, são a única fonte do que o documento atribui à "Entrevista".

## Primeira anotação

- No papel a rua está saneada, mas na prática não. A ideia é empoderar a comunidade para questionar as áreas que não estão saneadas.
- Recebe áudio, vídeo, imagem, sem anonimato para identificar a pessoa.
- A prefeitura verá os status numa tela para identificar volumetria de denúncias por localização.
- Existem fontes alternativas, mas difíceis de usar e pouco ágeis.
- Ele espera que o cliente tenha acesso à internet, sem planos do que fazer caso contrário.
- Precisão exata.
- Solução gratuita.
- Seria interessante dar prioridade a grupos de risco diversos.

## Segunda anotação

- 1. Própria comunidade fazer o papel de questionar as áreas que estão saneadas: fotos, vídeos, áudios, transcrever por IA, pessoa se declarar, geolocalização, prefeitura ver em dashboard, validar a denúncia.
- 2. Um aplicativo que deve ser utilizado para resolver essa problemática.
- 3. É esperado que o cliente tenha celular com conexão. Uso off-line, até a denúncia prosseguir.
- 4. Solução gratuita.
- 5. Integração com o censo do IBGE.
- 6. Priorizar situações de risco.

## Divergências entre as anotações

- Conexão: a primeira anotação diz que não há plano para quem estiver sem internet; a segunda registra "uso off-line, até a denúncia prosseguir". A divergência foi resolvida pela resposta 4 da seção 11.4.

## Respostas do cliente às dúvidas

Depois da entrevista, quatro dúvidas foram enviadas ao cliente, cada uma com a solução que o grupo pretendia adotar. O cliente concordou com as quatro.

- 1. Quem valida a denúncia: um fiscal da prefeitura, indo ao local (TBD-25).
- 2. Perfis da prefeitura: um perfil só, que vê tudo, inclusive nome e telefone do morador (TBD-01, TBD-03, TBD-04, TBD-07, TBD-08 e TBD-09).
- 3. Anonimato: a denúncia é sempre identificada, como dito na entrevista (TBD-10).
- 4. Sem internet: o aplicativo guarda a denúncia e a envia quando a conexão voltar (TBD-23).

[[quebra]]

# Anexos

Os anexos complementam a descrição geral do sistema e os requisitos funcionais.

## Ciclo de vida do ponto

Um ponto reúne os relatos de um mesmo problema num mesmo local (RF004). A tabela abaixo lista todas as mudanças de status permitidas. Qualquer mudança que não esteja na tabela é proibida.

<!-- cols: 20,22,30,13,15 -->
| De | Para | Evento | Quem | Requisito |
|---|---|---|---|---|
| (novo) | Aguardando vistoria | Relato sem ponto do mesmo tipo a até 50 metros | Sistema | RF004 |
| Aguardando vistoria | Em vistoria | Atribuição do ponto a um fiscal | Prefeitura | RF005 |
| Em vistoria | Aguardando vistoria | Retirada da atribuição | Prefeitura | RF005 |
| Em vistoria | Confirmado | Vistoria com resultado "confirmado" | Fiscal | RF007 |
| Em vistoria | Não confirmado | Vistoria com resultado "não confirmado" | Fiscal | RF007 |
| Não confirmado | Aguardando vistoria | Relato novo vinculado ao ponto | Sistema | RF004 |


## Matriz de permissões

A matriz de permissões mostra o que cada perfil pode fazer no sistema. Cada linha é uma ação, e cada coluna, um perfil. "Sim" indica que o perfil pode executar a ação, e "—", que não pode. A Prefeitura é um perfil único, sem divisão interna.

<!-- cols: 46,16,12,12,14 -->
| Ação | Requisitos | Morador | Fiscal | Prefeitura |
|---|---|---|---|---|
| Registrar ocorrência | RF001, RF002 | Sim | — | — |
| Consultar os pontos atribuídos a si | RF005 | — | Sim | — |
| Registrar vistoria | RF007 | — | Sim | — |
| Corrigir tipo e localização de um ponto | RF007 | — | Sim | — |
| Marcar região de foto para novo desfoque | RF003 | — | Sim | — |
| Ver nome e telefone do morador, a coordenada exata e ouvir áudios | RF001, RF002, RF003 | — | Sim | Sim |
| Consultar a fila completa e atribuir pontos a fiscais | RF005 | — | — | Sim |
| Consultar mapa, painel e indicadores | RF010, RF015 | — | — | Sim |
| Cadastrar usuários internos | RF013 | — | — | Sim |
| Receber o primeiro acesso na instalação | RF021 | — | — | Sim |
| Manter camadas de vulnerabilidade e importar dados do Censo | RF006, RF020 | — | — | Sim |
| Consultar o log de auditoria | RF019 | — | — | Sim |

## Hipóteses fora do escopo

As funções abaixo apareceram em versões anteriores como hipóteses do analista, mas nenhum pedido da entrevista depende delas. Elas ficam fora desta versão e só voltam se o cliente as pedir. Seus identificadores não são reutilizados.

<!-- cols: 12,30,58 -->
| Grupo | Função | O que fazia |
|---|---|---|
| RF008 | Cruzamento contratual | Comparava os pontos confirmados com a área de abrangência e o cronograma do contrato de concessão, segundo o Marco Legal do Saneamento. |
| RF009 | Notificação | Gerava o ofício de cobrança à concessionária a partir dos pontos confirmados. |
| RF011 | Exportação | Exportava pontos em planilha e gerava relatórios em PDF. |
| RF012 | Moderação | Sinalizava e descartava relatos falsos ou repetidos. |
| RF014 | Parametrização | Permitia alterar em tela os limites, pesos, tipos e textos do sistema. |
| RF016 | Rota do fiscal | Calculava a ordem de visita das vistorias do dia. |
| RF017 | Consulta de protocolo | Permitia ao morador acompanhar o relato pelo número do protocolo. |

Também saíram requisitos isolados que não eram exigidos por nenhum pedido: o mapa público, os indicadores de tempo e de reincidência, o encaminhamento a outro órgão, a reordenação manual da fila, a área de atuação do fiscal, a junção e a separação manual de pontos e a extração de quadros de vídeo.
