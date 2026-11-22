# Spec 02 — Fila Ponderada, Camadas de Vulnerabilidade, Painel e Indicadores

**Requisitos cobertos:** `RF005`, `RF006`, `RF010`, `RF015`, `RS009`, `RS012`  
**Atores:** Prefeitura (autenticada) e Sistema  
**Interface:** [painel.html](saneamapa-app/src/main/resources/static/painel.html)

---

## 1. Objetivo

Priorizar automaticamente os pontos pendentes em uma fila de vistoria com base em critérios objetivos e vulnerabilidade social, oferecer mapa interativo com filtros e camadas temáticas à Prefeitura, e consolidar indicadores gerenciais.

---

## 2. Endpoints e Contratos

| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| `GET` | `/api/prefeitura/pontos` | `PREFEITURA` | Lista pontos filtrados por `tipo`, `status`, `bairro`, `dias`, `minRelatos` e `urgencia`, ordenados pela fila. |
| `GET` | `/api/prefeitura/pontos/{pontoId}` | `PREFEITURA` | Retorna detalhe do ponto (relatos, evidências, vistorias, setores do Censo). |
| `POST` | `/api/prefeitura/pontos/{pontoId}/coordenada-exata` | `PREFEITURA` | Revela latitude/longitude exatas registrando `ACESSO_COORDENADA_EXATA` na auditoria. |
| `POST` | `/api/prefeitura/pontos/{pontoId}/contatos` | `PREFEITURA` | Revela nome/telefone dos relatos identificados registrando `ACESSO_CONTATO_MORADOR`. |
| `POST` | `/api/prefeitura/evidencias/{evidenciaId}/audio` | `PREFEITURA` | Libera reprodução de áudio registrando `AUDIO_OUVIDO`. |
| `PUT` | `/api/prefeitura/pontos/{pontoId}/atribuicao` | `PREFEITURA` | Atribui ou retira `fiscalId` do ponto, gravando `ATRIBUICAO_PONTO` / `RETIRADA_ATRIBUICAO`. |
| `GET` | `/api/prefeitura/indicadores` | `PREFEITURA` | Retorna totais por status, tipo de ocorrência, urgência e bairro (filtros `dias`, `bairro`). |
| `GET` | `/api/prefeitura/camadas` | `PREFEITURA` | Lista camadas de vulnerabilidade e quantidade de pontos abrangidos. |
| `POST` | `/api/prefeitura/camadas` | `PREFEITURA` | Cria camada e recalcula urgência/fila dos pontos afetados. |
| `PATCH` | `/api/prefeitura/camadas/{id}/alternar-ativa` | `PREFEITURA` | Ativa/desativa camada e recalcula urgência/fila. |

---

## 3. Regras de Negócio e POO

### 3.1. Classificação de Urgência e Polimorfismo Espacial (`RF006`, `RS012`)
- [CamadaVulnerabilidade.java](saneamapa-app/src/main/java/br/edu/cesmac/saneamapa/domain/model/CamadaVulnerabilidade.java) valida:
  - `pesoUrgencia` entre `1` e `3`.
  - Se `categoria == VULNERABILIDADE_EPIDEMIOLOGICA`, exige obrigatoriamente `tipoGeometria == POLIGONO` (`RS012` — impede expor localização individual de pacientes).
  - Geometria `PONTO_RAIO` exige centro e raio > 0; `POLIGONO` exige coordenadas não vazias.
- O método `obterGeometria()` retorna a interface `GeometriaCamada` ([GeometriaPontoRaio](saneamapa-app/src/main/java/br/edu/cesmac/saneamapa/domain/model/GeometriaPontoRaio.java) ou [GeometriaPoligono](saneamapa-app/src/main/java/br/edu/cesmac/saneamapa/domain/model/GeometriaPoligono.java)), permitindo a `ServicoClassificacaoUrgenciaImpl` verificar contenção espacial polimorficamente.
- O ponto assume o **maior peso** dentre as camadas ativas que o contêm (`0 = NORMAL`, `1..2 = ELEVADA`, `3 = CRITICA`).

### 3.2. Cálculo da Pontuação da Fila (`RF005`)
- Interface `CalculadoraPontuacaoFila` implementada por [CalculadoraPontuacaoPadrao.java](saneamapa-app/src/main/java/br/edu/cesmac/saneamapa/domain/service/CalculadoraPontuacaoPadrao.java), com pesos configuráveis em `application.yml`:
  - `quantidadeRelatos * pesoRelatos` (padrão `6.0`)
  - `diasEspera * pesoDiasEspera` (padrão `1.5`)
  - `pesoUrgencia * pesoUrgenciaCamada` (padrão `15.0`)
  - `pontosNaFilaNoBairro * pesoDensidadeBairro` (padrão `2.0`)
  - `+ bonusIdentificado` (padrão `10.0` se houver ao menos um relato identificado)
- A pontuação é recalculada na criação/agrupamento de relato, na alteração de camadas e diariamente via rotina agendada (`PontoFilaService`).

### 3.3. Atribuição a Fiscal
- `Ponto.atribuirFiscal(Usuario)` exige usuário com `perfil == FISCAL` e `ativo == true`, e só permite pontos em `AGUARDANDO_VISTORIA` ou `EM_VISTORIA`.
- Ao atribuir, o ponto transiciona para `EM_VISTORIA`; ao retirar atribuição (`retirarAtribuicao()`), retorna a `AGUARDANDO_VISTORIA`.

---

## 4. Critérios de Aceitação Verificados em Testes

- `DominioPooTest.deveValidarRegraEpidemiologicaSomentePoligonoEContencaoPolimorfica`
- `SaneaMapaIntegracaoTest.fluxoCompletoMoradorAgrupamentoPrefeituraEFiscal` (consulta a indicadores, listagem e atribuição ao fiscal)
