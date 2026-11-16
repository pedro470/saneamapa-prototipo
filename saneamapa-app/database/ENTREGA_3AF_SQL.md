# Entrega 3AF — Estrutura de Banco de Dados PostgreSQL (SaneaMapa)

## 1. Introdução do Projeto
O **SaneaMapa** é um Sistema de Registro e Notificação de Saneamento Básico, concebido a partir de uma demanda social e educativa apresentada pela comunidade ao CESMAC. Seu objetivo é permitir que moradores registrem ocorrências de falhas de saneamento (casa sem ligação de água, água fora do padrão, rua sem rede de esgoto e esgoto a céu aberto) com evidências georreferenciadas (foto, vídeo e áudio com transcrição), confrontando a realidade de campo com os dados oficiais do Censo do IBGE ("saneado no papel versus saneado na prática"). O sistema conta com três perfis — **Morador**, **Fiscal** (vistoria em campo a até 100 m do ponto) e **Prefeitura** (gestão do mapa, fila priorizada por vulnerabilidade social, usuários, Censo e auditoria LGPD).

### Como executar
Os scripts completos estão na pasta `database/` e devem rodar nesta ordem, num banco PostgreSQL vazio chamado `saneamapa`:

| Ordem | Arquivo | Conteúdo |
|---|---|---|
| 1 | `01_ddl_4_integridades.sql` | Tabelas com as 4 integridades |
| 2 | `02_triggers.sql` | Triggers (seção 3) |
| 3 | `03_indices.sql` | Índices (seção 4) |
| 4 | `04_contas_permissoes.sql` | Contas e permissões (seção 5) |
| 5 | `05_views.sql` | Views (seção 6) |
| 6 | `06_seed_dados.sql` | Carga de demonstração (opcional) |

Exemplo com `psql`: `psql -U postgres -d saneamapa -f 01_ddl_4_integridades.sql`, repetindo para cada arquivo. Com Docker, `docker compose up --build` na raiz do repositório já executa os scripts 01 a 05.

---

## 2. Criação das Tabelas (DDL) mantendo todas as 4 Integridades
*(Trecho principal representando as 4 integridades: **Entidade**, **Domínio**, **Referencial** e **Semântica/Negócio**)*

