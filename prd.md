# PRD — SaneaMapa (Product Requirements Document)

**Versão:** 1.0 (alinhada ao Documento de Requisitos v7 e à implementação Spring Boot + PostgreSQL)  
**Contexto:** Plano de Avaliação Integrada — Programação Orientada a Objetos (2026.2) / Demanda social e educativa da comunidade ao CESMAC  
**Repositório:** `saneamapa-app/` (backend Java 21 / Spring Boot + frontend web)

---

## 1. Visão Geral e Problema

Em muitas áreas urbanas, registros oficiais indicam que ruas e bairros estão atendidos por rede de água e esgotamento sanitário ("saneado no papel"), mas na prática os moradores enfrentam falta de água, água imprópria, ausência de rede de esgoto ou esgoto a céu aberto. Fontes públicas tradicionais (como Censo decenal ou bases agregadas por município) não descem ao nível do trecho de rua em tempo hábil e não permitem à população contestar ou evidenciar falhas pontuais.

O **SaneaMapa** é um Sistema de Registro e Notificação de Saneamento Básico que permite à comunidade registrar ocorrências georreferenciadas com evidências multimídia (foto, vídeo e áudio), confrontando os relatos de campo com os dados censitários do IBGE e organizando a validação técnica por fiscais em campo e gestores municipais, em conformidade com a **LGPD (Lei nº 13.709/2018)**.

---

## 2. Objetivos do Produto

- **Empoderar a comunidade** com um canal gratuito, simples (até 3 telas) e acessível no navegador/celular para denunciar falhas de saneamento sem necessidade de cadastro prévio.
- **Consolidar ocorrências por proximidade** (agrupamento em raio de 50 metros por tipo), evitando duplicidade e mensurando a gravidade real de cada ponto.
- **Priorizar áreas vulneráveis** cruzando os pontos com camadas de sensibilidade social (creches, escolas, UBS, comunidades tradicionais e áreas de vulnerabilidade epidemiológica).
- **Garantir validação confiável em campo**, exigindo presença física do fiscal (raio ≤ 100 m via GPS) e captura direta pela câmera ao confirmar ocorrências.
- **Assegurar conformidade LGPD**, protegendo dados pessoais de moradores (ocultação de coordenadas exatas e contatos na visão pública, tratamento de mídias, controle de acesso por perfil e trilha de auditoria imutável).

---

## 3. Perfis de Usuário (Personas)

| Perfil | Autenticação | Responsabilidades Principais | Interface |
|---|---|---|---|
| **Morador** | Sem login (público) | Registrar ocorrências de saneamento com GPS/mapa e evidências (foto, vídeo, áudio); optar por identificação ou envio anônimo; consultar mapa público LGPD. | [morador.html](saneamapa-app/src/main/resources/static/morador.html) |
| **Fiscal** | Individual (E-mail + Senha / JWT) | Visualizar exclusivamente os pontos atribuídos a si; acessar contatos e áudios com log de auditoria; aplicar novo desfoque e corrigir transcrições; registrar vistoria em campo a até 100 m do ponto. | [fiscal.html](saneamapa-app/src/main/resources/static/fiscal.html) |
| **Prefeitura** | Individual (E-mail + Senha / JWT) | Visualizar mapa completo com camadas (pontos, calor, vulnerabilidade e Censo IBGE); consultar indicadores; gerenciar fila e atribuir pontos a fiscais; cadastrar usuários e camadas; importar/reverter dados do Censo; auditar acessos LGPD. | [painel.html](saneamapa-app/src/main/resources/static/painel.html) |

---

## 4. Escopo Funcional (Requisitos Funcionais Implementados)

