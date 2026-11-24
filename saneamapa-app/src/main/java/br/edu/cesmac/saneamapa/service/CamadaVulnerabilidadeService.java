package br.edu.cesmac.saneamapa.service;

import br.edu.cesmac.saneamapa.domain.exception.RecursoNaoEncontradoException;
import br.edu.cesmac.saneamapa.domain.model.CamadaVulnerabilidade;
import br.edu.cesmac.saneamapa.domain.model.GeometriaCamada;
import br.edu.cesmac.saneamapa.domain.model.Ponto;
import br.edu.cesmac.saneamapa.domain.service.ServicoClassificacaoUrgencia;
import br.edu.cesmac.saneamapa.dto.AdminDtos.CamadaDto;
import br.edu.cesmac.saneamapa.dto.AdminDtos.CriarCamadaRequest;
import br.edu.cesmac.saneamapa.repository.CamadaVulnerabilidadeRepository;
import br.edu.cesmac.saneamapa.repository.PontoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CamadaVulnerabilidadeService {

    private final CamadaVulnerabilidadeRepository camadaRepository;
    private final PontoRepository pontoRepository;
    private final ServicoClassificacaoUrgencia servicoUrgencia;
    private final PontoFilaService pontoFilaService;
    private final ApplicationEventPublisher eventPublisher;

    public CamadaVulnerabilidadeService(CamadaVulnerabilidadeRepository camadaRepository,
                                        PontoRepository pontoRepository,
                                        ServicoClassificacaoUrgencia servicoUrgencia,
                                        PontoFilaService pontoFilaService,
                                        ApplicationEventPublisher eventPublisher) {
        this.camadaRepository = camadaRepository;
        this.pontoRepository = pontoRepository;
        this.servicoUrgencia = servicoUrgencia;
        this.pontoFilaService = pontoFilaService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<CamadaDto> listarTodas() {
        List<Ponto> pontos = pontoRepository.findAll();
        return camadaRepository.findAllByOrderByAtivaDescNomeAsc().stream()
                .map(c -> toDto(c, pontos))
                .toList();
    }

    @Transactional
    public CamadaDto criar(CriarCamadaRequest req) {
        CamadaVulnerabilidade camada = new CamadaVulnerabilidade(
                req.nome(),
                req.categoria(),
                req.tipoGeometria(),
                req.centroLatitude(),
                req.centroLongitude(),
                req.raioMetros(),
                req.poligonoCoordenadas(),
                req.pesoUrgencia(),
                req.fonteDado(),
                req.dataAtualizacao(),
                req.ativa()
        );
        camada = camadaRepository.save(camada);
        recalcularUrgenciaDeTodosOsPontos();
        eventPublisher.publishEvent(new EventoMutacaoGrafo.CamadasAlteradas(camada.getId()));
        return toDto(camada, pontoRepository.findAll());
    }

    @Transactional
    public CamadaDto alternarAtiva(Long id) {
        CamadaVulnerabilidade camada = camadaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Camada não encontrada.", "Atualize a lista de camadas."));
        camada.alterarSituacao(!camada.isAtiva());
        camadaRepository.save(camada);
        recalcularUrgenciaDeTodosOsPontos();
        eventPublisher.publishEvent(new EventoMutacaoGrafo.CamadasAlteradas(camada.getId()));
        return toDto(camada, pontoRepository.findAll());
    }

    /**
     * RF006: recalcula a urgência de todos os pontos e, em seguida, a pontuação da fila em lote
     * (antes eram várias consultas por ponto dentro da mesma requisição).
     */
    @Transactional
    public int recalcularUrgenciaDeTodosOsPontos() {
        List<CamadaVulnerabilidade> ativas = camadaRepository.findByAtivaTrue();
        List<Ponto> pontos = pontoRepository.findAll();
        for (Ponto p : pontos) {
            servicoUrgencia.classificarPonto(p, ativas);
        }
        pontoRepository.saveAll(pontos);
        pontoFilaService.recalcularPontuacoesDaFila();
        return pontos.size();
    }

    private CamadaDto toDto(CamadaVulnerabilidade c, List<Ponto> pontos) {
        GeometriaCamada geom = c.obterGeometria();
        long afetados = c.isAtiva()
                ? pontos.stream().filter(p -> geom.contemPonto(p.getLatitude().doubleValue(), p.getLongitude().doubleValue())).count()
                : 0L;
        return new CamadaDto(
                c.getId(),
                c.getNome(),
                c.getCategoria(),
                c.getCategoria().getRotulo(),
                c.getTipoGeometria(),
                geom.descrever(),
                c.getCentroLatitude(),
                c.getCentroLongitude(),
                c.getRaioMetros(),
                c.getPoligonoCoordenadas(),
                c.getPesoUrgencia(),
                c.getFonteDado(),
                c.getDataAtualizacao(),
                c.isAtiva(),
                afetados
        );
    }
}
