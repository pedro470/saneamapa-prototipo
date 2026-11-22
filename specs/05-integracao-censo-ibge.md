# Spec 05 — Integração e Rollback do Censo IBGE

**Requisitos cobertos:** `RF020`, `RF010`, `RF019`, `RS013`  
**Ator:** Prefeitura (`ROLE_PREFEITURA`) e Sistema

---

## 1. Objetivo

Permitir que a Prefeitura importe dados oficiais dos setores censitários do IBGE (cobertura de rede de água e rede de esgoto) em formato CSV ou GeoJSON, exiba essa camada no mapa para confronto com os relatos ("saneado no papel vs. saneado na prática") e suporte rollback seguro para importações anteriores.

---

## 2. Endpoints e Contratos

| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| `GET` | `/api/prefeitura/censo/importacoes` | `PREFEITURA` | Lista histórico de importações do Censo, indicando qual está `ativa`. |
| `GET` | `/api/prefeitura/censo/setores-ativos` | `PREFEITURA` | Retorna polígonos e percentuais de água/esgoto dos setores da importação ativa. |
| `POST` | `/api/prefeitura/censo/importar` | `PREFEITURA` | Recebe arquivo multipart (`arquivo`) `.csv` ou `.geojson`, valida e ativa nova versão. |
| `POST` | `/api/prefeitura/censo/importacoes/{id}/voltar` | `PREFEITURA` | Reativa uma importação anterior (rollback), desativando a atual. |

---

## 3. Regras de Negócio e Segurança (`CensoService` / `RS013`)

Em [CensoService.java](saneamapa-app/src/main/java/br/edu/cesmac/saneamapa/service/CensoService.java):

1. **Formatos aceitos:** `CSV` (separador `;` ou `,`) e `GEOJSON` (FeatureCollection com polígonos e propriedades de setor).
2. **Validação estrita antes de efetivar:**
   - `codigoSetor` deve conter exatamente **15 dígitos numéricos** (`^\d{15}$` — também garantido pela constraint `chk_setor_codigo_15_digitos`).
   - Não permite código de setor duplicado dentro da mesma importação (`uq_setor_por_importacao`).
   - `pctRedeAgua` e `pctRedeEsgoto` devem estar entre `0.00` e `100.00`.
   - Se houver qualquer erro de formato ou regra, a importação falha de forma atômica, retorna `sucesso = false` com a lista de erros detalhada (`erros`) e **não substitui** a importação ativa.
3. **Versionamento e Rollback (`RS013`):**
   - Importações anteriores são preservadas no banco (`importacao_censo` e `setor_censitario`).
   - Ao concluir importação válida ou executar `voltarParaImportacao`, apenas uma importação fica com `ativa = true`, e o evento `IMPORTACAO_CENSO` ou `ROLLBACK_CENSO` é gravado na auditoria (`log_auditoria`).
4. **Cruzamento no detalhe do ponto:**
   - Ao abrir o detalhe de um ponto no painel, o sistema verifica se o ponto está contido em algum `SetorCensitario` ativo (`GeoUtils.pontoDentroDoPoligono`) e exibe a cobertura oficial de água ou esgoto do IBGE correspondente ao tipo de ocorrência.

---

## 4. Critérios de Aceitação Verificados em Testes

- `SaneaMapaIntegracaoTest.fluxoCompletoMoradorAgrupamentoPrefeituraEFiscal` (importação de CSV inválido com código de 14 dígitos rejeitada mantendo os dados ativos íntegros)
