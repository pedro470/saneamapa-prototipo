package br.edu.cesmac.saneamapa.controller;

import br.edu.cesmac.saneamapa.dto.PainelDtos.PontoResumoDto;
import br.edu.cesmac.saneamapa.dto.RelatoDtos.CriarRelatoRequest;
import br.edu.cesmac.saneamapa.dto.RelatoDtos.RelatoCriadoResponse;
import br.edu.cesmac.saneamapa.service.PontoFilaService;
import br.edu.cesmac.saneamapa.service.RelatoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class MoradorController {

    private final RelatoService relatoService;
    private final PontoFilaService pontoFilaService;

    public MoradorController(RelatoService relatoService, PontoFilaService pontoFilaService) {
        this.relatoService = relatoService;
        this.pontoFilaService = pontoFilaService;
    }

    @PostMapping("/api/morador/relatos")
    @ResponseStatus(HttpStatus.CREATED)
    public RelatoCriadoResponse registrarRelato(@Valid @RequestBody CriarRelatoRequest request) {
        return relatoService.registrarRelato(request);
    }

    /**
     * Endpoint público em conformidade com a LGPD (exibe apenas trecho de rua, sem coordenadas exatas, contatos ou nível de urgência).
     */
    @GetMapping("/api/publico/pontos")
    public List<PontoResumoDto> listarPontosPublico() {
        return pontoFilaService.listarResumoPublicoLgpd();
    }

    @GetMapping("/api/publico/config")
    public Map<String, Object> obterConfiguracaoPublica() {
        return Map.of(
                "mapaEngine", "openstreetmap",
                "cidadePadrao", "Maceió, AL",
                "centroPadrao", Map.of("lat", -9.66599, "lng", -35.73500)
        );
    }
}