```sql
-- Tabela PONTO: Entidade (PK/UNIQUE), Domínio (CHECKs de tipo/status/peso), Referencial (FK) e Semântica (datas e peso-nível)
CREATE TABLE IF NOT EXISTS ponto (
    id                          BIGSERIAL      PRIMARY KEY,                   -- Integridade de Entidade
    codigo_exibicao             VARCHAR(12)    NOT NULL UNIQUE,               -- Entidade (chave natural)
    tipo_ocorrencia             VARCHAR(30)    NOT NULL,
    latitude                    NUMERIC(10, 7) NOT NULL,
    longitude                   NUMERIC(10, 7) NOT NULL,
    trecho_rua                  VARCHAR(200)   NOT NULL,
    bairro                      VARCHAR(80)    NOT NULL,
    quantidade_relatos          INTEGER        NOT NULL DEFAULT 1,
    data_primeiro_relato        DATE           NOT NULL,
    data_ultimo_relato          DATE           NOT NULL,
    status                      VARCHAR(25)    NOT NULL DEFAULT 'AGUARDANDO_VISTORIA',
    reincidencia                BOOLEAN        NOT NULL DEFAULT FALSE,
    peso_urgencia               SMALLINT       NOT NULL DEFAULT 0,
    nivel_urgencia              VARCHAR(15)    NOT NULL DEFAULT 'NORMAL',
    motivo_urgencia             VARCHAR(300),
    fiscal_atribuido_id         BIGINT,
    posicao_manual_algum_relato BOOLEAN        NOT NULL DEFAULT FALSE,
    criado_em                   TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em               TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- mantido pelo trigger do ciclo de vida

    -- Integridade Referencial
    CONSTRAINT fk_ponto_fiscal
        FOREIGN KEY (fiscal_atribuido_id) REFERENCES usuario(id) ON DELETE SET NULL,

    -- Integridade de Domínio
    CONSTRAINT chk_ponto_tipo
        CHECK (tipo_ocorrencia IN ('CASA_SEM_AGUA', 'AGUA_FORA_PADRAO', 'RUA_SEM_ESGOTO', 'ESGOTO_CEU_ABERTO')),
    CONSTRAINT chk_ponto_status
        CHECK (status IN ('AGUARDANDO_VISTORIA', 'EM_VISTORIA', 'CONFIRMADO', 'NAO_CONFIRMADO')),
    CONSTRAINT chk_ponto_peso CHECK (peso_urgencia BETWEEN 0 AND 3),
    CONSTRAINT chk_ponto_nivel CHECK (nivel_urgencia IN ('NORMAL', 'ELEVADA', 'CRITICA')),

    -- Integridade Semântica (Regras de Negócio)
    CONSTRAINT chk_ponto_datas_coerentes CHECK (data_ultimo_relato >= data_primeiro_relato),
    CONSTRAINT chk_ponto_peso_nivel_coerentes CHECK (
        (peso_urgencia = 0 AND nivel_urgencia = 'NORMAL') OR
        (peso_urgencia IN (1, 2) AND nivel_urgencia = 'ELEVADA') OR
        (peso_urgencia = 3 AND nivel_urgencia = 'CRITICA')
    )
);

-- Tabela RELATO: Integridade Semântica LGPD (anônimo não guarda nome/telefone; identificado exige ambos válidos)
CREATE TABLE IF NOT EXISTS relato (
    id                 BIGSERIAL      PRIMARY KEY,
    ponto_id           BIGINT         NOT NULL REFERENCES ponto(id) ON DELETE RESTRICT,
    protocolo          VARCHAR(20)    NOT NULL UNIQUE,
    data_hora_registro TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo_ocorrencia    VARCHAR(30)    NOT NULL,
    identificado       BOOLEAN        NOT NULL DEFAULT TRUE,
    nome_morador       VARCHAR(120),
    telefone_morador   VARCHAR(15),
    latitude           NUMERIC(10, 7) NOT NULL CHECK (latitude BETWEEN -90.0 AND 90.0),
    longitude          NUMERIC(10, 7) NOT NULL CHECK (longitude BETWEEN -180.0 AND 180.0),
    precisao_metros    INTEGER        NOT NULL CHECK (precisao_metros >= 0),
    criado_em          TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- hora do servidor (limite de 24 h)

    CONSTRAINT chk_relato_lgpd_identificacao CHECK (
        (identificado = TRUE AND nome_morador IS NOT NULL AND telefone_morador ~ '^\(\d{2}\) \d{4,5}-\d{4}$')
        OR
        (identificado = FALSE AND nome_morador IS NULL AND telefone_morador IS NULL)
    )
);
```
*(O DDL completo das 11 tabelas encontra-se em `database/01_ddl_4_integridades.sql`: usuario, alteracao_usuario, ponto, relato, evidencia, item_fila_vistoria, camada_vulnerabilidade, vistoria, importacao_censo, setor_censitario e log_auditoria.)*

---

## 3. Criação de Trigger no Banco

```sql
-- Trigger de imutabilidade do log de auditoria LGPD (RF019 / RS011)
CREATE OR REPLACE FUNCTION fn_bloquear_mutacao_auditoria()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION
        'Operação % proibida na tabela log_auditoria: registros de auditoria LGPD são imutáveis.',
        TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_log_auditoria_imutavel
    BEFORE UPDATE OR DELETE ON log_auditoria
    FOR EACH ROW
    EXECUTE FUNCTION fn_bloquear_mutacao_auditoria();

-- Trigger do ciclo de vida do ponto (Seção 10.1 do Documento de Requisitos)
CREATE OR REPLACE FUNCTION fn_validar_ciclo_vida_ponto()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status IS DISTINCT FROM OLD.status THEN
        IF NOT (
            (OLD.status = 'AGUARDANDO_VISTORIA' AND NEW.status = 'EM_VISTORIA') OR
            (OLD.status = 'EM_VISTORIA'         AND NEW.status IN ('AGUARDANDO_VISTORIA', 'CONFIRMADO', 'NAO_CONFIRMADO')) OR
            (OLD.status = 'NAO_CONFIRMADO'      AND NEW.status = 'AGUARDANDO_VISTORIA')
        ) THEN
            RAISE EXCEPTION 'Transição de status inválida: % -> %', OLD.status, NEW.status;
        END IF;
    END IF;
    NEW.atualizado_em := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ponto_ciclo_vida
    BEFORE UPDATE ON ponto
    FOR EACH ROW
    EXECUTE FUNCTION fn_validar_ciclo_vida_ponto();
```

---

## 4. Criação de Campos Indexados

