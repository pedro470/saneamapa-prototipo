# Spec 04 — Autenticação, Gestão de Usuários e Auditoria LGPD

**Requisitos cobertos:** `RF013`, `RF018`, `RF019`, `RF021`, `RNF16`, `RS010`, `RS011`  
**Atores:** Equipe Interna (`FISCAL`, `PREFEITURA`) e Sistema

---

## 1. Objetivo

Controlar o acesso individual da equipe interna por meio de autenticação segura com JWT, manter o cadastro e o histórico de alterações de usuários sem exclusão física e registrar de forma imutável todos os acessos a dados pessoais e mutações críticas exigidos pela LGPD.

---

## 2. Endpoints e Contratos

| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| `POST` | `/api/auth/login` | Público | Autentica por `email` e `senha`, retornando token JWT e perfil. |
| `POST` | `/api/auth/redefinir-senha` | Público | Solicitação de redefinição de senha para conta ativa. |
| `GET` | `/api/auth/me` | Autenticado | Retorna dados do usuário autenticado no token atual. |
| `GET` | `/api/prefeitura/usuarios` | `PREFEITURA` | Lista todos os usuários com CPF mascarado. |
| `GET` | `/api/prefeitura/usuarios/alteracoes` | `PREFEITURA` | Lista histórico de alterações cadastrais (`AlteracaoUsuario`). |
| `POST` | `/api/prefeitura/usuarios` | `PREFEITURA` | Cria novo usuário (`FISCAL` ou `PREFEITURA`) validando CPF/unicidade/política de senha. |
| `PUT` | `/api/prefeitura/usuarios/{id}` | `PREFEITURA` | Atualiza nome, e-mail, celular, perfil e status ativo (CPF é imutável). |
| `PATCH` | `/api/prefeitura/usuarios/{id}/alternar-ativo` | `PREFEITURA` | Ativa/desativa usuário impedindo desativar o último `PREFEITURA` ativo (`RF021`). |
| `GET` | `/api/prefeitura/auditoria` | `PREFEITURA` | Pesquisa logs imutáveis por `usuarioId`, `apenasSistema`, `tipoEvento`, `de` e `ate`. |
| `GET` | `/api/publico/privacidade` | Público | Dados do termo de uso: controlador, encarregado de dados e prazo de guarda (RNF19). |

---

## 3. Regras de Negócio e Segurança

### 3.1. Política de Senha e Bloqueio (`RF018`, `RNF16`)
- Senhas são armazenadas com `BCryptPasswordEncoder`.
- Política mínima no cadastro: ao menos 8 caracteres, contendo letras e números.
- Em [Usuario.java](saneamapa-app/src/main/java/br/edu/cesmac/saneamapa/domain/model/Usuario.java):
  - Usuário com `ativo == false` não autentica (`AcessoNegadoException`).
  - Após **5 tentativas erradas consecutivas**, a conta é bloqueada por **15 minutos** (`bloqueadoAte`).
  - Login bem-sucedido zera `tentativasFalhas`.
  - Token JWT possui validade de **30 minutos** (`saneamapa.jwt.expiration-minutes: 30`).

### 3.2. Integridade Cadastral e Bootstrap (`RF013`, `RF021`)
- Validação dos dígitos verificadores do CPF em `CpfUtils.isCpfValido`.
- CPF e e-mail únicos (erro HTTP 409 `ConflitoDadosException` com mensagem `"CPF já cadastrado"` / `"E-mail já cadastrado"`).
- CPF possui `updatable = false` no JPA e não é editável após criação.
- Alterações cadastrais gravam `AlteracaoUsuario` (autor, alvo, campo, valor anterior e novo) + evento `ALTERACAO_USUARIO` em `log_auditoria`.
- Não há exclusão física (`DELETE`) de usuários; usa-se desativação lógica (`ativo = false`), sendo proibido desativar o último usuário ativo com perfil `PREFEITURA`.
- [DataInitializer.java](saneamapa-app/src/main/java/br/edu/cesmac/saneamapa/config/DataInitializer.java) cria os usuários iniciais quando o banco está vazio.

