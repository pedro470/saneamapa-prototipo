# Spec 03 — Vistoria em Campo pelo Fiscal

**Requisitos cobertos:** `RF007`, `RF003` (edição de desfoque/transcrição), `RS006`, `RS008`, `RS009`  
**Ator:** Fiscal autenticado (`ROLE_FISCAL`)  
**Interface:** [fiscal.html](saneamapa-app/src/main/resources/static/fiscal.html)

---

## 1. Objetivo

Permitir que o fiscal realize a validação técnica em campo exclusivamente nos pontos atribuídos a ele, garantindo presença física no local por GPS, evidências originais de câmera e atualização do ciclo de vida do ponto.

---

## 2. Endpoints e Contratos

| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| `GET` | `/api/fiscal/pontos` | `FISCAL` | Lista apenas os pontos atribuídos ao fiscal autenticado. |
| `GET` | `/api/fiscal/pontos/{pontoId}` | `FISCAL` | Detalhe do ponto com relatos, evidências e coordenada exata (somente se atribuído ao fiscal). |
| `POST` | `/api/fiscal/pontos/{pontoId}/contatos` | `FISCAL` | Revela contatos dos moradores com registro em `log_auditoria`. |
| `POST` | `/api/fiscal/evidencias/{evidenciaId}/audio` | `FISCAL` | Libera reprodução de áudio com registro em `log_auditoria`. |
| `POST` | `/api/fiscal/midias` | `FISCAL` | Upload de foto/vídeo da vistoria (rejeita origem diferente de `CAMERA`). |
| `POST` | `/api/fiscal/pontos/{pontoId}/vistoria` | `FISCAL` | Registra vistoria (`RegistrarVistoriaRequest`) e transiciona o status do ponto. |
| `POST` | `/api/fiscal/evidencias/{evidenciaId}/desfoque` | `FISCAL` | Registra nova região de desfoque manual em `EvidenciaFoto` e audita `EDICAO_FOTO`. |
| `PUT` | `/api/fiscal/evidencias/{evidenciaId}/transcricao` | `FISCAL` | Corrige transcrição de `EvidenciaAudio` e audita `CORRECAO_TRANSCRICAO`. |

---

## 3. Regras de Negócio e Segurança (`Vistoria` / `RS008`)

Em [Vistoria.java](saneamapa-app/src/main/java/br/edu/cesmac/saneamapa/domain/model/Vistoria.java) e [VistoriaService.java](saneamapa-app/src/main/java/br/edu/cesmac/saneamapa/service/VistoriaService.java):

1. **Restrição de atribuição:** O ponto deve estar atribuído ao fiscal autenticado (`AcessoNegadoException` / HTTP 403 caso contrário).
2. **Raio máximo de 100 metros (`RS008`):**
   - A distância entre `(ponto.latitude, ponto.longitude)` e `(latitudeFiscal, longitudeFiscal)` é calculada no servidor via `GeoUtils.calcularDistanciaMetros`.
   - Mesmo que o fiscal envie `novaLatitudePonto` / `novaLongitudePonto`, o cálculo é feito contra a coordenada **originalmente gravada** no ponto, evitando fraude.
   - Distância > `100.0 m` lança `DistanciaVistoriaExcedidaException` (HTTP 422) e também é barrada pela constraint `chk_vistoria_raio_100m` no PostgreSQL.
3. **Captura exclusiva pela câmera:**
   - Qualquer item em `origensMidia` diferente de `CAMERA` é rejeitado.
4. **Evidência obrigatória para confirmação:**
   - Se `resultado == CONFIRMADO`, `midiasCamera` não pode ser vazio (validado em `Vistoria` e pela constraint `chk_vistoria_confirmado_exige_midia`).
5. **Motivo da não confirmação (RF007, nota 5; TBD-26):**
   - Se `resultado == NAO_CONFIRMADO`, `motivoNaoConfirmacao` é obrigatório: `NAO_ENCONTRADO` (problema não encontrado no local) ou `FORA_DO_ESCOPO` (problema existe, mas não é de saneamento, como poço, vazamento ou lixo). Na vistoria confirmada, o motivo é recusado.
   - Validado em `Vistoria` e no banco pelas constraints `chk_vistoria_motivo` e `chk_vistoria_motivo_so_nao_confirmado`. O motivo entra no detalhe do evento `MUDANCA_STATUS_PONTO`, que a Prefeitura vê no histórico do ponto.
6. **Transição de status e remoção da fila:**
   - `Ponto.aplicarResultadoVistoria` atualiza tipo/coordenadas corrigidas e transiciona `EM_VISTORIA` → `CONFIRMADO` ou `NAO_CONFIRMADO`.
   - `VistoriaService` remove o `ItemFilaVistoria` da fila e registra `MUDANCA_STATUS_PONTO` em `log_auditoria`.

---

## 4. Critérios de Aceitação Verificados em Testes

- `DominioPooTest.deveBloquearVistoriaForaDe100MetrosEExigirCamera` (valida distância > 100 m, tentativa de correção forjada e rejeição de `GALERIA`)
- `DominioPooTest.deveExigirMotivoSoNaVistoriaNaoConfirmada` (motivo obrigatório no "não confirmado" e recusado no "confirmado")
- `SaneaMapaIntegracaoTest.fluxoCompletoMoradorAgrupamentoPrefeituraEFiscal` (vistoria rejeitada a > 100 m com HTTP 422 e confirmada dentro de 100 m com HTTP 200)
