package br.edu.cesmac.saneamapa.service;

import br.edu.cesmac.saneamapa.domain.enums.TipoOcorrencia;
import br.edu.cesmac.saneamapa.domain.exception.RecursoNaoEncontradoException;
import br.edu.cesmac.saneamapa.domain.model.CamadaVulnerabilidade;
import br.edu.cesmac.saneamapa.domain.model.Evidencia;
import br.edu.cesmac.saneamapa.domain.model.Ponto;
import br.edu.cesmac.saneamapa.domain.model.Relato;
import br.edu.cesmac.saneamapa.domain.model.SetorCensitario;
import br.edu.cesmac.saneamapa.domain.model.Usuario;
import br.edu.cesmac.saneamapa.domain.model.Vistoria;
import br.edu.cesmac.saneamapa.domain.ontologia.ClasseOntologia;
import br.edu.cesmac.saneamapa.domain.ontologia.GrafoConhecimento;
import br.edu.cesmac.saneamapa.domain.ontologia.RelacaoOntologia;
import br.edu.cesmac.saneamapa.dto.GrafoDtos.ArestaDto;
import br.edu.cesmac.saneamapa.dto.GrafoDtos.ClasseDto;
import br.edu.cesmac.saneamapa.dto.GrafoDtos.GrafoDto;
import br.edu.cesmac.saneamapa.dto.GrafoDtos.NoDto;
import br.edu.cesmac.saneamapa.dto.GrafoDtos.OntologiaDto;
import br.edu.cesmac.saneamapa.dto.GrafoDtos.RelacaoDto;
import br.edu.cesmac.saneamapa.repository.CamadaVulnerabilidadeRepository;
import br.edu.cesmac.saneamapa.repository.EvidenciaRepository;
import br.edu.cesmac.saneamapa.repository.PontoRepository;
import br.edu.cesmac.saneamapa.repository.RelatoRepository;
import br.edu.cesmac.saneamapa.repository.SetorCensitarioRepository;
import br.edu.cesmac.saneamapa.repository.VistoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Monta o grafo de conhecimento do SaneaMapa a partir do banco, usando a ontologia como esquema.
 *
 * O grafo é uma visão de leitura: nada é gravado. Vínculos declarados saem das chaves estrangeiras;
 * vínculos inferidos (setor, camada de vulnerabilidade, vizinhança) são deduzidos das geometrias.
 * Nenhum nó leva nome ou telefone de morador, nem coordenada exata.
 */
@Service
public class GrafoConhecimentoService {

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int SALTOS_PADRAO = 2;
    private static final int SALTOS_MAXIMO = 4;

    private final PontoRepository pontoRepository;
    private final RelatoRepository relatoRepository;
    private final EvidenciaRepository evidenciaRepository;
    private final VistoriaRepository vistoriaRepository;
    private final CamadaVulnerabilidadeRepository camadaRepository;
    private final SetorCensitarioRepository setorRepository;

    public GrafoConhecimentoService(PontoRepository pontoRepository,
                                    RelatoRepository relatoRepository,
                                    EvidenciaRepository evidenciaRepository,
                                    VistoriaRepository vistoriaRepository,
                                    CamadaVulnerabilidadeRepository camadaRepository,
                                    SetorCensitarioRepository setorRepository) {
        this.pontoRepository = pontoRepository;
        this.relatoRepository = relatoRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.vistoriaRepository = vistoriaRepository;
        this.camadaRepository = camadaRepository;
        this.setorRepository = setorRepository;
    }

    /** Esquema: classes e relações, para a tela explicar o que cada nó e aresta significa. */
    public OntologiaDto obterOntologia() {
        List<ClasseDto> classes = Arrays.stream(ClasseOntologia.values())
                .map(c -> new ClasseDto(c.name(), c.getRotulo(),
                        c.getSuperclasse() != null ? c.getSuperclasse().name() : null,
                        c.isAbstrata(), c.isProtegidaLgpd(), c.getDescricao()))
                .toList();
        List<RelacaoDto> relacoes = Arrays.stream(RelacaoOntologia.values())
                .map(r -> new RelacaoDto(r.name(), r.getRotulo(), r.getDominio().name(), r.getContradominio().name(),
                        r.isInferida(), r.podeSerMaterializada(), r.getDescricao()))
                .toList();
        return new OntologiaDto(classes, relacoes);
    }

    /** Grafo inteiro, ou só a vizinhança de um ponto quando {@code pontoId} é informado. */
    @Transactional(readOnly = true)
    public GrafoDto obterGrafo(Long pontoId, Integer saltos) {
        GrafoConhecimento grafo = montar();
        if (pontoId == null) {
            return toDto(grafo, null);
        }
        String centro = GrafoConhecimento.idDe(ClasseOntologia.PONTO, pontoId);
        if (!grafo.contemNo(centro)) {
            throw new RecursoNaoEncontradoException("Ponto não encontrado.", "Selecione outro ponto no mapa.");
        }
        int alcance = saltos == null ? SALTOS_PADRAO : Math.max(1, Math.min(SALTOS_MAXIMO, saltos));
        return toDto(grafo.vizinhanca(centro, alcance), centro);
    }

