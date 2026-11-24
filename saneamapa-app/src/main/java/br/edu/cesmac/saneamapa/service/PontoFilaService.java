package br.edu.cesmac.saneamapa.service;

import br.edu.cesmac.saneamapa.domain.enums.NivelUrgencia;
import br.edu.cesmac.saneamapa.domain.enums.StatusPonto;
import br.edu.cesmac.saneamapa.domain.enums.TipoEventoAuditoria;
import br.edu.cesmac.saneamapa.domain.enums.TipoOcorrencia;
import br.edu.cesmac.saneamapa.domain.enums.TipoObjetoAuditoria;
import br.edu.cesmac.saneamapa.domain.exception.RecursoNaoEncontradoException;
import br.edu.cesmac.saneamapa.domain.model.Evidencia;
import br.edu.cesmac.saneamapa.domain.model.EvidenciaAudio;
import br.edu.cesmac.saneamapa.domain.model.ItemFilaVistoria;
import br.edu.cesmac.saneamapa.domain.model.Ponto;
import br.edu.cesmac.saneamapa.domain.model.Relato;
import br.edu.cesmac.saneamapa.domain.model.SetorCensitario;
import br.edu.cesmac.saneamapa.domain.model.Usuario;
import br.edu.cesmac.saneamapa.domain.service.CalculadoraPontuacaoFila;
import br.edu.cesmac.saneamapa.dto.PainelDtos.*;
import br.edu.cesmac.saneamapa.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class PontoFilaService {

    private final PontoRepository pontoRepository;
    private final ItemFilaVistoriaRepository filaRepository;
    private final RelatoRepository relatoRepository;
    private final EvidenciaRepository evidenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SetorCensitarioRepository setorRepository;
    private final LogAuditoriaRepository logRepository;
    private final CalculadoraPontuacaoFila calculadoraPontuacao;
    private final AuditoriaService auditoriaService;
    private final ApplicationEventPublisher eventPublisher;

    public PontoFilaService(PontoRepository pontoRepository,
                            ItemFilaVistoriaRepository filaRepository,
                            RelatoRepository relatoRepository,
                            EvidenciaRepository evidenciaRepository,
                            UsuarioRepository usuarioRepository,
                            SetorCensitarioRepository setorRepository,
                            LogAuditoriaRepository logRepository,
                            CalculadoraPontuacaoFila calculadoraPontuacao,
                            AuditoriaService auditoriaService,
                            ApplicationEventPublisher eventPublisher) {
        this.pontoRepository = pontoRepository;
        this.filaRepository = filaRepository;
        this.relatoRepository = relatoRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.usuarioRepository = usuarioRepository;
        this.setorRepository = setorRepository;
        this.logRepository = logRepository;
        this.calculadoraPontuacao = calculadoraPontuacao;
        this.auditoriaService = auditoriaService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<PontoResumoDto> listarPainel(TipoOcorrencia tipo, StatusPonto status, String bairro,
                                             Integer dias, Integer minRelatos, NivelUrgencia urgencia) {
        List<ItemFilaVistoria> fila = filaRepository.listarFilaOrdenada();
        Map<Long, ItemFilaVistoria> mapaFila = new HashMap<>();
        Map<Long, Integer> posicoes = new HashMap<>();
        int pos = 1;
        for (ItemFilaVistoria item : fila) {
            mapaFila.put(item.getPonto().getId(), item);
            posicoes.put(item.getPonto().getId(), pos++);
        }

        LocalDate corte = (dias != null && dias > 0) ? LocalDate.now().minusDays(dias) : null;

        return pontoRepository.findAll().stream()
                .filter(p -> tipo == null || p.getTipoOcorrencia() == tipo)
                .filter(p -> status == null || p.getStatus() == status)
                .filter(p -> bairro == null || bairro.isBlank() || p.getBairro().equalsIgnoreCase(bairro.trim()))
                .filter(p -> corte == null || !p.getDataUltimoRelato().isBefore(corte))
                .filter(p -> minRelatos == null || p.getQuantidadeRelatos() >= minRelatos)
                .filter(p -> urgencia == null || p.getNivelUrgencia() == urgencia)
                .sorted(Comparator
                        .comparing((Ponto p) -> p.getStatus().estaNaFila() ? 0 : 1)
                        .thenComparing(p -> posicoes.getOrDefault(p.getId(), Integer.MAX_VALUE))
                        .thenComparing(Ponto::getDataUltimoRelato, Comparator.reverseOrder()))
                .map(p -> toResumoDto(p, mapaFila.get(p.getId()), posicoes.get(p.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PontoResumoDto> listarResumoPublicoLgpd() {
        return pontoRepository.findAll().stream()
                .map(p -> new PontoResumoDto(
                        p.getId(), p.getCodigoExibicao(), p.getTipoOcorrencia(), p.getTipoOcorrencia().getRotulo(),
                        p.getStatus(), p.getStatus().getRotulo(),
                        null, null, 0, null, // RF006: morador/público não vê urgência
                        p.getTrechoRua(), p.getBairro(), p.getQuantidadeRelatos(),
                        0, null, null, null, null,
                        p.isReincidencia(), p.isPosicaoManualAlgumRelato(),
                        p.getDataPrimeiroRelato(), p.getDataUltimoRelato(),
                        null, null // RF002 / LGPD: coordenada exata oculta fora da equipe interna
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public PontoDetalheDto obterDetalhe(Long pontoId) {
        return obterDetalheInterno(pontoId, false);
    }

    @Transactional(readOnly = true)
    public PontoDetalheDto obterDetalheCampo(Long pontoId) {
        return obterDetalheInterno(pontoId, true);
    }

    private PontoDetalheDto obterDetalheInterno(Long pontoId, boolean coordenadaExata) {
        Ponto ponto = buscarPonto(pontoId);
        ItemFilaVistoria item = filaRepository.findByPontoId(pontoId).orElse(null);

        List<EvidenciaResumoDto> evidencias = evidenciaRepository.findByRelatoPontoIdOrderByIdAsc(pontoId).stream()
                .map(ev -> new EvidenciaResumoDto(
                        ev.getId(),
                        ev.getTipoEvidencia().name(),
                        ev.getOrigem().name(),
                        ev.isEvidenciaDivergente(),
                        ev.descreverTratamento(),
                        ev.getTranscricao(),
                        ev.getRegiaoDesfoqueManual(),
                        ev.getDuracaoSegundos(),
                        ev instanceof EvidenciaAudio ? null : ev.getArquivoUsoCaminho()
                ))
                .toList();

        List<RelatoVinculadoDto> relatos = relatoRepository.findByPontoIdOrderByDataHoraRegistroDesc(pontoId).stream()
                .map(r -> new RelatoVinculadoDto(r.getId(), r.getProtocolo(), r.getDataHoraRegistro(),
                        r.getDescricao(), r.getLogradouro(), r.getBairro(), r.getPontoReferencia()))
                .toList();

        List<HistoricoEventoDto> historico = logRepository.findTop20ByTipoObjetoAndObjetoIdOrderByDataHoraDesc(
                TipoObjetoAuditoria.PONTO, pontoId).stream()
                .map(l -> new HistoricoEventoDto(l.getDataHora(), l.getTipoEvento().getRotulo(), l.getAtorNome(), l.getDetalhes()))
                .toList();

        String censoResumo = montarResumoCenso(ponto);
        PontoResumoDto resumo = toResumoDto(ponto, item, null, coordenadaExata);
        return new PontoDetalheDto(resumo, censoResumo, evidencias, relatos, historico);
    }

    @Transactional
    public CoordenadaExataDto revelarCoordenadaExata(Long pontoId, Long usuarioId) {
        Ponto ponto = buscarPonto(pontoId);
        auditoriaService.registrar(usuarioId, TipoEventoAuditoria.ACESSO_COORDENADA_EXATA,
                TipoObjetoAuditoria.PONTO, ponto.getId(),
                "Consulta à coordenada exata do ponto " + ponto.getCodigoExibicao());
        return new CoordenadaExataDto(ponto.getLatitude(), ponto.getLongitude(),
                "Acesso à coordenada exata registrado na auditoria LGPD.");
    }

    @Transactional
    public AcessoContatosResponse revelarContatosIdentificados(Long pontoId, Long usuarioId) {
        Ponto ponto = buscarPonto(pontoId);
        List<ContatoIdentificadoDto> contatos = relatoRepository.findByPontoIdOrderByDataHoraRegistroDesc(pontoId).stream()
                .filter(r -> !r.isAnonimizado()) // RNF20: nome e telefone já eliminados
                .map(r -> new ContatoIdentificadoDto(r.getId(), r.getNomeMorador(), r.getTelefoneMorador()))
                .toList();
        auditoriaService.registrar(usuarioId, TipoEventoAuditoria.ACESSO_CONTATO_MORADOR,
                TipoObjetoAuditoria.PONTO, ponto.getId(),
                "Consulta a nome e telefone de " + contatos.size() + " relato(s) do ponto " + ponto.getCodigoExibicao());
        return new AcessoContatosResponse(contatos, "Acesso aos dados pessoais registrado na auditoria LGPD.");
    }

    @Transactional
    public AcessoAudioResponse registrarAcessoAudio(Long evidenciaId, Long usuarioId) {
        Evidencia ev = evidenciaRepository.findById(evidenciaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evidência não encontrada.", "Selecione outra evidência."));
        if (!(ev instanceof EvidenciaAudio audio)) {
            throw new RecursoNaoEncontradoException("A evidência informada não é um áudio.", "Selecione um áudio do relato.");
        }
        auditoriaService.registrar(usuarioId, TipoEventoAuditoria.AUDIO_OUVIDO,
                TipoObjetoAuditoria.EVIDENCIA, audio.getId(),
                "Reprodução de áudio do relato " + audio.getRelato().getProtocolo());
        return new AcessoAudioResponse(audio.getId(), audio.getArquivoUsoCaminho(), audio.getTranscricao(),
                "Reprodução do áudio registrada na auditoria LGPD.");
    }

    @Transactional
    public PontoResumoDto atribuirFiscal(Long pontoId, Long fiscalId, Long autorId) {
        Ponto ponto = buscarPonto(pontoId);
        Usuario fiscal = null;
        if (fiscalId != null) {
            fiscal = usuarioRepository.findById(fiscalId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Fiscal não encontrado.", "Escolha outro fiscal ativo."));
        }
        ponto.atribuirFiscal(fiscal);
        pontoRepository.save(ponto);

        ItemFilaVistoria item = filaRepository.findByPontoId(pontoId).orElse(null);
        if (item != null) {
            item.sincronizarFiscal(fiscal);
            filaRepository.save(item);
        }

        if (fiscal != null) {
            auditoriaService.registrar(autorId, TipoEventoAuditoria.ATRIBUICAO_PONTO,
                    TipoObjetoAuditoria.PONTO, ponto.getId(),
                    "Ponto " + ponto.getCodigoExibicao() + " atribuído a " + fiscal.getNomeCompleto() + " (em vistoria).");
        } else {
            auditoriaService.registrar(autorId, TipoEventoAuditoria.RETIRADA_ATRIBUICAO,
                    TipoObjetoAuditoria.PONTO, ponto.getId(),
                    "Atribuição retirada do ponto " + ponto.getCodigoExibicao() + " (retornou a aguardando vistoria).");
        }

        eventPublisher.publishEvent(new EventoMutacaoGrafo.PontoAtribuicaoAlterada(ponto.getId()));

        return toResumoDto(ponto, item, null);
    }

    @Transactional(readOnly = true)
    public IndicadoresResponse obterIndicadores(Integer dias, String bairro) {
        LocalDate corte = (dias != null && dias > 0) ? LocalDate.now().minusDays(dias) : null;
        List<Ponto> filtrados = pontoRepository.findAll().stream()
                .filter(p -> corte == null || !p.getDataUltimoRelato().isBefore(corte))
                .filter(p -> bairro == null || bairro.isBlank() || p.getBairro().equalsIgnoreCase(bairro.trim()))
                .toList();

        List<ItemIndicadorDto> porStatus = Arrays.stream(StatusPonto.values())
                .map(s -> new ItemIndicadorDto(s.name(), s.getRotulo(),
                        filtrados.stream().filter(p -> p.getStatus() == s).count()))
                .toList();

        List<ItemIndicadorDto> porTipo = Arrays.stream(TipoOcorrencia.values())
                .map(t -> new ItemIndicadorDto(t.name(), t.getRotulo(),
                        filtrados.stream().filter(p -> p.getTipoOcorrencia() == t).count()))
                .toList();

        List<ItemIndicadorDto> porUrgencia = List.of(
                new ItemIndicadorDto(NivelUrgencia.CRITICA.name(), NivelUrgencia.CRITICA.getRotulo(),
                        filtrados.stream().filter(p -> p.getNivelUrgencia() == NivelUrgencia.CRITICA).count()),
                new ItemIndicadorDto(NivelUrgencia.ELEVADA.name(), NivelUrgencia.ELEVADA.getRotulo(),
                        filtrados.stream().filter(p -> p.getNivelUrgencia() == NivelUrgencia.ELEVADA).count()),
                new ItemIndicadorDto(NivelUrgencia.NORMAL.name(), NivelUrgencia.NORMAL.getRotulo(),
                        filtrados.stream().filter(p -> p.getNivelUrgencia() == NivelUrgencia.NORMAL).count())
        );

        Map<String, Long> porBairroMap = new LinkedHashMap<>();
        filtrados.forEach(p -> porBairroMap.merge(p.getBairro(), 1L, Long::sum));
        List<ItemIndicadorDto> porBairro = porBairroMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new ItemIndicadorDto(e.getKey(), e.getKey(), e.getValue()))
                .toList();

        return new IndicadoresResponse(filtrados.size(), porStatus, porTipo, porUrgencia, porBairro);
    }

    /**
     * RF005 nota 1: recalcula a pontuação da fila uma vez por dia.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void recalcularPontuacoesDiarias() {
        recalcularPontuacoesDaFila();
    }

    /**
     * Recalcula a pontuação de todos os itens da fila com consultas agregadas: a contagem por bairro
     * é lida uma vez, em vez de uma consulta por ponto.
     */
    @Transactional
    public void recalcularPontuacoesDaFila() {
        List<ItemFilaVistoria> fila = filaRepository.listarFilaOrdenada();
        Map<String, Long> naFilaPorBairro = new HashMap<>();
        for (ItemFilaVistoria item : fila) {
            String bairro = item.getPonto().getBairro();
            if (bairro != null) naFilaPorBairro.merge(bairro, 1L, Long::sum);
        }
        for (ItemFilaVistoria item : fila) {
            Ponto p = item.getPonto();
            long naFilaBairro = p.getBairro() != null ? naFilaPorBairro.getOrDefault(p.getBairro(), 0L) : 0L;
            item.atualizarPontuacao(calculadoraPontuacao.calcular(p, naFilaBairro));
        }
        filaRepository.saveAll(fila);
    }

    private String montarResumoCenso(Ponto ponto) {
        List<SetorCensitario> setores = setorRepository.findByImportacaoAtivaTrue();
        for (SetorCensitario s : setores) {
            if (s.contemCoordenada(ponto.getLatitude().doubleValue(), ponto.getLongitude().doubleValue())) {
                if ("AGUA".equals(ponto.getTipoOcorrencia().getServicoCenso())) {
                    return String.format("Setor %s: %.0f%% dos domicílios aparecem ligados à rede geral de água.",
                            s.getCodigoSetor(), s.getPctRedeAgua());
                }
                return String.format("Setor %s: %.0f%% dos domicílios aparecem ligados à rede geral de esgoto.",
                        s.getCodigoSetor(), s.getPctRedeEsgoto());
            }
        }
        return "Sem setor censitário ativo mapeado para esta coordenada.";
    }

    public PontoResumoDto toResumoDto(Ponto p, ItemFilaVistoria item, Integer posFila) {
        return toResumoDto(p, item, posFila, false);
    }

    public PontoResumoDto toResumoDto(Ponto p, ItemFilaVistoria item, Integer posFila, boolean coordenadaExata) {
        long espera = Math.max(0, ChronoUnit.DAYS.between(p.getDataPrimeiroRelato(), LocalDate.now()));
        return new PontoResumoDto(
                p.getId(),
                p.getCodigoExibicao(),
                p.getTipoOcorrencia(),
                p.getTipoOcorrencia().getRotulo(),
                p.getStatus(),
                p.getStatus().getRotulo(),
                p.getNivelUrgencia(),
                p.getNivelUrgencia().getRotulo(),
                p.getNivelUrgencia().getNivelMedidor(),
                p.getMotivoUrgencia(),
                p.getTrechoRua(),
                p.getBairro(),
                p.getQuantidadeRelatos(),
                espera,
                item != null && p.getStatus().estaNaFila() ? item.getPontuacao() : null,
                posFila,
                p.getFiscalAtribuido() != null ? p.getFiscalAtribuido().getId() : null,
                p.getFiscalAtribuido() != null ? p.getFiscalAtribuido().getNomeCompleto() : "—",
                p.isReincidencia(),
                p.isPosicaoManualAlgumRelato(),
                p.getDataPrimeiroRelato(),
                p.getDataUltimoRelato(),
                coordenadaExata ? p.getLatitude() : p.getLatitude().setScale(3, RoundingMode.HALF_UP),
                coordenadaExata ? p.getLongitude() : p.getLongitude().setScale(3, RoundingMode.HALF_UP)
        );
    }

    private Ponto buscarPonto(Long id) {
        return pontoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ponto não encontrado.", "Selecione outro ponto no mapa."));
    }
}
