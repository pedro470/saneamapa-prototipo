-- ============================================================================
-- SaneaMapa — DDL PostgreSQL com as 4 Integridades (Entidade, Domínio, Referencial, Semântica)
-- Baseado no Documento de Requisitos v6 e no Protótipo SaneaMapa
-- ============================================================================

-- Limpeza opcional para reexecução em ambiente de desenvolvimento
-- DROP TABLE IF EXISTS log_auditoria, setor_censitario, importacao_censo, vistoria,
--   item_fila_vistoria, camada_vulnerabilidade, evidencia, relato, ponto,
--   alteracao_usuario, usuario CASCADE;

-- 1. USUÁRIO DA EQUIPE INTERNA (RF013, RF018, RNF16)
CREATE TABLE IF NOT EXISTS usuario (
    id                BIGSERIAL     PRIMARY KEY,                              -- Integridade de Entidade
    cpf               VARCHAR(14)   NOT NULL UNIQUE,                          -- Entidade (chave natural única)
    nome_completo     VARCHAR(120)  NOT NULL,                                 -- Domínio
    email             VARCHAR(200)  NOT NULL UNIQUE,                          -- Entidade (chave natural única)
    celular           VARCHAR(15)   NOT NULL,                                 -- Domínio
    senha_hash        VARCHAR(255)  NOT NULL,                                 -- Domínio
    perfil            VARCHAR(20)   NOT NULL,                                 -- Domínio
    ativo             BOOLEAN       NOT NULL DEFAULT TRUE,                    -- Domínio
    tentativas_falhas SMALLINT      NOT NULL DEFAULT 0,                       -- Domínio
    bloqueado_ate     TIMESTAMPTZ,
    criado_em         TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Integridade de Domínio
    CONSTRAINT chk_usuario_cpf_formato
        CHECK (cpf ~ '^\d{3}\.\d{3}\.\d{3}-\d{2}$'),
    CONSTRAINT chk_usuario_email_formato
        CHECK (email LIKE '%@%.%'),
    CONSTRAINT chk_usuario_celular_formato
        CHECK (celular ~ '^\(\d{2}\) \d{4,5}-\d{4}$'),
    CONSTRAINT chk_usuario_perfil
        CHECK (perfil IN ('FISCAL', 'PREFEITURA')),
    CONSTRAINT chk_usuario_tentativas
        CHECK (tentativas_falhas >= 0 AND tentativas_falhas <= 10),
    CONSTRAINT chk_usuario_nome_nao_vazio
        CHECK (length(trim(nome_completo)) >= 3)
);

-- 2. HISTÓRICO DE ALTERAÇÕES DE USUÁRIO (RF013, RF019)
CREATE TABLE IF NOT EXISTS alteracao_usuario (
    id               BIGSERIAL    PRIMARY KEY,                                -- Entidade
    usuario_alvo_id  BIGINT       NOT NULL,                                   -- Referencial
    autor_id         BIGINT       NOT NULL,                                   -- Referencial
    campo_alterado   VARCHAR(60)  NOT NULL,                                   -- Domínio
    valor_anterior   VARCHAR(255),
    valor_novo       VARCHAR(255),
    data_hora        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Integridade Referencial
    CONSTRAINT fk_alt_usuario_alvo
        FOREIGN KEY (usuario_alvo_id) REFERENCES usuario(id) ON DELETE RESTRICT,
    CONSTRAINT fk_alt_usuario_autor
        FOREIGN KEY (autor_id) REFERENCES usuario(id) ON DELETE RESTRICT
);

