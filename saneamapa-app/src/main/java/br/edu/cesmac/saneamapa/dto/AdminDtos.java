package br.edu.cesmac.saneamapa.dto;

import br.edu.cesmac.saneamapa.domain.enums.CategoriaCamada;
import br.edu.cesmac.saneamapa.domain.enums.PerfilUsuario;
import br.edu.cesmac.saneamapa.domain.enums.TipoEventoAuditoria;
import br.edu.cesmac.saneamapa.domain.enums.TipoGeometria;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class AdminDtos {

    public record UsuarioDto(
            Long id,
            String cpfMascarado,
            String nomeCompleto,
            String email,
            String celular,
            PerfilUsuario perfil,
            boolean ativo
    ) {}

    public record CriarUsuarioRequest(
            @NotBlank String cpf,
            @NotBlank @Size(max = 120) String nomeCompleto,
            @NotBlank @Size(max = 200) String email,
            @NotBlank @Size(max = 15) String celular,
            @NotBlank String senha,
            @NotNull PerfilUsuario perfil,
            boolean ativo
    ) {}

    public record AtualizarUsuarioRequest(
            @NotBlank @Size(max = 120) String nomeCompleto,
            @NotBlank @Size(max = 200) String email,
            @NotBlank @Size(max = 15) String celular,
            @NotNull PerfilUsuario perfil,
            boolean ativo
    ) {}

    public record AlteracaoUsuarioDto(
            OffsetDateTime dataHora,
            String autorNome,
            String usuarioAlvoNome,
            String campoAlterado,
            String valorAnterior,
            String valorNovo
    ) {}

    public record CamadaDto(
            Long id,
            String nome,
            CategoriaCamada categoria,
            String categoriaRotulo,
            TipoGeometria tipoGeometria,
            String geometriaDescricao,
            BigDecimal centroLatitude,
            BigDecimal centroLongitude,
            Integer raioMetros,
            String poligonoCoordenadas,
            int pesoUrgencia,
            String fonteDado,
            LocalDate dataAtualizacao,
            boolean ativa,
            long pontosAfetados
    ) {}

    public record CriarCamadaRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotNull CategoriaCamada categoria,
            @NotNull TipoGeometria tipoGeometria,
            BigDecimal centroLatitude,
            BigDecimal centroLongitude,
            Integer raioMetros,
            String poligonoCoordenadas,
            @Min(1) @Max(3) int pesoUrgencia,
            @NotBlank @Size(max = 200) String fonteDado,
            LocalDate dataAtualizacao,
            boolean ativa
    ) {}

    public record ImportacaoCensoDto(
            Long id,
            String nomeArquivo,
            String formato,
            OffsetDateTime dataImportacao,
            String importadoPorNome,
            int quantidadeSetores,
            boolean ativa
    ) {}

    public record SetorCensitarioDto(
            String codigoSetor,
            String bairro,
            String poligonoCoordenadas,
            BigDecimal pctRedeAgua,
            BigDecimal pctRedeEsgoto
    ) {}

    public record ImportarCensoResponse(
            boolean sucesso,
            String mensagem,
            List<String> erros,
            ImportacaoCensoDto importacao
    ) {}

    public record LogAuditoriaDto(
            Long id,
            OffsetDateTime dataHora,
            String atorNome,
            TipoEventoAuditoria tipoEvento,
            String eventoRotulo,
            String tipoObjeto,
            Long objetoId,
            String detalhes
    ) {}
}
