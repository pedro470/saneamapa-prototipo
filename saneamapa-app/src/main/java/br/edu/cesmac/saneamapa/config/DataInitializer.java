package br.edu.cesmac.saneamapa.config;

import br.edu.cesmac.saneamapa.domain.enums.*;
import br.edu.cesmac.saneamapa.domain.model.*;
import br.edu.cesmac.saneamapa.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Inicializador de dados operacionais mínimos do SaneaMapa (equipe inicial, camadas territoriais e Censo base).
 * Roda só com o banco vazio. Não insere relatos nem ocorrências e nunca apaga dados: a carga de demonstração
 * com relatos e pontos fica no database/06_seed_dados.sql, que é a única fonte desses dados.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final CamadaVulnerabilidadeRepository camadaRepository;
    private final ImportacaoCensoRepository importacaoRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository,
                           CamadaVulnerabilidadeRepository camadaRepository,
                           ImportacaoCensoRepository importacaoRepository,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.camadaRepository = camadaRepository;
        this.importacaoRepository = importacaoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Banco já tem usuários (do próprio app ou da carga 06_seed_dados.sql): nada é inserido nem apagado.
        if (usuarioRepository.count() > 0) {
            return;
        }

        String senhaPadrao = passwordEncoder.encode("Sanea2026");

        Usuario ana = usuarioRepository.save(new Usuario("218.334.001-00", "Ana Lima", "ana.lima@exemplo.gov.br", "(82) 99120-4455", senhaPadrao, PerfilUsuario.PREFEITURA, true));
        usuarioRepository.save(new Usuario("456.789.002-77", "Carlos Mendes", "carlos.mendes@exemplo.gov.br", "(82) 99876-1020", senhaPadrao, PerfilUsuario.FISCAL, true));

        camadaRepository.save(new CamadaVulnerabilidade(
                "Creche municipal Vergel", CategoriaCamada.PUBLICO_INFANTIL_ESCOLAR, TipoGeometria.PONTO_RAIO,
                new BigDecimal("-9.6713000"), new BigDecimal("-35.7490000"), 150, null,
                3, "Secretaria de Educação", LocalDate.now().minusDays(55), true));
        camadaRepository.save(new CamadaVulnerabilidade(
                "Escola estadual Benedito Bentes", CategoriaCamada.PUBLICO_INFANTIL_ESCOLAR, TipoGeometria.PONTO_RAIO,
                new BigDecimal("-9.5531000"), new BigDecimal("-35.7202000"), 200, null,
                2, "Censo Escolar 2025", LocalDate.now().minusDays(55), true));
        camadaRepository.save(new CamadaVulnerabilidade(
                "UBS Ponta Grossa", CategoriaCamada.IDOSOS_CUIDADOS, TipoGeometria.PONTO_RAIO,
                new BigDecimal("-9.6754000"), new BigDecimal("-35.7331000"), 120, null,
                2, "CNES", LocalDate.now().minusDays(40), true));
        camadaRepository.save(new CamadaVulnerabilidade(
                "Vila de pescadores do Bom Parto", CategoriaCamada.COMUNIDADES_TRADICIONAIS_VULNERAVEIS, TipoGeometria.POLIGONO,
                null, null, null, "[[-9.685,-35.756],[-9.676,-35.756],[-9.676,-35.746],[-9.685,-35.746]]",
                3, "Levantamento da assistência social", LocalDate.now().minusDays(80), true));
        camadaRepository.save(new CamadaVulnerabilidade(
                "Área com vulnerabilidade epidemiológica", CategoriaCamada.VULNERABILIDADE_EPIDEMIOLOGICA, TipoGeometria.POLIGONO,
                null, null, null, "[[-9.674,-35.754],[-9.668,-35.754],[-9.668,-35.744],[-9.674,-35.744]]",
                3, "Vigilância epidemiológica", LocalDate.now().minusDays(14), true));

        ImportacaoCenso imp = new ImportacaoCenso("setores-maceio-2022.geojson", "GEOJSON", ana, 4, true);
        imp.adicionarSetor(new SetorCensitario("270430405000123", "Vergel do Lago",
                "[[-9.675,-35.755],[-9.665,-35.755],[-9.665,-35.742],[-9.675,-35.742]]", new BigDecimal("97.50"), new BigDecimal("88.00")));
        imp.adicionarSetor(new SetorCensitario("270430405000087", "Bom Parto",
                "[[-9.686,-35.758],[-9.676,-35.758],[-9.676,-35.745],[-9.686,-35.745]]", new BigDecimal("99.00"), new BigDecimal("92.00")));
        imp.adicionarSetor(new SetorCensitario("270430405000311", "Benedito Bentes",
                "[[-9.560,-35.728],[-9.545,-35.728],[-9.545,-35.712],[-9.560,-35.712]]", new BigDecimal("97.00"), new BigDecimal("65.00")));
        imp.adicionarSetor(new SetorCensitario("270430405000198", "Jacintinho",
                "[[-9.655,-35.728],[-9.640,-35.728],[-9.640,-35.710],[-9.655,-35.710]]", new BigDecimal("90.00"), new BigDecimal("34.00")));
        importacaoRepository.save(imp);
    }
}
