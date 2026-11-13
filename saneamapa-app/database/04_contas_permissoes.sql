-- ============================================================================
-- SaneaMapa — Contas (Roles/Usuários) e Permissões no PostgreSQL
-- Alinhado à Matriz de Permissões (Seção 10.2) e LGPD (RF001, RF002, RF019)
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'role_morador') THEN
        CREATE ROLE role_morador NOLOGIN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'role_fiscal') THEN
        CREATE ROLE role_fiscal NOLOGIN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'role_prefeitura') THEN
        CREATE ROLE role_prefeitura NOLOGIN;
    END IF;

    -- Contas de acesso ao banco por perfil/aplicação
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'saneamapa_morador') THEN
        CREATE USER saneamapa_morador WITH PASSWORD 'SaneaMorador#2026';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'saneamapa_fiscal') THEN
        CREATE USER saneamapa_fiscal WITH PASSWORD 'SaneaFiscal#2026';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'saneamapa_prefeitura') THEN
        CREATE USER saneamapa_prefeitura WITH PASSWORD 'SaneaPref#2026';
    END IF;
END
$$;

GRANT role_morador TO saneamapa_morador;
GRANT role_fiscal TO saneamapa_fiscal;
GRANT role_prefeitura TO saneamapa_prefeitura;

-- Princípio do menor privilégio: revoga acesso público padrão
REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO role_morador, role_fiscal, role_prefeitura;

-- 1. Permissões do MORADOR (só registra relatos/evidências e consulta mapa público LGPD)
GRANT INSERT, SELECT ON TABLE ponto, relato, evidencia TO role_morador;
GRANT USAGE, SELECT ON SEQUENCE ponto_id_seq, relato_id_seq, evidencia_id_seq TO role_morador;
REVOKE SELECT (nome_morador, telefone_morador) ON TABLE relato FROM role_morador;
-- Morador não acessa usuários, auditoria, vistorias, camadas nem fila:
REVOKE ALL ON TABLE usuario, alteracao_usuario, log_auditoria, vistoria,
    camada_vulnerabilidade, item_fila_vistoria, importacao_censo, setor_censitario FROM role_morador;

-- 2. Permissões do FISCAL (consulta pontos atribuídos, registra vistoria, re-desfoque e correção de transcrição)
GRANT SELECT ON TABLE ponto, relato, evidencia, item_fila_vistoria,
    camada_vulnerabilidade, importacao_censo, setor_censitario TO role_fiscal;
GRANT SELECT (id, nome_completo, perfil, ativo) ON TABLE usuario TO role_fiscal;
GRANT INSERT, SELECT ON TABLE vistoria, log_auditoria TO role_fiscal;
GRANT UPDATE (tipo_ocorrencia, latitude, longitude, trecho_rua, bairro, status, atualizado_em) ON TABLE ponto TO role_fiscal;
GRANT UPDATE (desfoque_aplicado, regiao_desfoque_manual, transcricao, atualizado_em) ON TABLE evidencia TO role_fiscal;
GRANT USAGE, SELECT ON SEQUENCE vistoria_id_seq, log_auditoria_id_seq TO role_fiscal;
-- Nenhum perfil pode apagar logs de auditoria
REVOKE UPDATE, DELETE ON TABLE log_auditoria FROM role_fiscal;

-- 3. Permissões da PREFEITURA (painel completo, gestão de fila, usuários, camadas e censo; auditoria somente leitura/inserção)
GRANT SELECT, INSERT, UPDATE ON TABLE usuario, alteracao_usuario, ponto, relato, evidencia,
    item_fila_vistoria, camada_vulnerabilidade, importacao_censo, setor_censitario TO role_prefeitura;
GRANT SELECT ON TABLE vistoria TO role_prefeitura;
GRANT SELECT, INSERT ON TABLE log_auditoria TO role_prefeitura;
GRANT DELETE ON TABLE item_fila_vistoria TO role_prefeitura;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO role_prefeitura;
-- Garantia LGPD/RS011: nem a Prefeitura altera ou apaga auditoria
REVOKE UPDATE, DELETE ON TABLE log_auditoria FROM role_prefeitura;
