-- ============================================================================
-- SaneaMapa — Triggers PostgreSQL
-- Garantem imutabilidade da auditoria (RF019/RS011), ciclo de vida do ponto
-- (Seção 10.1), atualização automática no agrupamento (RF004) e limite 24h (RS001)
-- ============================================================================

-- 1. TRIGGER DE IMUTABILIDADE DO LOG DE AUDITORIA (RF019 / RS011)
CREATE OR REPLACE FUNCTION fn_bloquear_mutacao_auditoria()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION
        'Operação % proibida na tabela log_auditoria: registros de auditoria LGPD são imutáveis.',
        TG_OP;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_log_auditoria_imutavel ON log_auditoria;
CREATE TRIGGER trg_log_auditoria_imutavel
    BEFORE UPDATE OR DELETE ON log_auditoria
    FOR EACH ROW
    EXECUTE FUNCTION fn_bloquear_mutacao_auditoria();


-- 2. TRIGGER DO CICLO DE VIDA DO PONTO (Seção 10.1 do Documento de Requisitos)
-- Transições permitidas:
--   AGUARDANDO_VISTORIA -> EM_VISTORIA
--   EM_VISTORIA         -> AGUARDANDO_VISTORIA | CONFIRMADO | NAO_CONFIRMADO
--   NAO_CONFIRMADO      -> AGUARDANDO_VISTORIA
CREATE OR REPLACE FUNCTION fn_validar_ciclo_vida_ponto()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status IS DISTINCT FROM OLD.status THEN
        IF NOT (
            (OLD.status = 'AGUARDANDO_VISTORIA' AND NEW.status = 'EM_VISTORIA') OR
            (OLD.status = 'EM_VISTORIA'         AND NEW.status IN ('AGUARDANDO_VISTORIA', 'CONFIRMADO', 'NAO_CONFIRMADO')) OR
            (OLD.status = 'NAO_CONFIRMADO'      AND NEW.status = 'AGUARDANDO_VISTORIA')
        ) THEN
            RAISE EXCEPTION
                'Transição de status inválida no ponto %: % -> % não é permitida pelo ciclo de vida.',
                OLD.codigo_exibicao, OLD.status, NEW.status;
        END IF;
    END IF;

    NEW.atualizado_em := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_ponto_ciclo_vida ON ponto;
CREATE TRIGGER trg_ponto_ciclo_vida
    BEFORE UPDATE ON ponto
    FOR EACH ROW
    EXECUTE FUNCTION fn_validar_ciclo_vida_ponto();


-- 3. TRIGGER DE LIMITE DE 5 RELATOS POR TELEFONE A CADA 24 HORAS (RF001 / RS001)
CREATE OR REPLACE FUNCTION fn_validar_limite_relatos_24h()
RETURNS TRIGGER
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    qtd_recentes INTEGER;
BEGIN
    IF NEW.identificado = TRUE AND NEW.telefone_morador IS NOT NULL THEN
        SELECT COUNT(*)
          INTO qtd_recentes
          FROM relato
         WHERE telefone_morador = NEW.telefone_morador
           AND data_hora_registro >= (CURRENT_TIMESTAMP - INTERVAL '24 hours');

        IF qtd_recentes >= 5 THEN
            RAISE EXCEPTION
                'Limite diário atingido: o telefone % já enviou 5 relatos nas últimas 24 horas.',
                NEW.telefone_morador;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_relato_limite_24h ON relato;
CREATE TRIGGER trg_relato_limite_24h
    BEFORE INSERT ON relato
    FOR EACH ROW
    EXECUTE FUNCTION fn_validar_limite_relatos_24h();


-- 4. TRIGGER DE ATUALIZAÇÃO DO PONTO AO VINCULAR NOVO RELATO (RF004 / RS009)
CREATE OR REPLACE FUNCTION fn_ao_inserir_relato()
RETURNS TRIGGER
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_status_atual VARCHAR(25);
BEGIN
    SELECT status INTO v_status_atual FROM ponto WHERE id = NEW.ponto_id;

    UPDATE ponto
       SET quantidade_relatos = (SELECT COUNT(*) FROM relato WHERE ponto_id = NEW.ponto_id),
           data_ultimo_relato = GREATEST(data_ultimo_relato, NEW.data_hora_registro::DATE),
           reincidencia = CASE
               WHEN v_status_atual = 'CONFIRMADO' THEN TRUE
               ELSE reincidencia
           END,
           status = CASE
               WHEN v_status_atual = 'NAO_CONFIRMADO' THEN 'AGUARDANDO_VISTORIA'
               ELSE status
           END,
           posicao_manual_algum_relato = posicao_manual_algum_relato OR NEW.posicao_manual
     WHERE id = NEW.ponto_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_relato_atualiza_ponto ON relato;
CREATE TRIGGER trg_relato_atualiza_ponto
    AFTER INSERT ON relato
    FOR EACH ROW
    EXECUTE FUNCTION fn_ao_inserir_relato();
