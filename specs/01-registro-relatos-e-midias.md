# Spec 01 — Registro de Relatos, Mídias LGPD e Agrupamento Espacial

**Requisitos cobertos:** `RF001`, `RF002`, `RF003`, `RF004`, `RS001`, `RS002`, `RS003`, `RS005`, `RS006`, `RS007`  
**Ator principal:** Morador (público, sem autenticação) e Sistema  
**Interface:** [morador.html](saneamapa-app/src/main/resources/static/morador.html)

---

## 1. Objetivo

Permitir que qualquer morador registre ocorrências de saneamento com evidências georreferenciadas em até três etapas principais, garantindo proteção de dados pessoais (LGPD), controle contra abuso/spam e consolidação automática de relatos próximos em um único ponto.

---

## 2. Endpoints e Contratos

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/api/morador/midias` | Público | Upload multipart (`arquivo`, `tipo`, `duracaoSegundos`). Retorna caminho `/api/midias/...` e metadados. |
| `POST` | `/api/morador/relatos` | Público | Registra relato (`CriarRelatoRequest`) e retorna protocolo, código do ponto e flags (`agrupadoEmPontoExistente`, `prioridadeReduzida`). |
| `GET` | `/api/publico/pontos` | Público | Lista pontos resumidos em conformidade LGPD (sem coordenada exata, contatos, áudios ou urgência). |
| `GET` | `/api/publico/config` | Público | Retorna motor do mapa (`openstreetmap`) e centro padrão (`Maceió, AL`). |
| `GET` | `/api/midias/{nomeArquivo}` | `FISCAL` ou `PREFEITURA` | Serve mídias armazenadas apenas para equipe autenticada (bloqueado com HTTP 401 para o público). |

---

## 3. Regras de Negócio e POO

### 3.1. Identificação e LGPD (`Relato`)
- Em [Relato.java](saneamapa-app/src/main/java/br/edu/cesmac/saneamapa/domain/model/Relato.java):
  - Quando `identificado = true`, exige `nomeMorador` não vazio e `telefoneMorador` no padrão `(xx) xxxxx-xxxx` ou `(xx) xxxx-xxxx` (`CpfUtils.isTelefoneValido`).
  - Quando `identificado = false` (anônimo), força `nomeMorador = null` e `telefoneMorador = null`, e o relato recebe `prioridadeReduzida = true` na fila (`RS001`).
  - No banco, a constraint `chk_relato_lgpd_identificacao` ([01_ddl_4_integridades.sql](saneamapa-app/database/01_ddl_4_integridades.sql)) reforça essa invariante.

### 3.2. Limite Anti-Abuso em 24h (`RS001`)
- `RelatoService.registrarRelato` consulta relatos recentes pelo `telefoneMorador` usando a hora do servidor (`criado_em` nas últimas 24h), recusando a partir do 6º relato (`LIMITE_RELATOS_24H_TELEFONE = 5`).
- No PostgreSQL, o trigger `trg_relato_limite_24h` ([02_triggers.sql](saneamapa-app/database/02_triggers.sql)) e o índice parcial `idx_relato_telefone_24h` ([03_indices.sql](saneamapa-app/database/03_indices.sql)) garantem a mesma regra na camada de dados.

### 3.3. Polimorfismo de Evidências (`Evidencia`)
- A hierarquia `Evidencia` → `EvidenciaFoto`, `EvidenciaVideo`, `EvidenciaAudio` ([Evidencia.java](saneamapa-app/src/main/java/br/edu/cesmac/saneamapa/domain/model/Evidencia.java)) aplica polimorfismo via `Relato.adicionarEvidencia(Evidencia)`:
  - `validarRegrasCaptura()`: rejeita origem incompatível (ex.: foto/vídeo de `MICROFONE`, áudio de `CAMERA`) e valida duração máxima (`VIDEO <= 30s`, `AUDIO <= 60s`).
  - `aplicarTratamentoInicial()`: marca desfoque (`EvidenciaFoto`), desfoque + remoção de som (`EvidenciaVideo`) e transcrição por IA (`EvidenciaAudio` via `ServicoTranscricaoAudio`).
  - `verificarDivergenciaGaleria()`: se `origem == GALERIA` e a data diferir > 7 dias ou a distância > 500 m em relação ao relato, marca `evidenciaDivergente = true` (`RS002`).

### 3.4. Agrupamento Espacial em 50 metros (`RF004`)
- `RelatoService` busca pontos do mesmo `TipoOcorrencia` a até **50,0 m** (Haversine via `GeoUtils`).
- Se existir ponto dentro do raio, vincula ao mais próximo via `Ponto.registrarVinculoNovoRelato`:
  - Incrementa `quantidadeRelatos` e atualiza `dataUltimoRelato`.
  - Se o ponto estiver `CONFIRMADO`, marca `reincidencia = true`.
  - Se o ponto estiver `NAO_CONFIRMADO`, retorna para `AGUARDANDO_VISTORIA` e limpa `fiscalAtribuido`.
- Se não existir ponto no raio, cria um novo `Ponto` (`AGUARDANDO_VISTORIA`) com `codigoExibicao` único (`P-xxxx`).

---

## 4. Critérios de Aceitação Verificados em Testes

- `DominioPooTest.deveValidarPolimorfismoDeEvidenciasETratamentoLgpd`
- `SaneaMapaIntegracaoTest.fluxoCompletoMoradorAgrupamentoPrefeituraEFiscal` (criação de ponto + agrupamento no 2º relato + ocultação LGPD em `/api/publico/pontos`)
- `SaneaMapaIntegracaoTest.uploadRealDeMidiasEConsultaConfiguracao` (upload, recusa de vídeo > 30s, bloqueio público 401 e acesso autenticado 200)