-- 3. PONTO DE OCORRÊNCIA AGRUPADA (RF004, RF005, RF006, RF007)
CREATE TABLE IF NOT EXISTS ponto (
    id                          BIGSERIAL      PRIMARY KEY,                   -- Entidade
    codigo_exibicao             VARCHAR(12)    NOT NULL UNIQUE,               -- Entidade (ex: P-0142)
    tipo_ocorrencia             VARCHAR(30)    NOT NULL,                      -- Domínio
    latitude                    NUMERIC(10, 7) NOT NULL,                      -- Domínio
    longitude                   NUMERIC(10, 7) NOT NULL,                      -- Domínio
    trecho_rua                  VARCHAR(200)   NOT NULL,                      -- Domínio (LGPD: exposição pública)
    bairro                      VARCHAR(80)    NOT NULL,                      -- Domínio
    quantidade_relatos          INTEGER        NOT NULL DEFAULT 1,            -- Domínio
    data_primeiro_relato        DATE           NOT NULL,                      -- Domínio
    data_ultimo_relato          DATE           NOT NULL,                      -- Domínio
    status                      VARCHAR(25)    NOT NULL DEFAULT 'AGUARDANDO_VISTORIA',
    reincidencia                BOOLEAN        NOT NULL DEFAULT FALSE,
    peso_urgencia               SMALLINT       NOT NULL DEFAULT 0,
    nivel_urgencia              VARCHAR(15)    NOT NULL DEFAULT 'NORMAL',
    motivo_urgencia             VARCHAR(300),
    fiscal_atribuido_id         BIGINT,                                       -- Referencial
    posicao_manual_algum_relato BOOLEAN        NOT NULL DEFAULT FALSE,
    criado_em                   TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em               TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Integridade Referencial
    CONSTRAINT fk_ponto_fiscal
        FOREIGN KEY (fiscal_atribuido_id) REFERENCES usuario(id) ON DELETE SET NULL,

    -- Integridade de Domínio
    CONSTRAINT chk_ponto_tipo
        CHECK (tipo_ocorrencia IN ('CASA_SEM_AGUA', 'AGUA_FORA_PADRAO', 'RUA_SEM_ESGOTO', 'ESGOTO_CEU_ABERTO')),
    CONSTRAINT chk_ponto_lat
        CHECK (latitude BETWEEN -90.0 AND 90.0),
    CONSTRAINT chk_ponto_lon
        CHECK (longitude BETWEEN -180.0 AND 180.0),
    CONSTRAINT chk_ponto_qtd_relatos
        CHECK (quantidade_relatos >= 0),
    CONSTRAINT chk_ponto_status
        CHECK (status IN ('AGUARDANDO_VISTORIA', 'EM_VISTORIA', 'CONFIRMADO', 'NAO_CONFIRMADO')),
    CONSTRAINT chk_ponto_peso
        CHECK (peso_urgencia BETWEEN 0 AND 3),
    CONSTRAINT chk_ponto_nivel
        CHECK (nivel_urgencia IN ('NORMAL', 'ELEVADA', 'CRITICA')),

    -- Integridade Semântica (Regras de Negócio)
    CONSTRAINT chk_ponto_datas_coerentes
        CHECK (data_ultimo_relato >= data_primeiro_relato),
    CONSTRAINT chk_ponto_peso_nivel_coerentes
        CHECK (
            (peso_urgencia = 0 AND nivel_urgencia = 'NORMAL') OR
            (peso_urgencia IN (1, 2) AND nivel_urgencia = 'ELEVADA') OR
            (peso_urgencia = 3 AND nivel_urgencia = 'CRITICA')
        )
);

-- 4. RELATO DO MORADOR (RF001, RF002, LGPD)
CREATE TABLE IF NOT EXISTS relato (
    id                 BIGSERIAL      PRIMARY KEY,                            -- Entidade
    ponto_id           BIGINT         NOT NULL,                               -- Referencial
    protocolo          VARCHAR(20)    NOT NULL UNIQUE,                        -- Entidade
    data_hora_registro TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo_ocorrencia    VARCHAR(30)    NOT NULL,
    descricao          VARCHAR(2350),
    identificado       BOOLEAN        NOT NULL DEFAULT TRUE,
    nome_morador       VARCHAR(120),
    telefone_morador   VARCHAR(15),
    situacao_envio     VARCHAR(15)    NOT NULL DEFAULT 'ENVIADO',
    latitude           NUMERIC(10, 7) NOT NULL,
    longitude          NUMERIC(10, 7) NOT NULL,
    precisao_metros    INTEGER        NOT NULL,
    logradouro         VARCHAR(150),
    bairro             VARCHAR(80),
    ponto_referencia   VARCHAR(100),
    posicao_manual     BOOLEAN        NOT NULL DEFAULT FALSE,
    criado_em          TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Integridade Referencial
    CONSTRAINT fk_relato_ponto
        FOREIGN KEY (ponto_id) REFERENCES ponto(id) ON DELETE RESTRICT,

    -- Integridade de Domínio
    CONSTRAINT chk_relato_tipo
        CHECK (tipo_ocorrencia IN ('CASA_SEM_AGUA', 'AGUA_FORA_PADRAO', 'RUA_SEM_ESGOTO', 'ESGOTO_CEU_ABERTO')),
    CONSTRAINT chk_relato_situacao_envio
        CHECK (situacao_envio IN ('PENDENTE', 'ENVIADO')),
    CONSTRAINT chk_relato_lat
        CHECK (latitude BETWEEN -90.0 AND 90.0),
    CONSTRAINT chk_relato_lon
        CHECK (longitude BETWEEN -180.0 AND 180.0),
    CONSTRAINT chk_relato_precisao
        CHECK (precisao_metros >= 0),

    -- Integridade Semântica (LGPD RF001: anônimo NÃO armazena nome/telefone; identificado exige ambos válidos)
    CONSTRAINT chk_relato_lgpd_identificacao
        CHECK (
            (identificado = TRUE
             AND nome_morador IS NOT NULL AND length(trim(nome_morador)) > 0
             AND telefone_morador IS NOT NULL AND telefone_morador ~ '^\(\d{2}\) \d{4,5}-\d{4}$')
            OR
            (identificado = FALSE AND nome_morador IS NULL AND telefone_morador IS NULL)
        )
);