    @Transactional(readOnly = true)
    public GrafoConhecimento montar() {
        GrafoConhecimento g = new GrafoConhecimento();

        // Vocabulário controlado: tipos de ocorrência e o serviço que cada um afeta.
        for (TipoOcorrencia tipo : TipoOcorrencia.values()) {
            String servico = "AGUA".equals(tipo.getServicoCenso()) ? "Água" : "Esgoto";
            g.adicionarNo(ClasseOntologia.SERVICO_SANEAMENTO, tipo.getServicoCenso(), servico, null);
            g.adicionarNo(ClasseOntologia.TIPO_OCORRENCIA, tipo.name(), tipo.getRotulo(), null);
            g.relacionar(id(ClasseOntologia.TIPO_OCORRENCIA, tipo.name()), RelacaoOntologia.AFETA_SERVICO,
                    id(ClasseOntologia.SERVICO_SANEAMENTO, tipo.getServicoCenso()));
        }

        List<SetorCensitario> setores = setorRepository.findByImportacaoAtivaTrue();
        for (SetorCensitario s : setores) {
            Map<String, Object> at = new LinkedHashMap<>();
            at.put("Rede de água", s.getPctRedeAgua().stripTrailingZeros().toPlainString() + "% dos domicílios");
            at.put("Rede de esgoto", s.getPctRedeEsgoto().stripTrailingZeros().toPlainString() + "% dos domicílios");
            g.adicionarNo(ClasseOntologia.SETOR_CENSITARIO, s.getId(), "Setor " + s.getCodigoSetor(), at);
            String bairroId = noBairro(g, s.getBairro());
            if (bairroId != null) {
                g.relacionar(id(ClasseOntologia.SETOR_CENSITARIO, s.getId()), RelacaoOntologia.PERTENCE_A, bairroId);
            }
        }

        List<CamadaVulnerabilidade> camadas = camadaRepository.findByAtivaTrue();
        for (CamadaVulnerabilidade c : camadas) {
            Map<String, Object> at = new LinkedHashMap<>();
            at.put("Categoria", c.getCategoria().getRotulo());
            at.put("Peso de urgência", (int) c.getPesoUrgencia());
            at.put("Geometria", c.obterGeometria().descrever());
            at.put("Fonte", c.getFonteDado());
            g.adicionarNo(ClasseOntologia.CAMADA_VULNERABILIDADE, c.getId(), c.getNome(), at);
        }

        List<Ponto> pontos = pontoRepository.findAll();
        for (Ponto p : pontos) {
            Map<String, Object> at = new LinkedHashMap<>();
            at.put("Status", p.getStatus().getRotulo());
            at.put("Urgência", p.getNivelUrgencia().getRotulo());
            at.put("Relatos", p.getQuantidadeRelatos());
            at.put("Trecho", p.getTrechoRua());
            if (p.isReincidencia()) {
                at.put("Reincidência", "Sim");
            }
            // Chaves técnicas que a tela usa para colorir o nó e abrir o ponto no mapa.
            at.put("_pontoId", p.getId());
            at.put("_urgencia", p.getNivelUrgencia().name());
            at.put("_tipo", p.getTipoOcorrencia().name());
            String pontoId = g.adicionarNo(ClasseOntologia.PONTO, p.getId(), p.getCodigoExibicao(), at).id();

            g.relacionar(pontoId, RelacaoOntologia.E_DO_TIPO, id(ClasseOntologia.TIPO_OCORRENCIA, p.getTipoOcorrencia().name()));
            String bairroId = noBairro(g, p.getBairro());
            if (bairroId != null) {
                g.relacionar(pontoId, RelacaoOntologia.LOCALIZADO_EM, bairroId);
            }
            if (p.getFiscalAtribuido() != null) {
                g.relacionar(pontoId, RelacaoOntologia.ATRIBUIDO_A, noFiscal(g, p.getFiscalAtribuido()));
            }

            // Inferências espaciais: o vínculo não está gravado, sai da geometria.
            double lat = p.getLatitude().doubleValue();
            double lon = p.getLongitude().doubleValue();
            for (SetorCensitario s : setores) {
                if (s.contemCoordenada(lat, lon)) {
                    g.relacionar(pontoId, RelacaoOntologia.DENTRO_DO_SETOR, id(ClasseOntologia.SETOR_CENSITARIO, s.getId()));
                }
            }
            for (CamadaVulnerabilidade c : camadas) {
                if (c.obterGeometria().contemPonto(lat, lon)) {
                    g.relacionar(pontoId, RelacaoOntologia.SOB_INFLUENCIA_DE, id(ClasseOntologia.CAMADA_VULNERABILIDADE, c.getId()));
                }
            }
        }

        // Vizinhança é simétrica: uma aresta por par, do ponto mais antigo para o mais novo.
        for (int i = 0; i < pontos.size(); i++) {
            for (int j = i + 1; j < pontos.size(); j++) {
                Ponto a = pontos.get(i);
                Ponto b = pontos.get(j);
                double metros = a.distanciaEmMetrosPara(b.getLatitude().doubleValue(), b.getLongitude().doubleValue());
                if (metros <= RelacaoOntologia.RAIO_VIZINHANCA_METROS) {
                    g.relacionar(id(ClasseOntologia.PONTO, a.getId()), RelacaoOntologia.VIZINHO_DE, id(ClasseOntologia.PONTO, b.getId()));
                }
            }
        }

        for (Relato r : relatoRepository.findAll()) {
            Map<String, Object> at = new LinkedHashMap<>();
            at.put("Registrado em", r.getDataHoraRegistro().format(DATA_BR));
            at.put("Tipo", r.getTipoOcorrencia().getRotulo());
            String relatoId = g.adicionarNo(ClasseOntologia.RELATO, r.getId(), r.getProtocolo(), at).id();
            g.relacionar(relatoId, RelacaoOntologia.AGRUPADO_EM, id(ClasseOntologia.PONTO, r.getPonto().getId()));
        }

        for (Evidencia ev : evidenciaRepository.findAll()) {
            ClasseOntologia classe = switch (ev.getTipoEvidencia()) {
                case FOTO -> ClasseOntologia.EVIDENCIA_FOTO;
                case VIDEO -> ClasseOntologia.EVIDENCIA_VIDEO;
                case AUDIO -> ClasseOntologia.EVIDENCIA_AUDIO;
            };
            Map<String, Object> at = new LinkedHashMap<>();
            at.put("Origem", ev.getOrigem().name().charAt(0) + ev.getOrigem().name().substring(1).toLowerCase(Locale.ROOT));
            at.put("Tratamento", ev.descreverTratamento());
            if (ev.isEvidenciaDivergente()) {
                at.put("Divergente", "Sim");
            }
            String evId = g.adicionarNo(classe, ev.getId(), classe.getRotulo() + " " + ev.getId(), at).id();
            g.relacionar(evId, RelacaoOntologia.COMPROVA, id(ClasseOntologia.RELATO, ev.getRelato().getId()));
        }

        for (Vistoria v : vistoriaRepository.findAll()) {
            Map<String, Object> at = new LinkedHashMap<>();
            at.put("Resultado", v.getResultado().getRotulo());
            at.put("Data", v.getDataHoraVistoria().format(DATA_BR));
            at.put("Tipo validado", v.getTipoOcorrenciaValidado().getRotulo());
            String vistoriaId = g.adicionarNo(ClasseOntologia.VISTORIA, v.getId(), "Vistoria " + v.getId(), at).id();
            g.relacionar(vistoriaId, RelacaoOntologia.VERIFICA, id(ClasseOntologia.PONTO, v.getPonto().getId()));
            g.relacionar(vistoriaId, RelacaoOntologia.REALIZADA_POR, noFiscal(g, v.getFiscal()));
        }

        return g;
    }