| ID | Funcionalidade | Prioridade | Resumo do Fluxo e Regras | Spec |
|---|---|---|---|---|
| **RF001** | Registro de ocorrência | Essencial | 4 tipos (`CASA_SEM_AGUA`, `AGUA_FORA_PADRAO`, `RUA_SEM_ESGOTO`, `ESGOTO_CEU_ABERTO`); protocolo `SM-...` gerado no cliente; envio identificado (nome + telefone válidos) ou anônimo (prioridade reduzida, sem guardar nome/telefone); limite de 5 relatos por telefone em 24h; suporte a fila offline (`localStorage`). | [01-registro-relatos-e-midias.md](specs/01-registro-relatos-e-midias.md) |
| **RF002** | Captura e ajuste de localização | Essencial | Captura de GPS + ajuste manual no mapa Leaflet/OpenStreetMap; cálculo de trecho de rua público; coordenada exata restrita à equipe interna com log de acesso. | [01-registro-relatos-e-midias.md](specs/01-registro-relatos-e-midias.md) |
| **RF003** | Tratamento de evidências | Essencial | Foto, vídeo (≤ 30 s) e áudio (≤ 60 s); marcação de evidência divergente da galeria (> 7 dias ou > 500 m); novo desfoque manual e correção de transcrição pelo fiscal; bloqueio de acesso público às mídias. | [01-registro-relatos-e-midias.md](specs/01-registro-relatos-e-midias.md) |
| **RF004** | Agrupamento espacial de relatos | Essencial | Relatos do mesmo tipo em até **50 m** são agrupados no ponto mais próximo; relato em ponto `CONFIRMADO` marca reincidência; relato em `NAO_CONFIRMADO` reabre para `AGUARDANDO_VISTORIA`. | [01-registro-relatos-e-midias.md](specs/01-registro-relatos-e-midias.md) |
| **RF005** | Fila de vistoria ponderada e atribuição | Essencial | Pontuação ponderada por quantidade de relatos, dias de espera, peso de urgência, densidade no bairro e bônus de identificação; atribuição de fiscal muda status para `EM_VISTORIA`. | [02-fila-urgencia-e-painel.md](specs/02-fila-urgencia-e-painel.md) |
| **RF006** | Camadas de vulnerabilidade social | Importante | Geometrias `PONTO_RAIO` ou `POLIGONO` (polígono obrigatório para `VULNERABILIDADE_EPIDEMIOLOGICA` — RS012); classificação automática de peso (0 a 3) e nível (`NORMAL`, `ELEVADA`, `CRITICA`). | [02-fila-urgencia-e-painel.md](specs/02-fila-urgencia-e-painel.md) |
| **RF007** | Vistoria em campo | Essencial | Fiscal só vistoria pontos atribuídos a si; exige distância GPS ≤ **100 m** (RS008), origem `CAMERA` e ao menos uma mídia para confirmar; atualiza tipo/posição corrigidos e grava histórico. | [03-vistoria-em-campo.md](specs/03-vistoria-em-campo.md) |
| **RF010 / RF015** | Mapa, painel e indicadores | Essencial / Importante | Filtros por tipo, status, bairro, período, mínimo de relatos e urgência; camadas de mapa; totais por status, tipo, urgência e bairro. | [02-fila-urgencia-e-painel.md](specs/02-fila-urgencia-e-painel.md) |
| **RF013 / RF021** | Cadastro de usuários e bootstrap inicial | Essencial | Validação de dígitos verificadores do CPF (imutável), unicidade de CPF/e-mail, desativação lógica preservando histórico, bloqueio da desativação do último usuário `PREFEITURA` ativo e criação inicial via `DataInitializer`. | [04-autenticacao-usuarios-e-auditoria.md](specs/04-autenticacao-usuarios-e-auditoria.md) |
| **RF018** | Autenticação e controle de acesso | Essencial | Login com BCrypt + JWT (expiração em 30 min); bloqueio temporário de 15 minutos após 5 tentativas falhas; RBAC por perfil (`FISCAL`, `PREFEITURA`). | [04-autenticacao-usuarios-e-auditoria.md](specs/04-autenticacao-usuarios-e-auditoria.md) |
| **RF019** | Auditoria LGPD imutável | Essencial | Registro de acessos a dados pessoais (contatos, coordenada exata, áudio) e mutações críticas; protegido contra `UPDATE`/`DELETE` tanto no JPA quanto por trigger no PostgreSQL. | [04-autenticacao-usuarios-e-auditoria.md](specs/04-autenticacao-usuarios-e-auditoria.md) |
| **RF020** | Integração com Censo IBGE | Importante | Importação de setores censitários (15 dígitos, % água e esgoto) via CSV ou GeoJSON com validação integral e suporte a rollback para importação anterior. | [05-integracao-censo-ibge.md](specs/05-integracao-censo-ibge.md) |