-- 5. EVIDÊNCIAS DO RELATO (RF003, RS002, RS006, RS007)
CREATE TABLE IF NOT EXISTS evidencia (
    id                    BIGSERIAL      PRIMARY KEY,                         -- Entidade
    relato_id             BIGINT         NOT NULL,                            -- Referencial
    tipo_evidencia        VARCHAR(10)    NOT NULL,                            -- Discriminador POO (FOTO, VIDEO, AUDIO)
    origem                VARCHAR(15)    NOT NULL,
    evidencia_divergente  BOOLEAN        NOT NULL DEFAULT FALSE,
    arquivo_uso_caminho   VARCHAR(300)   NOT NULL,
    data_hora_captura     TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    latitude_captura      NUMERIC(10, 7),
    longitude_captura     NUMERIC(10, 7),
    duracao_segundos      INTEGER,
    desfoque_aplicado     BOOLEAN        NOT NULL DEFAULT FALSE,
    regiao_desfoque_manual VARCHAR(200),
    som_removido          BOOLEAN        NOT NULL DEFAULT FALSE,
    transcricao           VARCHAR(2000),
    criado_em             TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em         TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Integridade Referencial
    CONSTRAINT fk_evidencia_relato
        FOREIGN KEY (relato_id) REFERENCES relato(id) ON DELETE CASCADE,

    -- Integridade de Domínio
    CONSTRAINT chk_evidencia_tipo
        CHECK (tipo_evidencia IN ('FOTO', 'VIDEO', 'AUDIO')),
    CONSTRAINT chk_evidencia_origem
        CHECK (origem IN ('CAMERA', 'MICROFONE', 'GALERIA')),

    -- Integridade Semântica (RF001 nota 3: vídeo <= 30s, áudio <= 60s; origem compatível com tipo)
    CONSTRAINT chk_evidencia_duracao_limite
        CHECK (
            (tipo_evidencia = 'FOTO') OR
            (tipo_evidencia = 'VIDEO' AND (duracao_segundos IS NULL OR (duracao_segundos > 0 AND duracao_segundos <= 30))) OR
            (tipo_evidencia = 'AUDIO' AND (duracao_segundos IS NULL OR (duracao_segundos > 0 AND duracao_segundos <= 60)))
        ),
    CONSTRAINT chk_evidencia_origem_por_tipo
        CHECK (
            (tipo_evidencia IN ('FOTO', 'VIDEO') AND origem IN ('CAMERA', 'GALERIA')) OR
            (tipo_evidencia = 'AUDIO' AND origem IN ('MICROFONE', 'GALERIA'))
        )
);

-- 6. FILA DE VISTORIA (RF005)
CREATE TABLE IF NOT EXISTS item_fila_vistoria (
    id                  BIGSERIAL     PRIMARY KEY,                            -- Entidade
    ponto_id            BIGINT        NOT NULL UNIQUE,                        -- Entidade + Referencial (1:1 com ponto ativo)
    data_entrada        TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pontuacao           NUMERIC(8, 2) NOT NULL DEFAULT 0.00,
    fiscal_atribuido_id BIGINT,
    atualizado_em       TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Integridade Referencial
    CONSTRAINT fk_fila_ponto
        FOREIGN KEY (ponto_id) REFERENCES ponto(id) ON DELETE CASCADE,
    CONSTRAINT fk_fila_fiscal
        FOREIGN KEY (fiscal_atribuido_id) REFERENCES usuario(id) ON DELETE SET NULL,

    -- Integridade de Domínio
    CONSTRAINT chk_fila_pontuacao
        CHECK (pontuacao >= 0)
);