```sql
-- Índice parcial para validar rapidamente o limite de 5 relatos por telefone em 24h (RF001 / RS001)
CREATE INDEX IF NOT EXISTS idx_relato_telefone_24h
    ON relato (telefone_morador, criado_em DESC)
    WHERE telefone_morador IS NOT NULL;

-- Índice para ordenação da fila de vistoria por pontuação (RF005)
CREATE INDEX IF NOT EXISTS idx_item_fila_pontuacao_desc
    ON item_fila_vistoria (pontuacao DESC, data_entrada ASC);

-- Índice composto para consulta de mapa por status e tipo de ocorrência (RF010)
CREATE INDEX IF NOT EXISTS idx_ponto_status_tipo
    ON ponto (status, tipo_ocorrencia);
```

---

## 5. Criação de Contas e Permissões

```sql
CREATE ROLE role_morador NOLOGIN;
CREATE ROLE role_fiscal NOLOGIN;
CREATE ROLE role_prefeitura NOLOGIN;

CREATE USER saneamapa_morador    WITH PASSWORD 'SaneaMorador#2026';
CREATE USER saneamapa_fiscal     WITH PASSWORD 'SaneaFiscal#2026';
CREATE USER saneamapa_prefeitura WITH PASSWORD 'SaneaPref#2026';

GRANT role_morador    TO saneamapa_morador;
GRANT role_fiscal     TO saneamapa_fiscal;
GRANT role_prefeitura TO saneamapa_prefeitura;

REVOKE ALL ON ALL TABLES IN SCHEMA public FROM PUBLIC;

-- Morador: apenas insere relatos/evidências, sem acesso a dados pessoais de terceiros nem auditoria
GRANT INSERT, SELECT ON TABLE ponto, relato, evidencia TO role_morador;
REVOKE SELECT (nome_morador, telefone_morador) ON TABLE relato FROM role_morador;

-- Fiscal: consulta pontos/relatos/evidências, registra vistoria e edita desfoque/transcrição
GRANT SELECT ON TABLE ponto, relato, evidencia, item_fila_vistoria, camada_vulnerabilidade TO role_fiscal;
GRANT INSERT, SELECT ON TABLE vistoria, log_auditoria TO role_fiscal;
REVOKE UPDATE, DELETE ON TABLE log_auditoria FROM role_fiscal;

-- Prefeitura: gestão do painel e usuários, sem permissão de alterar ou excluir logs de auditoria
GRANT SELECT, INSERT, UPDATE ON TABLE usuario, ponto, relato, evidencia, item_fila_vistoria, camada_vulnerabilidade, importacao_censo, setor_censitario TO role_prefeitura;
GRANT SELECT, INSERT ON TABLE log_auditoria TO role_prefeitura;
REVOKE UPDATE, DELETE ON TABLE log_auditoria FROM role_prefeitura;
```

---

## 6. Criação de View

```sql
-- View da fila de vistoria ordenada por urgência/pontuação (RF005)
CREATE OR REPLACE VIEW vw_fila_vistoria AS
SELECT
    ROW_NUMBER() OVER (ORDER BY f.pontuacao DESC, f.data_entrada ASC) AS posicao_fila,
    p.id AS ponto_id,
    p.codigo_exibicao,
    p.tipo_ocorrencia,
    p.trecho_rua,
    p.bairro,
    p.quantidade_relatos,
    (CURRENT_DATE - p.data_primeiro_relato) AS dias_espera,
    p.nivel_urgencia,
    f.pontuacao,
    p.status,
    COALESCE(u.nome_completo, 'Sem atribuição') AS fiscal_nome
FROM item_fila_vistoria f
JOIN ponto p ON p.id = f.ponto_id
LEFT JOIN usuario u ON u.id = f.fiscal_atribuido_id
WHERE p.status IN ('AGUARDANDO_VISTORIA', 'EM_VISTORIA')
ORDER BY f.pontuacao DESC, f.data_entrada ASC;

-- View LGPD para exibição pública sem dados pessoais nem coordenada exata (RF002 / RS003)
CREATE OR REPLACE VIEW vw_mapa_publico_lgpd AS
SELECT id, codigo_exibicao, tipo_ocorrencia, trecho_rua, bairro, quantidade_relatos, status, reincidencia
FROM ponto;
```

---

## 7. Informações de Uso de Inteligência Artificial (Avaliação)
- **Modelo de IA utilizado:** Gemini
- **Prompt utilizado:**
  > "usando como base essa imagem, a documentação e o protótipo, faça a aplicação do SaneaMapa utilizando JAVA SPRING BOOT no Backend (seguindo os pilares de POO), PostgreSQL como Banco de dados."
