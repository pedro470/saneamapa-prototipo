package br.edu.cesmac.saneamapa.service;

import br.edu.cesmac.saneamapa.domain.enums.TipoEventoAuditoria;
import br.edu.cesmac.saneamapa.domain.enums.TipoObjetoAuditoria;
import br.edu.cesmac.saneamapa.domain.exception.RegraNegocioException;
import br.edu.cesmac.saneamapa.domain.model.CamadaVulnerabilidade;
import br.edu.cesmac.saneamapa.domain.model.Evidencia;
import br.edu.cesmac.saneamapa.domain.model.EvidenciaAudio;
import br.edu.cesmac.saneamapa.domain.model.EvidenciaFoto;
import br.edu.cesmac.saneamapa.domain.model.EvidenciaVideo;
import br.edu.cesmac.saneamapa.domain.model.ItemFilaVistoria;
import br.edu.cesmac.saneamapa.domain.model.Ponto;
import br.edu.cesmac.saneamapa.domain.model.Relato;
import br.edu.cesmac.saneamapa.domain.service.CalculadoraPontuacaoFila;
import br.edu.cesmac.saneamapa.domain.service.GeoUtils;
import br.edu.cesmac.saneamapa.domain.service.ServicoClassificacaoUrgencia;
import br.edu.cesmac.saneamapa.domain.service.ServicoTranscricaoAudio;
import br.edu.cesmac.saneamapa.dto.RelatoDtos.CriarRelatoRequest;
import br.edu.cesmac.saneamapa.dto.RelatoDtos.EvidenciaInput;
import br.edu.cesmac.saneamapa.dto.RelatoDtos.RelatoCriadoResponse;
import br.edu.cesmac.saneamapa.repository.CamadaVulnerabilidadeRepository;
import br.edu.cesmac.saneamapa.repository.ItemFilaVistoriaRepository;
import br.edu.cesmac.saneamapa.repository.PontoRepository;
import br.edu.cesmac.saneamapa.repository.RelatoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class RelatoService {

    private static final double RAIO_AGRUPAMENTO_METROS = 50.0;
    private static final int LIMITE_RELATOS_24H_TELEFONE = 5;

    private final RelatoRepository relatoRepository;
    private final PontoRepository pontoRepository;
    private final ItemFilaVistoriaRepository filaRepository;
    private final CamadaVulnerabilidadeRepository camadaRepository;
    private final ServicoClassificacaoUrgencia servicoUrgencia;
    private final ServicoTranscricaoAudio servicoTranscricao;
    private final CalculadoraPontuacaoFila calculadoraPontuacao;
    private final AuditoriaService auditoriaService;

    public RelatoService(RelatoRepository relatoRepository,
                         PontoRepository pontoRepository,
                         ItemFilaVistoriaRepository filaRepository,
                         CamadaVulnerabilidadeRepository camadaRepository,
                         ServicoClassificacaoUrgencia servicoUrgencia,
                         ServicoTranscricaoAudio servicoTranscricao,
                         CalculadoraPontuacaoFila calculadoraPontuacao,
                         AuditoriaService auditoriaService) {
        this.relatoRepository = relatoRepository;
        this.pontoRepository = pontoRepository;
        this.filaRepository = filaRepository;
        this.camadaRepository = camadaRepository;
        this.servicoUrgencia = servicoUrgencia;
        this.servicoTranscricao = servicoTranscricao;
        this.calculadoraPontuacao = calculadoraPontuacao;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public RelatoCriadoResponse registrarRelato(CriarRelatoRequest req) {
        OffsetDateTime agora = req.dataHoraRegistro() != null ? req.dataHoraRegistro() : OffsetDateTime.now();

        if (req.identificado() && req.telefoneMorador() != null) {
            long recentes = relatoRepository.countByTelefoneMoradorAndDataHoraRegistroAfter(
                    req.telefoneMorador().trim(), agora.minusHours(24)
            );
            if (recentes >= LIMITE_RELATOS_24H_TELEFONE) {
                throw new RegraNegocioException(
                        "Limite de 5 relatos em 24 horas atingido para este telefone (RS001).",
                        "Aguarde completar 24 horas desde os envios anteriores antes de registrar novo relato identificado."
                );
            }
        }

        // RF004: busca o ponto mais próximo do mesmo tipo situado a até 50 metros
        Ponto pontoExistente = pontoRepository.findByTipoOcorrencia(req.tipoOcorrencia()).stream()
                .filter(p -> p.distanciaEmMetrosPara(req.latitude().doubleValue(), req.longitude().doubleValue()) <= RAIO_AGRUPAMENTO_METROS)
                .min(Comparator.comparingDouble(p -> p.distanciaEmMetrosPara(req.latitude().doubleValue(), req.longitude().doubleValue())))
                .orElse(null);

        boolean agrupado = pontoExistente != null;
        Ponto ponto;
        if (agrupado) {
            ponto = pontoExistente;
            ponto.registrarVinculoNovoRelato(agora.toLocalDate(), req.posicaoManual());
        } else {
            String codigo = gerarCodigoPonto();
            String trecho = GeoUtils.derivarTrechoRua(req.logradouro(), req.pontoReferencia());
            ponto = new Ponto(
                    codigo,
                    req.tipoOcorrencia(),
                    req.latitude(),
                    req.longitude(),
                    trecho,
                    req.bairro(),
                    agora.toLocalDate(),
                    req.posicaoManual()
            );
        }

        List<CamadaVulnerabilidade> camadasAtivas = camadaRepository.findByAtivaTrue();
        servicoUrgencia.classificarPonto(ponto, camadasAtivas);
        ponto = pontoRepository.save(ponto);

        String protocolo = (req.protocolo() != null && !req.protocolo().isBlank())
                ? req.protocolo().trim()
                : gerarProtocolo(req.identificado());

        Relato relato = new Relato(
                ponto,
                protocolo,
                agora,
                req.tipoOcorrencia(),
                req.descricao(),
                req.identificado(),
                req.nomeMorador(),
                req.telefoneMorador(),
                req.latitude(),
                req.longitude(),
                req.precisaoMetros() != null ? req.precisaoMetros() : 15,
                req.logradouro(),
                req.bairro(),
                req.pontoReferencia(),
                req.posicaoManual()
        );

        for (EvidenciaInput evIn : req.evidencias()) {
            Evidencia ev = instanciarEvidenciaPolimorfica(evIn, req);
            relato.adicionarEvidencia(ev);
            if (ev instanceof EvidenciaAudio audio) {
                relato.acrescentarTranscricao(audio.getTranscricao());
            }
        }

        relato = relatoRepository.save(relato);

        if (ponto.getStatus().estaNaFila()) {
            long naFilaBairro = pontoRepository.contarNaFilaPorBairro(ponto.getBairro());
            boolean temIdentificado = relatoRepository.existsByPontoIdAndIdentificadoTrue(ponto.getId());
            BigDecimal pontuacao = calculadoraPontuacao.calcular(ponto, naFilaBairro, temIdentificado);
            ItemFilaVistoria item = filaRepository.findByPontoId(ponto.getId())
                    .orElse(new ItemFilaVistoria(ponto, pontuacao));
            item.atualizarPontuacao(pontuacao);
            filaRepository.save(item);
        }

        if (ponto.isReincidencia()) {
            auditoriaService.registrarSistema(TipoEventoAuditoria.MUDANCA_STATUS_PONTO,
                    TipoObjetoAuditoria.PONTO, ponto.getId(),
                    "Reincidência registrada no ponto " + ponto.getCodigoExibicao() + " via protocolo " + protocolo);
        }

        return new RelatoCriadoResponse(
                relato.getProtocolo(),
                relato.getDataHoraRegistro(),
                ponto.getCodigoExibicao(),
                agrupado,
                !req.identificado(),
                agrupado
                        ? "Relato vinculado ao ponto " + ponto.getCodigoExibicao() + "."
                        : "Novo ponto " + ponto.getCodigoExibicao() + " criado e enviado para a fila."
        );
    }

    private Evidencia instanciarEvidenciaPolimorfica(EvidenciaInput in, CriarRelatoRequest req) {
        return switch (in.tipo()) {
            case FOTO -> new EvidenciaFoto(in.origem(), in.arquivoCaminho(), in.dataHoraCaptura(),
                    in.latitudeCaptura(), in.longitudeCaptura());
            case VIDEO -> new EvidenciaVideo(in.origem(), in.arquivoCaminho(), in.dataHoraCaptura(),
                    in.latitudeCaptura(), in.longitudeCaptura(), in.duracaoSegundos());
            case AUDIO -> {
                String transcricao = servicoTranscricao.transcrever(
                        in.arquivoCaminho(), req.tipoOcorrencia(), req.logradouro());
                yield new EvidenciaAudio(in.origem(), in.arquivoCaminho(), in.dataHoraCaptura(),
                        in.latitudeCaptura(), in.longitudeCaptura(), in.duracaoSegundos(), transcricao);
            }
        };
    }

    private String gerarCodigoPonto() {
        long proximo = pontoRepository.count() + 151;
        String codigo = String.format("P-%04d", proximo);
        while (pontoRepository.findByCodigoExibicao(codigo).isPresent()) {
            proximo++;
            codigo = String.format("P-%04d", proximo);
        }
        return codigo;
    }

    private String gerarProtocolo(boolean identificado) {
        String sufixo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return (identificado ? "SM-" : "SM-ANON-") + sufixo;
    }
}
