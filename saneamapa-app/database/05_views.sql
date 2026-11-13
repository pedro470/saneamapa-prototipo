-- ============================================================================
-- SaneaMapa — Views PostgreSQL
-- Consultas consolidadas para Fila de Vistoria, Mapa LGPD, Indicadores e Auditoria
-- ============================================================================

-- 1. VIEW DA FILA DE VISTORIA ORDENADA POR PONTUAÇÃO (RF005)
CREATE OR REPLACE VIEW vw_fila_vistoria AS
SELECT
    ROW_NUMBER() OVER (ORDER BY f.pontuacao DESC, f.data_entrada ASC) AS posicao_fila,
    p.id                               AS ponto_id,
    p.codigo_exibicao,
    p.tipo_ocorrencia,
    p.trecho_rua,
    p.bairro,
    p.quantidade_relatos,
    (CURRENT_DATE - p.data_primeiro_relato) AS dias_espera,
    p.peso_urgencia,
    p.nivel_urgencia,
    p.motivo_urgencia,
    f.pontuacao,
    p.status,
    p.reincidencia,
    u.id                               AS fiscal_id,
    COALESCE(u.nome_completo, 'Sem atribuição') AS fiscal_nome,
    f.data_entrada
FROM item_fila_vistoria f
JOIN ponto p ON p.id = f.ponto_id
LEFT JOIN usuario u ON u.id = f.fiscal_atribuido_id
WHERE p.status IN ('AGUARDANDO_VISTORIA', 'EM_VISTORIA')
ORDER BY f.pontuacao DESC, f.data_entrada ASC;


-- 2. VIEW LGPD PARA EXIBIÇÃO RESTRITA FORA DA EQUIPE INTERNA (RF001, RF002, RF006, RS003)
-- Oculta coordenadas exatas, nome/telefone de moradores e nível de urgência.
CREATE OR REPLACE VIEW vw_mapa_publico_lgpd AS
SELECT
    p.id,
    p.codigo_exibicao,
    p.tipo_ocorrencia,
    p.trecho_rua,
    p.bairro,
    p.quantidade_relatos,
    p.status,
    p.reincidencia,
    p.data_primeiro_relato,
    p.data_ultimo_relato
FROM ponto p;


-- 3. VIEW DE INDICADORES DO PAINEL DA PREFEITURA (RF015)
CREATE OR REPLACE VIEW vw_indicadores_painel AS
SELECT
    'STATUS' AS dimensao,
    status   AS categoria,
    COUNT(*) AS total_pontos,
    SUM(quantidade_relatos) AS total_relatos
FROM ponto
GROUP BY status
UNION ALL
SELECT
    'TIPO_OCORRENCIA' AS dimensao,
    tipo_ocorrencia   AS categoria,
    COUNT(*)          AS total_pontos,
    SUM(quantidade_relatos) AS total_relatos
FROM ponto
GROUP BY tipo_ocorrencia
UNION ALL
SELECT
    'NIVEL_URGENCIA' AS dimensao,
    nivel_urgencia   AS categoria,
    COUNT(*)         AS total_pontos,
    SUM(quantidade_relatos) AS total_relatos
FROM ponto
GROUP BY nivel_urgencia
UNION ALL
SELECT
    'BAIRRO' AS dimensao,
    bairro   AS categoria,
    COUNT(*) AS total_pontos,
    SUM(quantidade_relatos) AS total_relatos
FROM ponto
GROUP BY bairro;


-- 4. VIEW DE AUDITORIA ENRIQUECIDA (RF019)
CREATE OR REPLACE VIEW vw_relatorio_auditoria AS
SELECT
    l.id,
    l.data_hora,
    COALESCE(u.nome_completo, l.ator_nome) AS usuario_responsavel,
    u.perfil                               AS perfil_usuario,
    l.tipo_evento,
    l.tipo_objeto,
    l.objeto_id,
    l.detalhes
FROM log_auditoria l
LEFT JOIN usuario u ON u.id = l.usuario_id
ORDER BY l.data_hora DESC;

-- Conceder acesso de leitura às views conforme perfis
GRANT SELECT ON vw_mapa_publico_lgpd TO role_morador, role_fiscal, role_prefeitura;
GRANT SELECT ON vw_fila_vistoria TO role_fiscal, role_prefeitura;
GRANT SELECT ON vw_indicadores_painel, vw_relatorio_auditoria TO role_prefeitura;
