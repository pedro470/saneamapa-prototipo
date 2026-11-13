package br.edu.cesmac.saneamapa.controller;

import br.edu.cesmac.saneamapa.dto.FiscalDtos.CorrigirTranscricaoRequest;
import br.edu.cesmac.saneamapa.dto.FiscalDtos.NovoDesfoqueRequest;
import br.edu.cesmac.saneamapa.dto.FiscalDtos.RegistrarVistoriaRequest;
import br.edu.cesmac.saneamapa.dto.FiscalDtos.VistoriaRegistradaResponse;
import br.edu.cesmac.saneamapa.dto.PainelDtos.AcessoAudioResponse;
import br.edu.cesmac.saneamapa.dto.PainelDtos.AcessoContatosResponse;
import br.edu.cesmac.saneamapa.dto.PainelDtos.PontoDetalheDto;
import br.edu.cesmac.saneamapa.dto.PainelDtos.PontoResumoDto;
import br.edu.cesmac.saneamapa.security.UsuarioAutenticado;
import br.edu.cesmac.saneamapa.service.VistoriaService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fiscal")
public class FiscalController {

    private final VistoriaService vistoriaService;

    public FiscalController(VistoriaService vistoriaService) {
        this.vistoriaService = vistoriaService;
    }

    @GetMapping("/pontos")
    public List<PontoResumoDto> listarAtribuidos(@AuthenticationPrincipal UsuarioAutenticado fiscal) {
        return vistoriaService.listarPontosAtribuidos(fiscal.id());
    }

    @GetMapping("/pontos/{pontoId}")
    public PontoDetalheDto obterDetalhe(@PathVariable Long pontoId,
                                        @AuthenticationPrincipal UsuarioAutenticado fiscal) {
        return vistoriaService.obterPontoParaVistoria(pontoId, fiscal.id());
    }

    @PostMapping("/pontos/{pontoId}/contatos")
    public AcessoContatosResponse revelarContatos(@PathVariable Long pontoId,
                                                  @AuthenticationPrincipal UsuarioAutenticado fiscal) {
        return vistoriaService.revelarContatosParaFiscal(pontoId, fiscal.id());
    }

    @PostMapping("/evidencias/{evidenciaId}/audio")
    public AcessoAudioResponse ouvirAudio(@PathVariable Long evidenciaId,
                                          @AuthenticationPrincipal UsuarioAutenticado fiscal) {
        return vistoriaService.ouvirAudioParaFiscal(evidenciaId, fiscal.id());
    }

    @PostMapping("/pontos/{pontoId}/vistoria")
    public VistoriaRegistradaResponse registrarVistoria(@PathVariable Long pontoId,
                                                        @Valid @RequestBody RegistrarVistoriaRequest request,
                                                        @AuthenticationPrincipal UsuarioAutenticado fiscal) {
        return vistoriaService.registrarVistoria(pontoId, fiscal.id(), request);
    }

    @PostMapping("/evidencias/{evidenciaId}/desfoque")
    public Map<String, String> aplicarNovoDesfoque(@PathVariable Long evidenciaId,
                                                   @Valid @RequestBody NovoDesfoqueRequest request,
                                                   @AuthenticationPrincipal UsuarioAutenticado fiscal) {
        String msg = vistoriaService.aplicarNovoDesfoque(evidenciaId, fiscal.id(), request);
        return Map.of("mensagem", msg);
    }

    @PutMapping("/evidencias/{evidenciaId}/transcricao")
    public Map<String, String> corrigirTranscricao(@PathVariable Long evidenciaId,
                                                   @Valid @RequestBody CorrigirTranscricaoRequest request,
                                                   @AuthenticationPrincipal UsuarioAutenticado fiscal) {
        String msg = vistoriaService.corrigirTranscricao(evidenciaId, fiscal.id(), request);
        return Map.of("mensagem", msg);
    }
}