-- 7. CAMADAS DE VULNERABILIDADE SOCIAL (RF006, RS012)
CREATE TABLE IF NOT EXISTS camada_vulnerabilidade (
    id                   BIGSERIAL      PRIMARY KEY,                          -- Entidade
    nome                 VARCHAR(120)   NOT NULL,
    categoria            VARCHAR(40)    NOT NULL,
    tipo_geometria       VARCHAR(15)    NOT NULL,
    centro_latitude      NUMERIC(10, 7),
    centro_longitude     NUMERIC(10, 7),
    raio_metros          INTEGER,
    poligono_coordenadas TEXT,
    peso_urgencia        SMALLINT       NOT NULL,
    fonte_dado           VARCHAR(200)   NOT NULL,
    data_atualizacao     DATE           NOT NULL,
    ativa                BOOLEAN        NOT NULL DEFAULT TRUE,
    criado_em            TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Integridade de Domínio
    CONSTRAINT chk_camada_categoria
        CHECK (categoria IN (
            'PUBLICO_INFANTIL_ESCOLAR',
            'IDOSOS_CUIDADOS',
            'GESTANTES_RECEM_NASCIDOS',
            'COMUNIDADES_TRADICIONAIS_VULNERAVEIS',
            'VULNERABILIDADE_EPIDEMIOLOGICA'
        )),
    CONSTRAINT chk_camada_tipo_geometria
        CHECK (tipo_geometria IN ('PONTO_RAIO', 'POLIGONO')),
    CONSTRAINT chk_camada_peso
        CHECK (peso_urgencia BETWEEN 1 AND 3),

    -- Integridade Semântica (RS012 / RF006: categoria epidemiológica só aceita polígono; campos geométricos coerentes)
    CONSTRAINT chk_camada_epidemio_so_poligono
        CHECK (categoria <> 'VULNERABILIDADE_EPIDEMIOLOGICA' OR tipo_geometria = 'POLIGONO'),
    CONSTRAINT chk_camada_geometria_valida
        CHECK (
            (tipo_geometria = 'PONTO_RAIO' AND centro_latitude IS NOT NULL AND centro_longitude IS NOT NULL AND raio_metros IS NOT NULL AND raio_metros > 0)
            OR
            (tipo_geometria = 'POLIGONO' AND poligono_coordenadas IS NOT NULL AND length(trim(poligono_coordenadas)) > 0)
        )
);

-- 8. VISTORIA EM CAMPO (RF007, RS008)
CREATE TABLE IF NOT EXISTS vistoria (
    id                        BIGSERIAL      PRIMARY KEY,                     -- Entidade
    ponto_id                  BIGINT         NOT NULL,                        -- Referencial
    fiscal_id                 BIGINT         NOT NULL,                        -- Referencial
    data_hora_vistoria        TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resultado                 VARCHAR(20)    NOT NULL,
    tipo_ocorrencia_validado  VARCHAR(30)    NOT NULL,
    latitude_ponto            NUMERIC(10, 7) NOT NULL,
    longitude_ponto           NUMERIC(10, 7) NOT NULL,
    latitude_fiscal           NUMERIC(10, 7) NOT NULL,
    longitude_fiscal          NUMERIC(10, 7) NOT NULL,
    distancia_fiscal_metros   NUMERIC(8, 2)  NOT NULL,
    observacoes               VARCHAR(1000),
    evidencia_camera_caminhos TEXT,

    -- Integridade Referencial
    CONSTRAINT fk_vistoria_ponto
        FOREIGN KEY (ponto_id) REFERENCES ponto(id) ON DELETE RESTRICT,
    CONSTRAINT fk_vistoria_fiscal
        FOREIGN KEY (fiscal_id) REFERENCES usuario(id) ON DELETE RESTRICT,

    -- Integridade de Domínio
    CONSTRAINT chk_vistoria_resultado
        CHECK (resultado IN ('CONFIRMADO', 'NAO_CONFIRMADO')),
    CONSTRAINT chk_vistoria_tipo
        CHECK (tipo_ocorrencia_validado IN ('CASA_SEM_AGUA', 'AGUA_FORA_PADRAO', 'RUA_SEM_ESGOTO', 'ESGOTO_CEU_ABERTO')),

    -- Integridade Semântica (RF007 / RS008: fiscal a até 100 m e mídia obrigatória na confirmação)
    CONSTRAINT chk_vistoria_raio_100m
        CHECK (distancia_fiscal_metros >= 0 AND distancia_fiscal_metros <= 100.00),
    CONSTRAINT chk_vistoria_confirmado_exige_midia
        CHECK (resultado <> 'CONFIRMADO' OR (evidencia_camera_caminhos IS NOT NULL AND length(trim(evidencia_camera_caminhos)) > 0))
);