    private static String id(ClasseOntologia classe, Object chave) {
        return GrafoConhecimento.idDe(classe, chave);
    }

    /** Bairro não é tabela: vira nó a partir do texto, sem diferenciar maiúsculas. */
    private static String noBairro(GrafoConhecimento g, String bairro) {
        if (bairro == null || bairro.isBlank()) {
            return null;
        }
        String nome = bairro.trim();
        return g.adicionarNo(ClasseOntologia.BAIRRO, nome.toLowerCase(Locale.ROOT), nome, null).id();
    }

    private static String noFiscal(GrafoConhecimento g, Usuario fiscal) {
        Map<String, Object> at = new LinkedHashMap<>();
        at.put("Situação", fiscal.isAtivo() ? "Ativo" : "Desativado");
        return g.adicionarNo(ClasseOntologia.FISCAL, fiscal.getId(), fiscal.getNomeCompleto(), at).id();
    }

    private static GrafoDto toDto(GrafoConhecimento g, String centro) {
        List<NoDto> nos = g.nos().stream()
                .map(n -> new NoDto(n.id(), n.classe().name(), n.rotulo(), n.atributos()))
                .toList();
        List<ArestaDto> arestas = g.arestas().stream()
                .map(a -> new ArestaDto(a.origem(), a.relacao().name(), a.relacao().getRotulo(), a.destino(), a.relacao().isInferida()))
                .toList();
        return new GrafoDto(centro, nos, arestas);
    }
}
