package br.edu.cesmac.saneamapa.controller;

import br.edu.cesmac.saneamapa.domain.enums.NivelUrgencia;
import br.edu.cesmac.saneamapa.domain.enums.PerfilUsuario;
import br.edu.cesmac.saneamapa.domain.enums.StatusPonto;
import br.edu.cesmac.saneamapa.domain.enums.TipoEventoAuditoria;
import br.edu.cesmac.saneamapa.domain.enums.TipoOcorrencia;
import br.edu.cesmac.saneamapa.dto.AdminDtos.*;
import br.edu.cesmac.saneamapa.dto.GrafoDtos.GrafoDto;
import br.edu.cesmac.saneamapa.dto.GrafoDtos.OntologiaDto;
import br.edu.cesmac.saneamapa.dto.PainelDtos.*;
import br.edu.cesmac.saneamapa.repository.UsuarioRepository;
import br.edu.cesmac.saneamapa.security.UsuarioAutenticado;
import br.edu.cesmac.saneamapa.service.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/prefeitura")
public class PrefeituraController {

    private final PontoFilaService pontoFilaService;
    private final UsuarioService usuarioService;
    private final CamadaVulnerabilidadeService camadaService;
    private final CensoService censoService;
    private final AuditoriaService auditoriaService;
    private final UsuarioRepository usuarioRepository;
    private final GrafoConhecimentoService grafoService;

    public PrefeituraController(PontoFilaService pontoFilaService,
                                UsuarioService usuarioService,
                                CamadaVulnerabilidadeService camadaService,
                                CensoService censoService,
                                AuditoriaService auditoriaService,
                                UsuarioRepository usuarioRepository,
                                GrafoConhecimentoService grafoService) {
        this.pontoFilaService = pontoFilaService;
        this.usuarioService = usuarioService;
        this.camadaService = camadaService;
        this.censoService = censoService;
        this.auditoriaService = auditoriaService;
        this.usuarioRepository = usuarioRepository;
        this.grafoService = grafoService;
    }

    @GetMapping("/pontos")
    public List<PontoResumoDto> listarPontos(
            @RequestParam(required = false) TipoOcorrencia tipo,
            @RequestParam(required = false) StatusPonto status,
            @RequestParam(required = false) String bairro,
            @RequestParam(required = false) Integer dias,
            @RequestParam(required = false) Integer minRelatos,
            @RequestParam(required = false) NivelUrgencia urgencia) {
        return pontoFilaService.listarPainel(tipo, status, bairro, dias, minRelatos, urgencia);
    }

    @GetMapping("/pontos/{pontoId}")
    public PontoDetalheDto obterDetalhe(@PathVariable Long pontoId) {
        return pontoFilaService.obterDetalhe(pontoId);
    }

    @PostMapping("/pontos/{pontoId}/coordenada-exata")
    public CoordenadaExataDto revelarCoordenada(@PathVariable Long pontoId,
                                                @AuthenticationPrincipal UsuarioAutenticado user) {
        return pontoFilaService.revelarCoordenadaExata(pontoId, user.id());
    }

    @PostMapping("/pontos/{pontoId}/contatos")
    public AcessoContatosResponse revelarContatos(@PathVariable Long pontoId,
                                                  @AuthenticationPrincipal UsuarioAutenticado user) {
        return pontoFilaService.revelarContatosIdentificados(pontoId, user.id());
    }

    @PostMapping("/evidencias/{evidenciaId}/audio")
    public AcessoAudioResponse ouvirAudio(@PathVariable Long evidenciaId,
                                          @AuthenticationPrincipal UsuarioAutenticado user) {
        return pontoFilaService.registrarAcessoAudio(evidenciaId, user.id());
    }

    @PutMapping("/pontos/{pontoId}/atribuicao")
    public PontoResumoDto atribuirFiscal(@PathVariable Long pontoId,
                                         @RequestBody AtribuirFiscalRequest request,
                                         @AuthenticationPrincipal UsuarioAutenticado user) {
        return pontoFilaService.atribuirFiscal(pontoId, request.fiscalId(), user.id());
    }