-- 9. IMPORTAÇÃO E SETORES CENSITÁRIOS DO IBGE (RF020, RS013)
CREATE TABLE IF NOT EXISTS importacao_censo (
    id                 BIGSERIAL    PRIMARY KEY,                              -- Entidade
    nome_arquivo       VARCHAR(200) NOT NULL,
    formato            VARCHAR(10)  NOT NULL,
    data_importacao    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    importado_por_id   BIGINT       NOT NULL,                                 -- Referencial
    quantidade_setores INTEGER      NOT NULL,
    ativa              BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Integridade Referencial
    CONSTRAINT fk_importacao_usuario
        FOREIGN KEY (importado_por_id) REFERENCES usuario(id) ON DELETE RESTRICT,

    -- Integridade de Domínio
    CONSTRAINT chk_importacao_formato
        CHECK (formato IN ('CSV', 'GEOJSON')),
    CONSTRAINT chk_importacao_qtd
        CHECK (quantidade_setores > 0)
);

CREATE TABLE IF NOT EXISTS setor_censitario (
    id                   BIGSERIAL     PRIMARY KEY,                           -- Entidade
    importacao_id        BIGINT        NOT NULL,                              -- Referencial
    codigo_setor         VARCHAR(15)   NOT NULL,
    bairro               VARCHAR(80),
    poligono_coordenadas TEXT          NOT NULL,
    pct_rede_agua        NUMERIC(5, 2) NOT NULL,
    pct_rede_esgoto      NUMERIC(5, 2) NOT NULL,

    -- Integridade Referencial
    CONSTRAINT fk_setor_importacao
        FOREIGN KEY (importacao_id) REFERENCES importacao_censo(id) ON DELETE CASCADE,

    -- Integridade de Entidade composta (um código de setor não se repete na mesma importação)
    CONSTRAINT uq_setor_por_importacao
        UNIQUE (importacao_id, codigo_setor),

    -- Integridade de Domínio
    CONSTRAINT chk_setor_codigo_15_digitos
        CHECK (codigo_setor ~ '^\d{15}$'),
    CONSTRAINT chk_setor_pct_agua
        CHECK (pct_rede_agua >= 0.00 AND pct_rede_agua <= 100.00),
    CONSTRAINT chk_setor_pct_esgoto
        CHECK (pct_rede_esgoto >= 0.00 AND pct_rede_esgoto <= 100.00)
);

-- 10. LOG DE AUDITORIA LGPD IMUTÁVEL (RF019, RS011)
CREATE TABLE IF NOT EXISTS log_auditoria (
    id           BIGSERIAL    PRIMARY KEY,                                    -- Entidade
    usuario_id   BIGINT,                                                      -- Referencial (NULL quando ação automática do sistema)
    ator_nome    VARCHAR(120) NOT NULL DEFAULT 'sistema',
    data_hora    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo_evento  VARCHAR(40)  NOT NULL,
    tipo_objeto  VARCHAR(20)  NOT NULL,
    objeto_id    BIGINT,
    detalhes     VARCHAR(400) NOT NULL,

    -- Integridade Referencial
    CONSTRAINT fk_log_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE RESTRICT,

    -- Integridade de Domínio
    CONSTRAINT chk_log_evento
        CHECK (tipo_evento IN (
            'ACESSO_CONTATO_MORADOR',
            'ACESSO_COORDENADA_EXATA',
            'AUDIO_OUVIDO',
            'EDICAO_FOTO',
            'CORRECAO_TRANSCRICAO',
            'ATRIBUICAO_PONTO',
            'RETIRADA_ATRIBUICAO',
            'MUDANCA_STATUS_PONTO',
            'ALTERACAO_USUARIO',
            'IMPORTACAO_CENSO',
            'ROLLBACK_CENSO'
        )),
    CONSTRAINT chk_log_objeto
        CHECK (tipo_objeto IN ('RELATO', 'PONTO', 'EVIDENCIA', 'USUARIO', 'CENSO'))
);
