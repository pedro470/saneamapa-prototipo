-- ============================================================================
-- SaneaMapa — Criação de Índices (Campos Indexados)
-- Otimização das consultas críticas: fila de vistoria, mapa, limite 24h e auditoria
-- ============================================================================

-- Índice composto na tabela ponto para filtros de status e tipo no mapa (RF010)
CREATE INDEX IF NOT EXISTS idx_ponto_status_tipo
    ON ponto (status, tipo_ocorrencia);

-- Índice para filtros por bairro e nível de urgência no painel e indicadores (RF010, RF015)
CREATE INDEX IF NOT EXISTS idx_ponto_bairro_urgencia
    ON ponto (bairro, nivel_urgencia);

-- Índice espacial simples por latitude/longitude para agrupamento no raio de 50 m (RF004)
CREATE INDEX IF NOT EXISTS idx_ponto_coordenadas
    ON ponto (latitude, longitude);

-- Índice parcial para verificação rápida do limite de 5 relatos por telefone em 24h (RF001, RS001)
CREATE INDEX IF NOT EXISTS idx_relato_telefone_24h
    ON relato (telefone_morador, data_hora_registro DESC)
    WHERE telefone_morador IS NOT NULL;

-- Índice para ordenação decrescente da fila de vistoria por pontuação (RF005)
CREATE INDEX IF NOT EXISTS idx_item_fila_pontuacao_desc
    ON item_fila_vistoria (pontuacao DESC, data_entrada ASC);

-- Índice composto para pesquisa no log de auditoria LGPD por período, evento e usuário (RF019)
CREATE INDEX IF NOT EXISTS idx_log_auditoria_consulta
    ON log_auditoria (data_hora DESC, tipo_evento, usuario_id);

-- Índice para busca rápida de setores censitários por importação e código (RF020)
CREATE INDEX IF NOT EXISTS idx_setor_censitario_importacao_codigo
    ON setor_censitario (importacao_id, codigo_setor);