    @GetMapping("/fiscais-ativos")
    public List<UsuarioDto> listarFiscaisAtivos() {
        return usuarioRepository.findByPerfilAndAtivoTrueOrderByNomeCompletoAsc(PerfilUsuario.FISCAL).stream()
                .map(u -> new UsuarioDto(u.getId(), u.getCpfMascarado(), u.getNomeCompleto(),
                        u.getEmail(), u.getCelular(), u.getPerfil(), u.isAtivo()))
                .toList();
    }

    @GetMapping("/indicadores")
    public IndicadoresResponse obterIndicadores(@RequestParam(required = false) Integer dias,
                                                @RequestParam(required = false) String bairro) {
        return pontoFilaService.obterIndicadores(dias, bairro);
    }

    @GetMapping("/ontologia")
    public OntologiaDto obterOntologia() {
        return grafoService.obterOntologia();
    }

    @GetMapping("/grafo")
    public GrafoDto obterGrafo(@RequestParam(required = false) Long pontoId,
                               @RequestParam(required = false) Integer saltos) {
        return grafoService.obterGrafo(pontoId, saltos);
    }

    @GetMapping("/usuarios")
    public List<UsuarioDto> listarUsuarios() {
        return usuarioService.listarTodos();
    }

    @GetMapping("/usuarios/alteracoes")
    public List<AlteracaoUsuarioDto> listarAlteracoesUsuarios() {
        return usuarioService.listarAlteracoesRecentes();
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDto criarUsuario(@Valid @RequestBody CriarUsuarioRequest request,
                                   @AuthenticationPrincipal UsuarioAutenticado user) {
        return usuarioService.criar(request, user.id());
    }

    @PutMapping("/usuarios/{id}")
    public UsuarioDto atualizarUsuario(@PathVariable Long id,
                                       @Valid @RequestBody AtualizarUsuarioRequest request,
                                       @AuthenticationPrincipal UsuarioAutenticado user) {
        return usuarioService.atualizar(id, request, user.id());
    }

    @PatchMapping("/usuarios/{id}/alternar-ativo")
    public UsuarioDto alternarUsuarioAtivo(@PathVariable Long id,
                                           @AuthenticationPrincipal UsuarioAutenticado user) {
        return usuarioService.alternarAtivo(id, user.id());
    }

    @GetMapping("/camadas")
    public List<CamadaDto> listarCamadas() {
        return camadaService.listarTodas();
    }

    @PostMapping("/camadas")
    @ResponseStatus(HttpStatus.CREATED)
    public CamadaDto criarCamada(@Valid @RequestBody CriarCamadaRequest request) {
        return camadaService.criar(request);
    }

    @PatchMapping("/camadas/{id}/alternar-ativa")
    public CamadaDto alternarCamadaAtiva(@PathVariable Long id) {
        return camadaService.alternarAtiva(id);
    }

    @GetMapping("/censo/importacoes")
    public List<ImportacaoCensoDto> listarImportacoesCenso() {
        return censoService.listarImportacoes();
    }

    @GetMapping("/censo/setores-ativos")
    public List<SetorCensitarioDto> listarSetoresAtivos() {
        return censoService.listarSetoresAtivos();
    }

    @PostMapping("/censo/importar")
    public ImportarCensoResponse importarCenso(@RequestParam("arquivo") MultipartFile arquivo,
                                               @AuthenticationPrincipal UsuarioAutenticado user) {
        return censoService.importarArquivo(arquivo, user.id());
    }

    @PostMapping("/censo/importacoes/{id}/voltar")
    public ImportacaoCensoDto voltarImportacaoCenso(@PathVariable Long id,
                                                    @AuthenticationPrincipal UsuarioAutenticado user) {
        return censoService.voltarParaImportacao(id, user.id());
    }

    @GetMapping("/auditoria")
    public PaginaAuditoriaDto consultarAuditoria(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(defaultValue = "false") boolean apenasSistema,
            @RequestParam(required = false) TipoEventoAuditoria tipoEvento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(defaultValue = "0") int pagina) {
        return auditoriaService.pesquisar(usuarioId, apenasSistema, tipoEvento, de, ate, pagina);
    }
}
