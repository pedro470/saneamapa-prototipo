-- ============================================================================
-- SaneaMapa — Carga Inicial de Dados (Maceió/AL — alinhada ao Protótipo v6)
-- Senha padrão dos usuários de demonstração: Sanea2026 (hash BCrypt)
-- ============================================================================

INSERT INTO usuario (id, cpf, nome_completo, email, celular, senha_hash, perfil, ativo) VALUES
(1, '218.334.001-00', 'Ana Lima',      'ana.lima@exemplo.gov.br',      '(82) 99120-4455', '$2a$10$fNom7YUYtk0v5LD4ZwSeb.xTVd7lyiOCFheRQqXX3IWpOlxt6X/jm', 'PREFEITURA', TRUE),
(2, '456.789.002-77', 'Carlos Mendes', 'carlos.mendes@exemplo.gov.br', '(82) 99876-1020', '$2a$10$fNom7YUYtk0v5LD4ZwSeb.xTVd7lyiOCFheRQqXX3IWpOlxt6X/jm', 'FISCAL',     TRUE),
(3, '902.115.003-48', 'Júlia Souza',   'julia.souza@exemplo.gov.br',   '(82) 98733-5501', '$2a$10$fNom7YUYtk0v5LD4ZwSeb.xTVd7lyiOCFheRQqXX3IWpOlxt6X/jm', 'FISCAL',     TRUE),
(4, '330.871.004-80', 'Rafael Torres', 'rafael.torres@exemplo.gov.br', '(82) 98100-3344', '$2a$10$fNom7YUYtk0v5LD4ZwSeb.xTVd7lyiOCFheRQqXX3IWpOlxt6X/jm', 'FISCAL',     TRUE),
(5, '774.026.005-30', 'Bruno Freitas', 'bruno.freitas@exemplo.gov.br', '(82) 99654-7788', '$2a$10$fNom7YUYtk0v5LD4ZwSeb.xTVd7lyiOCFheRQqXX3IWpOlxt6X/jm', 'FISCAL',     FALSE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO camada_vulnerabilidade (id, nome, categoria, tipo_geometria, centro_latitude, centro_longitude, raio_metros, poligono_coordenadas, peso_urgencia, fonte_dado, data_atualizacao, ativa) VALUES
(1, 'Creche municipal Vergel',          'PUBLICO_INFANTIL_ESCOLAR',             'PONTO_RAIO', -9.6713000, -35.7490000, 150,  NULL, 3, 'Secretaria de Educação, lista de unidades', '2026-09-12', TRUE),
(2, 'Escola estadual Benedito Bentes',  'PUBLICO_INFANTIL_ESCOLAR',             'PONTO_RAIO', -9.5531000, -35.7202000, 200,  NULL, 2, 'Censo Escolar 2025',                        '2026-09-12', TRUE),
(3, 'UBS Ponta Grossa',                 'IDOSOS_CUIDADOS',                      'PONTO_RAIO', -9.6754000, -35.7331000, 120,  NULL, 2, 'CNES',                                      '2026-10-01', TRUE),
(4, 'Vila de pescadores do Bom Parto',  'COMUNIDADES_TRADICIONAIS_VULNERAVEIS', 'POLIGONO',   NULL,       NULL,        NULL, '[[-9.685,-35.756],[-9.676,-35.756],[-9.676,-35.746],[-9.685,-35.746]]', 3, 'Levantamento da assistência social', '2026-08-20', TRUE),
(5, 'Área com casos de diarreia aguda', 'VULNERABILIDADE_EPIDEMIOLOGICA',       'POLIGONO',   NULL,       NULL,        NULL, '[[-9.674,-35.754],[-9.668,-35.754],[-9.668,-35.744],[-9.674,-35.744]]', 3, 'Vigilância epidemiológica, boletim 41', '2026-10-30', TRUE),
(6, 'Maternidade antiga',               'GESTANTES_RECEM_NASCIDOS',             'PONTO_RAIO', -9.6620000, -35.7380000, 150,  NULL, 2, 'CNES',                                      '2026-03-03', FALSE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO ponto (id, codigo_exibicao, tipo_ocorrencia, latitude, longitude, trecho_rua, bairro, quantidade_relatos, data_primeiro_relato, data_ultimo_relato, status, reincidencia, peso_urgencia, nivel_urgencia, motivo_urgencia, fiscal_atribuido_id, posicao_manual_algum_relato) VALUES
(1, 'P-0142', 'ESGOTO_CEU_ABERTO', -9.6712400, -35.7489300, 'Rua São Domingos, trecho 200 a 300',     'Vergel do Lago',  7, '2026-10-09', '2026-11-07', 'EM_VISTORIA',         FALSE, 3, 'CRITICA', 'Crítica pela camada Creche municipal Vergel (Público infantil e escolar)', 2, FALSE),
(2, 'P-0118', 'RUA_SEM_ESGOTO',    -9.6801100, -35.7512000, 'Travessa Santa Maria, trecho 1 a 150',     'Bom Parto',       5, '2026-10-05', '2026-10-24', 'CONFIRMADO',          FALSE, 3, 'CRITICA', 'Crítica pela camada Vila de pescadores do Bom Parto', 2, FALSE),
(3, 'P-0131', 'CASA_SEM_AGUA',     -9.5530200, -35.7201100, 'Rua Nova Esperança, trecho 400 a 550',     'Benedito Bentes', 4, '2026-10-18', '2026-10-25', 'EM_VISTORIA',         FALSE, 2, 'ELEVADA', 'Elevada pela camada Escola estadual Benedito Bentes', 3, FALSE),
(4, 'P-0150', 'ESGOTO_CEU_ABERTO', -9.6441000, -35.7155500, 'Rua do Arame, trecho 50 a 200',            'Jacintinho',      2, '2026-11-04', '2026-11-06', 'AGUARDANDO_VISTORIA', FALSE, 0, 'NORMAL',  'Normal: nenhuma camada de vulnerabilidade no local', NULL, TRUE),
(5, 'P-0127', 'AGUA_FORA_PADRAO',  -9.6755000, -35.7332000, 'Avenida Senador Rui Palmeira, trecho 3',   'Ponta Grossa',    3, '2026-10-12', '2026-10-20', 'NAO_CONFIRMADO',      FALSE, 2, 'ELEVADA', 'Elevada pela camada UBS Ponta Grossa', 2, FALSE),
(6, 'P-0109', 'RUA_SEM_ESGOTO',    -9.6487000, -35.7224000, 'Grota do Cigano, trecho 2',                'Jacintinho',      6, '2026-09-20', '2026-10-15', 'CONFIRMADO',          FALSE, 0, 'NORMAL',  'Normal: nenhuma camada de vulnerabilidade no local', 4, FALSE),
(7, 'P-0098', 'CASA_SEM_AGUA',     -9.6012000, -35.7011000, 'Rua Projetada B, trecho 1',                'Clima Bom',       3, '2026-08-02', '2026-10-23', 'CONFIRMADO',          TRUE,  0, 'NORMAL',  'Normal: nenhuma camada de vulnerabilidade no local', 3, FALSE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO item_fila_vistoria (ponto_id, data_entrada, pontuacao, fiscal_atribuido_id) VALUES
(1, '2026-10-09 12:30:00-03', 88.00, 2),
(3, '2026-10-18 16:21:00-03', 71.00, 3),
(4, '2026-11-04 21:17:00-03', 31.00, NULL)
ON CONFLICT (ponto_id) DO NOTHING;

INSERT INTO relato (id, ponto_id, protocolo, data_hora_registro, tipo_ocorrencia, descricao, identificado, nome_morador, telefone_morador, situacao_envio, latitude, longitude, precisao_metros, logradouro, bairro, ponto_referencia, posicao_manual) VALUES
(1, 1, 'SM-2026-004812', '2026-10-22 10:15:00-03', 'ESGOTO_CEU_ABERTO', 'Esgoto descendo pela rua até a creche.', TRUE, 'Maria José da Silva', '(82) 99876-5432', 'ENVIADO', -9.6712400, -35.7489300, 8, 'Rua São Domingos', 'Vergel do Lago', 'Em frente à creche municipal', FALSE),
(2, 1, 'SM-2026-004899', '2026-11-07 14:19:00-03', 'ESGOTO_CEU_ABERTO', 'Fica empoçado o dia inteiro quando chove.', FALSE, NULL, NULL, 'ENVIADO', -9.6712500, -35.7489100, 12, 'Rua São Domingos', 'Vergel do Lago', 'Esquina da escola', FALSE),
(3, 2, 'SM-2026-004610', '2026-10-24 09:00:00-03', 'RUA_SEM_ESGOTO',    'Vala aberta na frente das casas, cheiro forte.', FALSE, NULL, NULL, 'ENVIADO', -9.6801100, -35.7512000, 11, 'Travessa Santa Maria', 'Bom Parto', 'Perto do trapiche', FALSE),
(4, 3, 'SM-2026-004720', '2026-10-25 11:30:00-03', 'CASA_SEM_AGUA',     'Quatro casas sem ligação, pegam água do vizinho.', TRUE, 'José Carlos Santos', '(82) 98811-4410', 'ENVIADO', -9.5530200, -35.7201100, 10, 'Rua Nova Esperança', 'Benedito Bentes', 'Quadra 4', FALSE),
(5, 4, 'SM-2026-004880', '2026-11-06 18:45:00-03', 'ESGOTO_CEU_ABERTO', 'Esgoto correndo na vala da rua.', FALSE, NULL, NULL, 'ENVIADO', -9.6441000, -35.7155500, 40, 'Rua do Arame', 'Jacintinho', 'Perto do mercadinho', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO evidencia (id, relato_id, tipo_evidencia, origem, evidencia_divergente, arquivo_uso_caminho, data_hora_captura, latitude_captura, longitude_captura, duracao_segundos, desfoque_aplicado, som_removido, transcricao) VALUES
(1, 1, 'FOTO',  'CAMERA',    FALSE, '/midias/tratadas/foto_019231_blur.jpg', '2026-10-22 10:14:00-03', -9.6712400, -35.7489300, NULL, TRUE,  FALSE, NULL),
(2, 1, 'AUDIO', 'MICROFONE', FALSE, '/midias/audios/audio_019240.ogg',       '2026-10-22 10:15:00-03', -9.6712400, -35.7489300, 24,   FALSE, FALSE, 'Aqui na São Domingos o esgoto desce pela rua toda vez que chove, passa na frente da creche e fica empoçado o dia inteiro.'),
(3, 2, 'FOTO',  'GALERIA',   TRUE,  '/midias/tratadas/foto_019233_blur.jpg', '2026-09-25 08:10:00-03', -9.6601000, -35.7400000, NULL, TRUE,  FALSE, NULL),
(4, 3, 'VIDEO', 'CAMERA',    FALSE, '/midias/tratadas/video_019100_mudo.mp4','2026-10-24 08:58:00-03', -9.6801100, -35.7512000, 18,   TRUE,  TRUE,  NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO importacao_censo (id, nome_arquivo, formato, data_importacao, importado_por_id, quantidade_setores, ativa) VALUES
(1, 'setores-maceio-2022.geojson',    'GEOJSON', '2026-09-14 10:00:00-03', 1, 2, FALSE),
(2, 'setores-maceio-2022-v2.geojson', 'GEOJSON', '2026-11-05 09:30:55-03', 1, 4, TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO setor_censitario (importacao_id, codigo_setor, bairro, poligono_coordenadas, pct_rede_agua, pct_rede_esgoto) VALUES
(1, '270430405000123', 'Vergel do Lago',  '[[-9.675,-35.755],[-9.665,-35.755],[-9.665,-35.742],[-9.675,-35.742]]', 96.00, 88.00),
(1, '270430405000087', 'Bom Parto',       '[[-9.686,-35.758],[-9.676,-35.758],[-9.676,-35.745],[-9.686,-35.745]]', 94.00, 92.00),
(2, '270430405000123', 'Vergel do Lago',  '[[-9.675,-35.755],[-9.665,-35.755],[-9.665,-35.742],[-9.675,-35.742]]', 97.50, 88.00),
(2, '270430405000087', 'Bom Parto',       '[[-9.686,-35.758],[-9.676,-35.758],[-9.676,-35.745],[-9.686,-35.745]]', 99.00, 92.00),
(2, '270430405000311', 'Benedito Bentes', '[[-9.560,-35.728],[-9.545,-35.728],[-9.545,-35.712],[-9.560,-35.712]]', 97.00, 65.00),
(2, '270430405000198', 'Jacintinho',      '[[-9.655,-35.728],[-9.640,-35.728],[-9.640,-35.710],[-9.655,-35.710]]', 90.00, 34.00)
ON CONFLICT (importacao_id, codigo_setor) DO NOTHING;

INSERT INTO log_auditoria (usuario_id, ator_nome, data_hora, tipo_evento, tipo_objeto, objeto_id, detalhes) VALUES
(1, 'Ana Lima',      '2026-11-05 09:30:55-03', 'IMPORTACAO_CENSO',        'CENSO',     2, 'Importação do arquivo setores-maceio-2022-v2.geojson com 4 setores.'),
(1, 'Ana Lima',      '2026-11-06 08:05:00-03', 'ATRIBUICAO_PONTO',        'PONTO',     1, 'Ponto P-0142 atribuído a Carlos Mendes (em vistoria).'),
(1, 'Ana Lima',      '2026-11-06 16:02:10-03', 'ALTERACAO_USUARIO',       'USUARIO',   4, 'Celular de Rafael Torres atualizado.'),
(2, 'Carlos Mendes', '2026-11-07 10:52:31-03', 'AUDIO_OUVIDO',            'EVIDENCIA', 2, 'Reprodução de áudio do relato SM-2026-004812.'),
(2, 'Carlos Mendes', '2026-11-07 11:08:44-03', 'EDICAO_FOTO',             'EVIDENCIA', 3, 'Novo desfoque aplicado na evidência divergente.'),
(1, 'Ana Lima',      '2026-11-07 14:21:02-03', 'ACESSO_COORDENADA_EXATA', 'PONTO',     1, 'Consulta à coordenada exata do ponto P-0142.'),
(1, 'Ana Lima',      '2026-11-07 14:21:07-03', 'ACESSO_CONTATO_MORADOR',  'RELATO',    1, 'Consulta a nome e telefone de morador identificado no ponto P-0142.')
ON CONFLICT DO NOTHING;

-- Atualiza sequências após inserção com IDs explícitos
SELECT setval('usuario_id_seq',                COALESCE((SELECT MAX(id) FROM usuario), 1));
SELECT setval('camada_vulnerabilidade_id_seq', COALESCE((SELECT MAX(id) FROM camada_vulnerabilidade), 1));
SELECT setval('ponto_id_seq',                  COALESCE((SELECT MAX(id) FROM ponto), 1));
SELECT setval('relato_id_seq',                 COALESCE((SELECT MAX(id) FROM relato), 1));
SELECT setval('evidencia_id_seq',              COALESCE((SELECT MAX(id) FROM evidencia), 1));
SELECT setval('importacao_censo_id_seq',       COALESCE((SELECT MAX(id) FROM importacao_censo), 1));