---

## 5. Ciclo de Vida do Ponto

```
(novo relato sem ponto ≤ 50 m) ──▶ AGUARDANDO_VISTORIA
AGUARDANDO_VISTORIA ──[Prefeitura atribui fiscal]──▶ EM_VISTORIA
EM_VISTORIA         ──[Prefeitura retira atribuição]──▶ AGUARDANDO_VISTORIA
EM_VISTORIA         ──[Vistoria: Confirmado]──▶ CONFIRMADO
EM_VISTORIA         ──[Vistoria: Não confirmado]──▶ NAO_CONFIRMADO
NAO_CONFIRMADO      ──[Novo relato vinculado]──▶ AGUARDANDO_VISTORIA
```

A máquina de estados é garantida duplamente: no domínio OO ([Ponto.java](saneamapa-app/src/main/java/br/edu/cesmac/saneamapa/domain/model/Ponto.java)) e no banco via trigger (`trg_ponto_ciclo_vida` em [02_triggers.sql](saneamapa-app/database/02_triggers.sql)).

---

## 6. Requisitos Não Funcionais e Segurança (Resumo)

- **Usabilidade e mensagens (RNF01–RNF03):** Fluxo do morador em até 3 etapas principais; erros padronizados pela `GlobalExceptionHandler` e `SecurityConfig` retornando sempre `{ "erro": "...", "orientacao": "..." }`.
- **Segurança de acesso (RNF16, RS010):** Hash BCrypt para senhas, JWT stateless com validade de 30 minutos, bloqueio por 15 minutos após 5 falhas consecutivas.
- **Privacidade LGPD (RF001–RF003, RS003, RS006, RS011, RS012):**
  - Endpoint público `/api/publico/pontos` omite coordenadas exatas, contatos, áudios e nível de urgência.
  - `/api/midias/**` exige autenticação de equipe interna (`FISCAL` ou `PREFEITURA`).
  - Camadas epidemiológicas aceitam apenas geometria `POLIGONO`.
  - Registros de `log_auditoria` são estritamente append-only.
- **Limitações conhecidas do protótipo implementado:**
  - Desfoque automático de rostos/placas (`EvidenciaFoto`) e remoção de áudio de vídeo (`EvidenciaVideo`) são simulados via metadados (`desfoqueAplicado`, `somRemovido`) — o arquivo armazenado é o enviado originalmente.
  - Transcrição de áudio (`ServicoTranscricaoAudioSimulado`) utiliza implementação simulada plugável via interface `ServicoTranscricaoAudio`.

---

## 7. Escopo Excluído desta Versão

Conforme a Seção 12.3 do [Documento de Requisitos](requisitos/requisitos.md), permanecem fora desta versão: cruzamento contratual com concessionária (RF008), geração de ofício de notificação (RF009), exportação de planilhas/PDF (RF011), moderação manual de relatos (RF012), tela de parametrização dinâmica (RF014), roteirização automática do fiscal (RF016) e consulta pública de protocolo (RF017).

---

## 8. Documentação Relacionada

- [Arquitetura do Sistema e Pilares de POO](docs/arquitetura.md)
- [Especificações Funcionais e Técnicas (`specs/`)](specs/)
- [Competências e Tecnologias Aplicadas (`docs/skills.md`)](docs/skills.md)
- [Documento de Requisitos Completo v7](requisitos/requisitos.md)
- [Entrega SQL 3AF](saneamapa-app/database/ENTREGA_3AF_SQL.md)
