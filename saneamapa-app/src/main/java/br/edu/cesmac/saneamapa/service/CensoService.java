package br.edu.cesmac.saneamapa.service;

import br.edu.cesmac.saneamapa.domain.enums.TipoEventoAuditoria;
import br.edu.cesmac.saneamapa.domain.enums.TipoObjetoAuditoria;
import br.edu.cesmac.saneamapa.domain.exception.RecursoNaoEncontradoException;
import br.edu.cesmac.saneamapa.domain.model.ImportacaoCenso;
import br.edu.cesmac.saneamapa.domain.model.SetorCensitario;
import br.edu.cesmac.saneamapa.domain.model.Usuario;
import br.edu.cesmac.saneamapa.domain.service.GeoUtils;
import br.edu.cesmac.saneamapa.dto.AdminDtos.ImportacaoCensoDto;
import br.edu.cesmac.saneamapa.dto.AdminDtos.ImportarCensoResponse;
import br.edu.cesmac.saneamapa.dto.AdminDtos.SetorCensitarioDto;
import br.edu.cesmac.saneamapa.repository.ImportacaoCensoRepository;
import br.edu.cesmac.saneamapa.repository.SetorCensitarioRepository;
import br.edu.cesmac.saneamapa.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CensoService {

    private final ImportacaoCensoRepository importacaoRepository;
    private final SetorCensitarioRepository setorRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CensoService(ImportacaoCensoRepository importacaoRepository,
                        SetorCensitarioRepository setorRepository,
                        UsuarioRepository usuarioRepository,
                        AuditoriaService auditoriaService) {
        this.importacaoRepository = importacaoRepository;
        this.setorRepository = setorRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<ImportacaoCensoDto> listarImportacoes() {
        return importacaoRepository.findAllByOrderByDataImportacaoDesc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SetorCensitarioDto> listarSetoresAtivos() {
        return setorRepository.findByImportacaoAtivaTrue().stream()
                .map(s -> new SetorCensitarioDto(
                        s.getCodigoSetor(),
                        s.getBairro(),
                        s.getPoligonoCoordenadas(),
                        s.getPctRedeAgua(),
                        s.getPctRedeEsgoto()
                ))
                .toList();
    }

    @Transactional
    public ImportarCensoResponse importarArquivo(MultipartFile arquivo, Long usuarioId) {
        if (arquivo == null || arquivo.isEmpty()) {
            return new ImportarCensoResponse(false, "Nenhum arquivo enviado.",
                    List.of("Selecione um arquivo CSV ou GeoJSON válido."), null);
        }

        String nome = arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename() : "censo.csv";
        String lower = nome.toLowerCase();
        String formato = (lower.endsWith(".geojson") || lower.endsWith(".json")) ? "GEOJSON" : "CSV";

        List<String> erros = new ArrayList<>();
        List<SetorCensitario> setores = new ArrayList<>();

        try {
            String conteudo = new String(arquivo.getBytes(), StandardCharsets.UTF_8);
            if ("GEOJSON".equals(formato)) {
                parseGeoJson(conteudo, setores, erros);
            } else {
                parseCsv(conteudo, setores, erros);
            }
        } catch (Exception e) {
            erros.add("Não foi possível ler o arquivo: " + e.getMessage());
        }

        if (!erros.isEmpty() || setores.isEmpty()) {
            if (erros.isEmpty()) {
                erros.add("Nenhum setor censitário encontrado no arquivo.");
            }
            return new ImportarCensoResponse(
                    false,
                    "O arquivo " + nome + " não foi importado. Corrija os erros apontados; a camada atual não foi alterada.",
                    erros,
                    null
            );
        }

        Usuario autor = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado.", "Faça login novamente."));

        importacaoRepository.findFirstByAtivaTrue().ifPresent(atual -> {
            atual.definirAtiva(false);
            importacaoRepository.save(atual);
        });

        ImportacaoCenso nova = new ImportacaoCenso(nome, formato, autor, setores.size(), true);
        setores.forEach(nova::adicionarSetor);
        nova = importacaoRepository.save(nova);

        auditoriaService.registrar(usuarioId, TipoEventoAuditoria.IMPORTACAO_CENSO,
                TipoObjetoAuditoria.CENSO, nova.getId(),
                "Importação do Censo " + nome + " com " + setores.size() + " setores.");

        return new ImportarCensoResponse(true, "Importação concluída com " + setores.size() + " setores.", List.of(), toDto(nova));
    }

    @Transactional
    public ImportacaoCensoDto voltarParaImportacao(Long importacaoId, Long usuarioId) {
        ImportacaoCenso alvo = importacaoRepository.findById(importacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Importação não encontrada.", "Escolha uma importação da lista."));

        importacaoRepository.findFirstByAtivaTrue().ifPresent(atual -> {
            atual.definirAtiva(false);
            importacaoRepository.save(atual);
        });

        alvo.definirAtiva(true);
        importacaoRepository.save(alvo);

        auditoriaService.registrar(usuarioId, TipoEventoAuditoria.ROLLBACK_CENSO,
                TipoObjetoAuditoria.CENSO, alvo.getId(),
                "Retorno da camada do Censo para a importação " + alvo.getNomeArquivo());

        return toDto(alvo);
    }

    /**
     * Formato CSV esperado (separado por ; ou ,): codigo_setor;bairro;pct_agua;pct_esgoto;poligono_json
     */
    private void parseCsv(String conteudo, List<SetorCensitario> setores, List<String> erros) {
        String[] linhas = conteudo.split("\\r?\\n");
        Set<String> codigosVistos = new HashSet<>();
        for (int i = 0; i < linhas.length; i++) {
            String linha = linhas[i].trim();
            if (linha.isEmpty() || (i == 0 && linha.toLowerCase().contains("codigo"))) {
                continue;
            }
            int numLinha = i + 1;
            String[] cols = linha.split(";");
            if (cols.length < 4) {
                cols = linha.split(",");
            }
            if (cols.length < 4) {
                erros.add("Linha " + numLinha + ": formato inválido. Esperado: codigo_setor;bairro;pct_agua;pct_esgoto;poligono");
                continue;
            }
            String codigo = cols[0].trim();
            String bairro = cols[1].trim();
            String aguaStr = cols[2].trim().replace("%", "").replace(",", ".");
            String esgotoStr = cols[3].trim().replace("%", "").replace(",", ".");
            // O polígono pode conter vírgulas: no CSV separado por ";" ele é a 5ª coluna inteira;
            // no separado por "," é tudo o que vem depois da 4ª coluna.
            String poligono = null;
            if (cols.length >= 5) {
                String sep = linha.contains(";") ? ";" : ",";
                String[] partes = linha.split(sep, 5);
                poligono = partes.length == 5 ? partes[4].trim() : null;
            }

            validarECriarSetor(numLinha, codigo, bairro, aguaStr, esgotoStr, poligono, codigosVistos, setores, erros);
        }
    }

    private void parseGeoJson(String conteudo, List<SetorCensitario> setores, List<String> erros) throws Exception {
        JsonNode root = objectMapper.readTree(conteudo);
        JsonNode features = root.path("features");
        if (!features.isArray()) {
            erros.add("GeoJSON inválido: propriedade 'features' ausente.");
            return;
        }
        Set<String> codigosVistos = new HashSet<>();
        int idx = 0;
        for (JsonNode f : features) {
            idx++;
            JsonNode props = f.path("properties");
            String codigo = props.path("codigo_setor").asText(props.path("CD_SETOR").asText(""));
            String bairro = props.path("bairro").asText("Maceió");
            String aguaStr = props.path("pct_agua").asText(props.path("agua").asText("0"));
            String esgotoStr = props.path("pct_esgoto").asText(props.path("esgoto").asText("0"));
            String poligono;
            try {
                // GeoJSON vem em [lon, lat]; o sistema guarda e consulta em [lat, lon].
                poligono = GeoUtils.converterAnelGeoJson(f.path("geometry"));
            } catch (IllegalArgumentException e) {
                erros.add("Setor " + idx + ": " + e.getMessage() + ".");
                continue;
            }
            validarECriarSetor(idx, codigo, bairro, aguaStr, esgotoStr, poligono, codigosVistos, setores, erros);
        }
    }

    private void validarECriarSetor(int linha, String codigo, String bairro, String aguaStr, String esgotoStr,
                                    String poligono, Set<String> codigosVistos,
                                    List<SetorCensitario> setores, List<String> erros) {
        if (!codigo.matches("^\\d{15}$")) {
            erros.add("Linha " + linha + ": o código '" + codigo + "' tem " + codigo.length() + " dígitos; o código do setor tem 15.");
            return;
        }
        if (!codigosVistos.add(codigo)) {
            erros.add("Linha " + linha + ": código de setor duplicado (" + codigo + ").");
            return;
        }
        if (poligono == null || poligono.isBlank()) {
            erros.add("Linha " + linha + ": polígono do setor ausente. Informe a 5ª coluna como [[lat,lon],...].");
            return;
        }
        try {
            GeoUtils.lerPoligono(poligono);
        } catch (IllegalArgumentException e) {
            erros.add("Linha " + linha + ": polígono inválido (" + e.getMessage() + ").");
            return;
        }
        try {
            BigDecimal agua = new BigDecimal(aguaStr);
            BigDecimal esgoto = new BigDecimal(esgotoStr);
            if (agua.doubleValue() < 0 || agua.doubleValue() > 100) {
                erros.add("Linha " + linha + ": água com " + agua + "%; a proporção vai de 0 a 100.");
                return;
            }
            if (esgoto.doubleValue() < 0 || esgoto.doubleValue() > 100) {
                erros.add("Linha " + linha + ": esgoto com " + esgoto + "%; a proporção vai de 0 a 100.");
                return;
            }
            setores.add(new SetorCensitario(codigo, bairro, poligono, agua, esgoto));
        } catch (NumberFormatException e) {
            erros.add("Linha " + linha + ": valores numéricos inválidos para água ou esgoto.");
        }
    }

    private ImportacaoCensoDto toDto(ImportacaoCenso i) {
        return new ImportacaoCensoDto(
                i.getId(),
                i.getNomeArquivo(),
                i.getFormato(),
                i.getDataImportacao(),
                i.getImportadoPor().getNomeCompleto(),
                i.getQuantidadeSetores(),
                i.isAtiva()
        );
    }
}
