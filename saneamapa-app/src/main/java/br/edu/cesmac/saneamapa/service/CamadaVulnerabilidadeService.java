package br.edu.cesmac.saneamapa.service;

import br.edu.cesmac.saneamapa.domain.exception.RecursoNaoEncontradoException;
import br.edu.cesmac.saneamapa.domain.model.CamadaVulnerabilidade;
import br.edu.cesmac.saneamapa.domain.model.GeometriaCamada;
import br.edu.cesmac.saneamapa.domain.model.ItemFilaVistoria;
import br.edu.cesmac.saneamapa.domain.model.Ponto;
import br.edu.cesmac.saneamapa.domain.service.CalculadoraPontuacaoFila;
import br.edu.cesmac.saneamapa.domain.service.ServicoClassificacaoUrgencia;
import br.edu.cesmac.saneamapa.dto.AdminDtos.CamadaDto;
import br.edu.cesmac.saneamapa.dto.AdminDtos.CriarCamadaRequest;
import br.edu.cesmac.saneamapa.repository.CamadaVulnerabilidadeRepository;
import br.edu.cesmac.saneamapa.repository.ItemFilaVistoriaRepository;
import br.edu.cesmac.saneamapa.repository.PontoRepository;
import br.edu.cesmac.saneamapa.repository.RelatoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CamadaVulnerabilidadeService {

    private final CamadaVulnerabilidadeRepository camadaRepository;
    private final PontoRepository pontoRepository;
    private final ItemFilaVistoriaRepository filaRepository;
    private final RelatoRepository relatoRepository;
    private final ServicoClassificacaoUrgencia servicoUrgencia;
    private final CalculadoraPontuacaoFila calculadoraPontuacao;

    public CamadaVulnerabilidadeService(CamadaVulnerabilidadeRepository camadaRepository,
                                        PontoRepository pontoRepository,
                                        ItemFilaVistoriaRepository filaRepository,
                                        RelatoRepository relatoRepository,
                                        ServicoClassificacaoUrgencia servicoUrgencia,
                                        CalculadoraPontuacaoFila calculadoraPontuacao) {
        this.camadaRepository = camadaRepository;
        this.pontoRepository = pontoRepository;
        this.filaRepository = filaRepository;
        this.relatoRepository = relatoRepository;
        this.servicoUrgencia = servicoUrgencia;
        this.calculadoraPontuacao = calculadoraPontuacao;
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
        return toDto(camada, pontoRepository.findAll());
    }

    @Transactional
    public CamadaDto alternarAtiva(Long id) {
        CamadaVulnerabilidade camada = camadaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Camada não encontrada.", "Atualize a lista de camadas."));
        camada.alterarSituacao(!camada.isAtiva());
        camadaRepository.save(camada);
        recalcularUrgenciaDeTodosOsPontos();
        return toDto(camada, pontoRepository.findAll());
    }

    @Transactional
    public int recalcularUrgenciaDeTodosOsPontos() {
        List<CamadaVulnerabilidade> ativas = camadaRepository.findByAtivaTrue();
        List<Ponto> pontos = pontoRepository.findAll();
        for (Ponto p : pontos) {
            servicoUrgencia.classificarPonto(p, ativas);
            pontoRepository.save(p);
            if (p.getStatus().estaNaFila()) {
                ItemFilaVistoria item = filaRepository.findByPontoId(p.getId()).orElse(null);
                if (item != null) {
                    long naFilaBairro = pontoRepository.contarNaFilaPorBairro(p.getBairro());
                    boolean temIdentificado = relatoRepository.existsByPontoIdAndIdentificadoTrue(p.getId());
                    item.atualizarPontuacao(calculadoraPontuacao.calcular(p, naFilaBairro, temIdentificado));
                    filaRepository.save(item);
                }
            }
        }
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
