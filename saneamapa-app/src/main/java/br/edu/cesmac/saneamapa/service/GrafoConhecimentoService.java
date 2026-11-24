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
import br.edu.cesmac.saneamapa.repository.UsuarioRepository;
import br.edu.cesmac.saneamapa.repository.VistoriaRepository;
import br.edu.cesmac.saneamapa.service.EventoMutacaoGrafo.CamadasAlteradas;
import br.edu.cesmac.saneamapa.service.EventoMutacaoGrafo.CensoAlterado;
import br.edu.cesmac.saneamapa.service.EventoMutacaoGrafo.EvidenciaAtualizada;
import br.edu.cesmac.saneamapa.service.EventoMutacaoGrafo.PontoAtribuicaoAlterada;
import br.edu.cesmac.saneamapa.service.EventoMutacaoGrafo.RelatoRegistrado;
import br.edu.cesmac.saneamapa.service.EventoMutacaoGrafo.UsuarioAtualizado;
import br.edu.cesmac.saneamapa.service.EventoMutacaoGrafo.VistoriaRegistrada;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Mantém o grafo de conhecimento do SaneaMapa em memória usando a ontologia como esquema.
 *
 * O grafo é hidratado uma única vez na subida da aplicação ({@link ApplicationReadyEvent}) e
 * atualizado incrementalmente após o commit de cada mudança no domínio ({@link EventoMutacaoGrafo}).
 * Leituras respondem direto da memória sem consultar o banco. Nenhum nó leva nome ou telefone
 * de morador, nem coordenada exata.
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
    private final UsuarioRepository usuarioRepository;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicInteger montagensCompletas = new AtomicInteger(0);
    private GrafoConhecimento grafoEmMemoria;

    public GrafoConhecimentoService(PontoRepository pontoRepository,
                                    RelatoRepository relatoRepository,
                                    EvidenciaRepository evidenciaRepository,
                                    VistoriaRepository vistoriaRepository,
                                    CamadaVulnerabilidadeRepository camadaRepository,
                                    SetorCensitarioRepository setorRepository,
                                    UsuarioRepository usuarioRepository) {
        this.pontoRepository = pontoRepository;
        this.relatoRepository = relatoRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.vistoriaRepository = vistoriaRepository;
        this.camadaRepository = camadaRepository;
        this.setorRepository = setorRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Hidrata o grafo em memória uma única vez após o bootstrap inicial (DataInitializer). */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void inicializarAoSubir() {
        lock.writeLock().lock();
        try {
            this.grafoEmMemoria = montar();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getQuantidadeMontagensCompletas() {
        return montagensCompletas.get();
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

    /** Grafo inteiro, ou só a vizinhança de um ponto quando {@code pontoId} é informado. Lê direto da memória. */
    public GrafoDto obterGrafo(Long pontoId, Integer saltos) {
        if (grafoEmMemoria == null) {
            lock.writeLock().lock();
            try {
                if (grafoEmMemoria == null) {
                    grafoEmMemoria = montar();
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        lock.readLock().lock();
        try {
            if (pontoId == null) {
                return toDto(grafoEmMemoria, null);
            }
            String centro = id(ClasseOntologia.PONTO, pontoId);
            if (!grafoEmMemoria.contemNo(centro)) {
                throw new RecursoNaoEncontradoException("Ponto não encontrado.", "Selecione outro ponto no mapa.");
            }
            int alcance = saltos == null ? SALTOS_PADRAO : Math.max(1, Math.min(SALTOS_MAXIMO, saltos));
            return toDto(grafoEmMemoria.vizinhanca(centro, alcance), centro);
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Atualiza o grafo em memória de forma incremental após o commit de cada mutação de domínio. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void aoMutarDominio(EventoMutacaoGrafo evento) {
        lock.writeLock().lock();
        try {
            if (grafoEmMemoria == null) {
                return;
            }
            GrafoConhecimento g = grafoEmMemoria;
            switch (evento) {
                case RelatoRegistrado(Long relatoId, Long pontoId, boolean novoPonto) -> {
                    Ponto ponto = pontoRepository.findById(pontoId).orElse(null);
                    Relato relato = relatoRepository.findById(relatoId).orElse(null);
                    if (ponto != null) {
                        if (novoPonto) {
                            sincronizarPontoCompleto(g, ponto);
                        } else {
                            sincronizarNoEAtribuicaoPonto(g, ponto);
                        }
                    }
                    if (relato != null) {
                        sincronizarRelato(g, relato);
                        for (Evidencia ev : evidenciaRepository.findByRelatoIdOrderByIdAsc(relatoId)) {
                            sincronizarEvidencia(g, ev);
                        }
                    }
                    removerOrfaos(g);
                }
                case PontoAtribuicaoAlterada(Long pontoId) -> {
                    pontoRepository.findById(pontoId).ifPresent(p -> sincronizarNoEAtribuicaoPonto(g, p));
                    removerOrfaos(g);
                }
                case VistoriaRegistrada(Long vistoriaId, Long pontoId) -> {
                    pontoRepository.findById(pontoId).ifPresent(p -> sincronizarPontoCompleto(g, p));
                    vistoriaRepository.findById(vistoriaId).ifPresent(v -> sincronizarVistoria(g, v));
                    removerOrfaos(g);
                }
                case EvidenciaAtualizada(Long evidenciaId) -> {
                    evidenciaRepository.findById(evidenciaId).ifPresent(ev -> sincronizarEvidencia(g, ev));
                }
                case CamadasAlteradas(Long camadaId) -> {
                    List<Ponto> pontos = pontoRepository.findAll();
                    for (Ponto p : pontos) {
                        g.atualizarOuAdicionarNo(ClasseOntologia.PONTO, p.getId(), p.getCodigoExibicao(), atributosPonto(p));
                    }
                    String camadaNoId = id(ClasseOntologia.CAMADA_VULNERABILIDADE, camadaId);
                    CamadaVulnerabilidade camada = camadaRepository.findById(camadaId).orElse(null);
                    if (camada != null && camada.isAtiva()) {
                        sincronizarCamada(g, camada);
                        g.removerArestasDeDestino(camadaNoId, RelacaoOntologia.SOB_INFLUENCIA_DE);
                        for (Ponto p : pontos) {
                            if (camada.obterGeometria().contemPonto(p.getLatitude().doubleValue(), p.getLongitude().doubleValue())) {
                                g.relacionar(id(ClasseOntologia.PONTO, p.getId()), RelacaoOntologia.SOB_INFLUENCIA_DE, camadaNoId);
                            }
                        }
                    } else {
                        g.removerNo(camadaNoId);
                    }
                }
                case CensoAlterado() -> {
                    for (GrafoConhecimento.No no : g.nosDaClasse(ClasseOntologia.SETOR_CENSITARIO)) {
                        g.removerNo(no.id());
                    }
                    List<SetorCensitario> setores = setorRepository.findByImportacaoAtivaTrue();
                    for (SetorCensitario s : setores) {
                        sincronizarSetor(g, s);
                    }
                    for (Ponto p : pontoRepository.findAll()) {
                        String pontoNoId = id(ClasseOntologia.PONTO, p.getId());
                        if (!g.contemNo(pontoNoId)) {
                            continue;
                        }
                        double lat = p.getLatitude().doubleValue();
                        double lon = p.getLongitude().doubleValue();
                        for (SetorCensitario s : setores) {
                            if (s.contemCoordenada(lat, lon)) {
                                g.relacionar(pontoNoId, RelacaoOntologia.DENTRO_DO_SETOR, id(ClasseOntologia.SETOR_CENSITARIO, s.getId()));
                            }
                        }
                    }
                    removerOrfaos(g);
                }
                case UsuarioAtualizado(Long usuarioId) -> {
                    String fiscalNoId = id(ClasseOntologia.FISCAL, usuarioId);
                    if (g.contemNo(fiscalNoId)) {
                        usuarioRepository.findById(usuarioId).ifPresent(u -> atualizarNoFiscal(g, u));
                    }
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Transactional(readOnly = true)
    public GrafoConhecimento montar() {
        montagensCompletas.incrementAndGet();
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
            sincronizarSetor(g, s);
        }

        List<CamadaVulnerabilidade> camadas = camadaRepository.findByAtivaTrue();
        for (CamadaVulnerabilidade c : camadas) {
            sincronizarCamada(g, c);
        }

        List<Ponto> pontos = pontoRepository.findAll();
        for (Ponto p : pontos) {
            String pontoId = g.adicionarNo(ClasseOntologia.PONTO, p.getId(), p.getCodigoExibicao(), atributosPonto(p)).id();

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

        // Vizinhança é simétrica: uma aresta por par, sempre do menor ID para o maior.
        for (int i = 0; i < pontos.size(); i++) {
            for (int j = i + 1; j < pontos.size(); j++) {
                Ponto a = pontos.get(i);
                Ponto b = pontos.get(j);
                double metros = a.distanciaEmMetrosPara(b.getLatitude().doubleValue(), b.getLongitude().doubleValue());
                if (metros <= RelacaoOntologia.RAIO_VIZINHANCA_METROS) {
                    Ponto menor = a.getId() < b.getId() ? a : b;
                    Ponto maior = a.getId() < b.getId() ? b : a;
                    g.relacionar(id(ClasseOntologia.PONTO, menor.getId()), RelacaoOntologia.VIZINHO_DE,
                            id(ClasseOntologia.PONTO, maior.getId()));
                }
            }
        }

        for (Relato r : relatoRepository.findAll()) {
            sincronizarRelato(g, r);
        }

        for (Evidencia ev : evidenciaRepository.findAll()) {
            sincronizarEvidencia(g, ev);
        }

        for (Vistoria v : vistoriaRepository.findAll()) {
            sincronizarVistoria(g, v);
        }

        return g;
    }

    private void sincronizarNoEAtribuicaoPonto(GrafoConhecimento g, Ponto p) {
        String pontoId = g.atualizarOuAdicionarNo(ClasseOntologia.PONTO, p.getId(), p.getCodigoExibicao(), atributosPonto(p)).id();
        g.removerArestasDeOrigem(pontoId, RelacaoOntologia.ATRIBUIDO_A);
        if (p.getFiscalAtribuido() != null) {
            g.relacionar(pontoId, RelacaoOntologia.ATRIBUIDO_A, noFiscal(g, p.getFiscalAtribuido()));
        }
    }

    private void sincronizarPontoCompleto(GrafoConhecimento g, Ponto p) {
        String pontoId = g.atualizarOuAdicionarNo(ClasseOntologia.PONTO, p.getId(), p.getCodigoExibicao(), atributosPonto(p)).id();

        g.removerArestasDeOrigem(pontoId, RelacaoOntologia.E_DO_TIPO);
        g.relacionar(pontoId, RelacaoOntologia.E_DO_TIPO, id(ClasseOntologia.TIPO_OCORRENCIA, p.getTipoOcorrencia().name()));

        g.removerArestasDeOrigem(pontoId, RelacaoOntologia.LOCALIZADO_EM);
        String bairroId = noBairro(g, p.getBairro());
        if (bairroId != null) {
            g.relacionar(pontoId, RelacaoOntologia.LOCALIZADO_EM, bairroId);
        }

        g.removerArestasDeOrigem(pontoId, RelacaoOntologia.ATRIBUIDO_A);
        if (p.getFiscalAtribuido() != null) {
            g.relacionar(pontoId, RelacaoOntologia.ATRIBUIDO_A, noFiscal(g, p.getFiscalAtribuido()));
        }

        double lat = p.getLatitude().doubleValue();
        double lon = p.getLongitude().doubleValue();

        g.removerArestasDeOrigem(pontoId, RelacaoOntologia.DENTRO_DO_SETOR);
        for (SetorCensitario s : setorRepository.findByImportacaoAtivaTrue()) {
            if (s.contemCoordenada(lat, lon)) {
                g.relacionar(pontoId, RelacaoOntologia.DENTRO_DO_SETOR, id(ClasseOntologia.SETOR_CENSITARIO, s.getId()));
            }
        }

        g.removerArestasDeOrigem(pontoId, RelacaoOntologia.SOB_INFLUENCIA_DE);
        for (CamadaVulnerabilidade c : camadaRepository.findByAtivaTrue()) {
            if (c.obterGeometria().contemPonto(lat, lon)) {
                g.relacionar(pontoId, RelacaoOntologia.SOB_INFLUENCIA_DE, id(ClasseOntologia.CAMADA_VULNERABILIDADE, c.getId()));
            }
        }

        g.removerArestasComNo(pontoId, RelacaoOntologia.VIZINHO_DE);
        for (Ponto outro : pontoRepository.findAll()) {
            if (outro.getId().equals(p.getId())) {
                continue;
            }
            double metros = p.distanciaEmMetrosPara(outro.getLatitude().doubleValue(), outro.getLongitude().doubleValue());
            if (metros <= RelacaoOntologia.RAIO_VIZINHANCA_METROS) {
                Ponto menor = p.getId() < outro.getId() ? p : outro;
                Ponto maior = p.getId() < outro.getId() ? outro : p;
                g.relacionar(id(ClasseOntologia.PONTO, menor.getId()), RelacaoOntologia.VIZINHO_DE,
                        id(ClasseOntologia.PONTO, maior.getId()));
            }
        }
    }

    private static Map<String, Object> atributosPonto(Ponto p) {
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
        return at;
    }

    private static void sincronizarSetor(GrafoConhecimento g, SetorCensitario s) {
        Map<String, Object> at = new LinkedHashMap<>();
        at.put("Rede de água", s.getPctRedeAgua().stripTrailingZeros().toPlainString() + "% dos domicílios");
        at.put("Rede de esgoto", s.getPctRedeEsgoto().stripTrailingZeros().toPlainString() + "% dos domicílios");
        g.atualizarOuAdicionarNo(ClasseOntologia.SETOR_CENSITARIO, s.getId(), "Setor " + s.getCodigoSetor(), at);
        String bairroId = noBairro(g, s.getBairro());
        if (bairroId != null) {
            g.relacionar(id(ClasseOntologia.SETOR_CENSITARIO, s.getId()), RelacaoOntologia.PERTENCE_A, bairroId);
        }
    }

    private static void sincronizarCamada(GrafoConhecimento g, CamadaVulnerabilidade c) {
        Map<String, Object> at = new LinkedHashMap<>();
        at.put("Categoria", c.getCategoria().getRotulo());
        at.put("Peso de urgência", (int) c.getPesoUrgencia());
        at.put("Geometria", c.obterGeometria().descrever());
        at.put("Fonte", c.getFonteDado());
        g.atualizarOuAdicionarNo(ClasseOntologia.CAMADA_VULNERABILIDADE, c.getId(), c.getNome(), at);
    }

    private static void sincronizarRelato(GrafoConhecimento g, Relato r) {
        Map<String, Object> at = new LinkedHashMap<>();
        at.put("Registrado em", r.getDataHoraRegistro().format(DATA_BR));
        at.put("Tipo", r.getTipoOcorrencia().getRotulo());
        String relatoId = g.atualizarOuAdicionarNo(ClasseOntologia.RELATO, r.getId(), r.getProtocolo(), at).id();
        g.relacionar(relatoId, RelacaoOntologia.AGRUPADO_EM, id(ClasseOntologia.PONTO, r.getPonto().getId()));
    }

    private static void sincronizarEvidencia(GrafoConhecimento g, Evidencia ev) {
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
        String evId = g.atualizarOuAdicionarNo(classe, ev.getId(), classe.getRotulo() + " " + ev.getId(), at).id();
        g.relacionar(evId, RelacaoOntologia.COMPROVA, id(ClasseOntologia.RELATO, ev.getRelato().getId()));
    }

    private static void sincronizarVistoria(GrafoConhecimento g, Vistoria v) {
        Map<String, Object> at = new LinkedHashMap<>();
        at.put("Resultado", v.getResultado().getRotulo());
        at.put("Data", v.getDataHoraVistoria().format(DATA_BR));
        at.put("Tipo validado", v.getTipoOcorrenciaValidado().getRotulo());
        String vistoriaId = g.atualizarOuAdicionarNo(ClasseOntologia.VISTORIA, v.getId(), "Vistoria " + v.getId(), at).id();
        g.relacionar(vistoriaId, RelacaoOntologia.VERIFICA, id(ClasseOntologia.PONTO, v.getPonto().getId()));
        g.relacionar(vistoriaId, RelacaoOntologia.REALIZADA_POR, noFiscal(g, v.getFiscal()));
    }

    private static void removerOrfaos(GrafoConhecimento g) {
        for (GrafoConhecimento.No no : g.nosDaClasse(ClasseOntologia.BAIRRO)) {
            if (!g.temArestaComNo(no.id())) {
                g.removerNo(no.id());
            }
        }
        for (GrafoConhecimento.No no : g.nosDaClasse(ClasseOntologia.FISCAL)) {
            if (!g.temArestaComNo(no.id())) {
                g.removerNo(no.id());
            }
        }
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
        return atualizarNoFiscal(g, fiscal);
    }

    private static String atualizarNoFiscal(GrafoConhecimento g, Usuario fiscal) {
        Map<String, Object> at = new LinkedHashMap<>();
        at.put("Situação", fiscal.isAtivo() ? "Ativo" : "Desativado");
        return g.atualizarOuAdicionarNo(ClasseOntologia.FISCAL, fiscal.getId(), fiscal.getNomeCompleto(), at).id();
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
