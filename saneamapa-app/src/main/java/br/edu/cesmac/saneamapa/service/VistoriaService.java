package br.edu.cesmac.saneamapa.service;

import br.edu.cesmac.saneamapa.domain.enums.TipoEventoAuditoria;
import br.edu.cesmac.saneamapa.domain.enums.TipoObjetoAuditoria;
import br.edu.cesmac.saneamapa.domain.exception.AcessoNegadoException;
import br.edu.cesmac.saneamapa.domain.exception.RecursoNaoEncontradoException;
import br.edu.cesmac.saneamapa.domain.exception.RegraNegocioException;
import br.edu.cesmac.saneamapa.domain.model.Evidencia;
import br.edu.cesmac.saneamapa.domain.model.EvidenciaAudio;
import br.edu.cesmac.saneamapa.domain.model.EvidenciaFoto;
import br.edu.cesmac.saneamapa.domain.model.ItemFilaVistoria;
import br.edu.cesmac.saneamapa.domain.model.Ponto;
import br.edu.cesmac.saneamapa.domain.model.Usuario;
import br.edu.cesmac.saneamapa.domain.model.Vistoria;
import br.edu.cesmac.saneamapa.dto.FiscalDtos.CorrigirTranscricaoRequest;
import br.edu.cesmac.saneamapa.dto.FiscalDtos.NovoDesfoqueRequest;
import br.edu.cesmac.saneamapa.dto.FiscalDtos.RegistrarVistoriaRequest;
import br.edu.cesmac.saneamapa.dto.FiscalDtos.VistoriaRegistradaResponse;
import br.edu.cesmac.saneamapa.dto.PainelDtos.AcessoAudioResponse;
import br.edu.cesmac.saneamapa.dto.PainelDtos.AcessoContatosResponse;
import br.edu.cesmac.saneamapa.dto.PainelDtos.PontoDetalheDto;
import br.edu.cesmac.saneamapa.dto.PainelDtos.PontoResumoDto;
import br.edu.cesmac.saneamapa.repository.EvidenciaRepository;
import br.edu.cesmac.saneamapa.repository.ItemFilaVistoriaRepository;
import br.edu.cesmac.saneamapa.repository.PontoRepository;
import br.edu.cesmac.saneamapa.repository.UsuarioRepository;
import br.edu.cesmac.saneamapa.repository.VistoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class VistoriaService {

    private final ItemFilaVistoriaRepository filaRepository;
    private final PontoRepository pontoRepository;
    private final UsuarioRepository usuarioRepository;
    private final VistoriaRepository vistoriaRepository;
    private final EvidenciaRepository evidenciaRepository;
    private final PontoFilaService pontoFilaService;
    private final AuditoriaService auditoriaService;

    public VistoriaService(ItemFilaVistoriaRepository filaRepository,
                           PontoRepository pontoRepository,
                           UsuarioRepository usuarioRepository,
                           VistoriaRepository vistoriaRepository,
                           EvidenciaRepository evidenciaRepository,
                           PontoFilaService pontoFilaService,
                           AuditoriaService auditoriaService) {
        this.filaRepository = filaRepository;
        this.pontoRepository = pontoRepository;
        this.usuarioRepository = usuarioRepository;
        this.vistoriaRepository = vistoriaRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.pontoFilaService = pontoFilaService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<PontoResumoDto> listarPontosAtribuidos(Long fiscalId) {
        List<ItemFilaVistoria> itens = filaRepository.listarAtribuidosAoFiscal(fiscalId);
        List<PontoResumoDto> resultado = new ArrayList<>();
        int pos = 1;
        for (ItemFilaVistoria item : itens) {
            resultado.add(pontoFilaService.toResumoDto(item.getPonto(), item, pos++));
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public PontoDetalheDto obterPontoParaVistoria(Long pontoId, Long fiscalId) {
        validarPontoAtribuido(pontoId, fiscalId);
        return pontoFilaService.obterDetalheCampo(pontoId);
    }

    @Transactional
    public AcessoContatosResponse revelarContatosParaFiscal(Long pontoId, Long fiscalId) {
        validarPontoAtribuido(pontoId, fiscalId);
        return pontoFilaService.revelarContatosIdentificados(pontoId, fiscalId);
    }

    @Transactional
    public AcessoAudioResponse ouvirAudioParaFiscal(Long evidenciaId, Long fiscalId) {
        Evidencia ev = evidenciaRepository.findById(evidenciaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evidência não encontrada.", "Atualize o ponto."));
        validarPontoAtribuido(ev.getRelato().getPonto().getId(), fiscalId);
        return pontoFilaService.registrarAcessoAudio(evidenciaId, fiscalId);
    }

    @Transactional
    public VistoriaRegistradaResponse registrarVistoria(Long pontoId, Long fiscalId, RegistrarVistoriaRequest req) {
        Ponto ponto = validarPontoAtribuido(pontoId, fiscalId);
        Usuario fiscal = usuarioRepository.findById(fiscalId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fiscal não encontrado.", "Faça login novamente."));

        Vistoria vistoria = new Vistoria(
                ponto,
                fiscal,
                req.resultado(),
                req.tipoOcorrenciaValidado(),
                req.novaLatitudePonto(),
                req.novaLongitudePonto(),
                req.latitudeFiscal(),
                req.longitudeFiscal(),
                req.observacoes(),
                req.midiasCamera(),
                req.origensMidia()
        );
        vistoria = vistoriaRepository.save(vistoria);

        ponto.aplicarResultadoVistoria(
                req.resultado(),
                req.tipoOcorrenciaValidado(),
                req.novaLatitudePonto(),
                req.novaLongitudePonto()
        );
        pontoRepository.save(ponto);

        auditoriaService.registrar(fiscalId, TipoEventoAuditoria.MUDANCA_STATUS_PONTO,
                TipoObjetoAuditoria.PONTO, ponto.getId(),
                "Vistoria registrada no ponto " + ponto.getCodigoExibicao() + ": " + req.resultado().getRotulo());

        return new VistoriaRegistradaResponse(
                vistoria.getId(),
                ponto.getCodigoExibicao(),
                ponto.getStatus().getRotulo(),
                vistoria.getDistanciaFiscalMetros(),
                vistoria.getDataHoraVistoria()
        );
    }

    @Transactional
    public String aplicarNovoDesfoque(Long evidenciaId, Long fiscalId, NovoDesfoqueRequest req) {
        Evidencia ev = evidenciaRepository.findById(evidenciaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evidência não encontrada.", "Selecione outra foto."));
        validarPontoAtribuido(ev.getRelato().getPonto().getId(), fiscalId);

        if (!(ev instanceof EvidenciaFoto foto)) {
            throw new RegraNegocioException("Novo desfoque aplica-se apenas a fotos.", "Selecione uma fotografia do relato.");
        }
        foto.aplicarNovoDesfoque(req.regiao());
        evidenciaRepository.save(foto);

        auditoriaService.registrar(fiscalId, TipoEventoAuditoria.EDICAO_FOTO,
                TipoObjetoAuditoria.EVIDENCIA, foto.getId(),
                "Novo desfoque aplicado na evidência #" + foto.getId() + " (" + req.regiao().trim() + "). Versão anterior descartada.");
        return "Desfoque aplicado na foto. A versão anterior foi descartada.";
    }

    @Transactional
    public String corrigirTranscricao(Long evidenciaId, Long fiscalId, CorrigirTranscricaoRequest req) {
        Evidencia ev = evidenciaRepository.findById(evidenciaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evidência não encontrada.", "Selecione outro áudio."));
        validarPontoAtribuido(ev.getRelato().getPonto().getId(), fiscalId);

        if (!(ev instanceof EvidenciaAudio audio)) {
            throw new RegraNegocioException("Transcrição aplica-se apenas a áudios.", "Selecione uma evidência de áudio.");
        }
        audio.definirTranscricao(req.transcricao());
        evidenciaRepository.save(audio);

        auditoriaService.registrar(fiscalId, TipoEventoAuditoria.CORRECAO_TRANSCRICAO,
                TipoObjetoAuditoria.EVIDENCIA, audio.getId(),
                "Transcrição de áudio corrigida pelo fiscal.");
        return "Transcrição atualizada com sucesso.";
    }

    private Ponto validarPontoAtribuido(Long pontoId, Long fiscalId) {
        Ponto ponto = pontoRepository.findById(pontoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ponto não encontrado.", "Volte à lista de pontos atribuídos."));
        if (ponto.getFiscalAtribuido() == null || !ponto.getFiscalAtribuido().getId().equals(fiscalId)) {
            throw new AcessoNegadoException(
                    "Você só pode visualizar e vistoriar pontos atribuídos a você (RF005 / RF007).",
                    "Volte à sua lista de pontos atribuídos."
            );
        }
        return ponto;
    }
}