### 3.3. Auditoria Imutável LGPD (`RF019`, `RS011`)
- Eventos auditados (`TipoEventoAuditoria`): `ACESSO_CONTATO_MORADOR`, `ACESSO_COORDENADA_EXATA`, `AUDIO_OUVIDO`, `EDICAO_FOTO`, `CORRECAO_TRANSCRICAO`, `ATRIBUICAO_PONTO`, `RETIRADA_ATRIBUICAO`, `MUDANCA_STATUS_PONTO`, `ALTERACAO_USUARIO`, `IMPORTACAO_CENSO`, `ROLLBACK_CENSO`, `ELIMINACAO_DADOS_MORADOR`.
- Imutabilidade garantida em 3 camadas:
  1. **JPA (`LogAuditoria`)**: `@PreUpdate` e `@PreRemove` lançam `AcessoNegadoException`.
  2. **Trigger PostgreSQL (`02_triggers.sql`)**: `trg_log_auditoria_imutavel` bloqueia `UPDATE` e `DELETE`.
  3. **Permissões SQL (`04_contas_permissoes.sql`)**: `REVOKE UPDATE, DELETE ON TABLE log_auditoria` para todas as roles.

### 3.4. Controlador e prazo de guarda (`RNF19`, `RNF20`; TBD-29 e TBD-30)
- O controlador dos dados é a prefeitura que opera o sistema. Nome e contato dela e do encarregado ficam em `saneamapa.lgpd.*` no [application.yml](saneamapa-app/src/main/resources/application.yml) (variáveis `CONTROLADOR_NOME`, `CONTROLADOR_CONTATO`, `ENCARREGADO_NOME`, `ENCARREGADO_CONTATO`). O termo de uso de [morador.html](saneamapa-app/src/main/resources/static/morador.html) lê esses dados de `/api/publico/privacidade`. A chave do aceite mudou para `saneamapa_termo_aceito_v8`, e quem aceitou o termo antigo vê o novo.
- [PrazoGuardaService.java](saneamapa-app/src/main/java/br/edu/cesmac/saneamapa/service/PrazoGuardaService.java) roda todo dia às 3h. Ele procura relatos ainda identificados cujo ponto recebeu, depois do relato, uma vistoria com mais de 180 dias (`PRAZO_GUARDA_DIAS`), chama `Relato.anonimizar` (nome e telefone viram `NULL` e `anonimizado_em` recebe a data) e registra `ELIMINACAO_DADOS_MORADOR`. O relato que chega a um ponto já `CONFIRMADO` (reincidência, sem nova vistoria) tem o prazo contado da data do relato. Enquanto o ponto aguarda vistoria, nada é apagado. A eliminação e o log ficam na mesma transação.
- No banco, `chk_relato_identificacao` aceita duas formas: relato identificado (nome e telefone válidos, `anonimizado_em` nulo) ou anonimizado (nome e telefone nulos, `anonimizado_em` preenchido).
- A consulta de contatos de um ponto ignora os relatos anonimizados.

---

## 4. Critérios de Aceitação Verificados em Testes

- `DominioPooTest.deveBloquearUsuarioApos5TentativasEValidarCpf`
- `DominioPooTest.deveAnonimizarRelatoUmaVezSoAoFimDoPrazo`
- `SaneaMapaIntegracaoTest.fluxoCompletoMoradorAgrupamentoPrefeituraEFiscal` (nada é apagado antes de 180 dias da vistoria; depois, a eliminação roda uma vez e entra no log; o relato de reincidência é apagado 180 dias depois do relato)
- `SaneaMapaIntegracaoTest.uploadRealDeMidiasEConsultaConfiguracao` (`/api/publico/privacidade` responde sem autenticação)
- `SaneaMapaIntegracaoTest.fluxoCompletoMoradorAgrupamentoPrefeituraEFiscal` (RBAC 403 quando Fiscal tenta acessar `/api/prefeitura/auditoria`, registro de auditoria e erro 409 em CPF duplicado)
