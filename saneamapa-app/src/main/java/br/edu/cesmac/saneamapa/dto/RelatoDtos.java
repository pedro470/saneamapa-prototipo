package br.edu.cesmac.saneamapa.dto;

import br.edu.cesmac.saneamapa.domain.enums.OrigemEvidencia;
import br.edu.cesmac.saneamapa.domain.enums.TipoEvidencia;
import br.edu.cesmac.saneamapa.domain.enums.TipoOcorrencia;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class RelatoDtos {

    public record EvidenciaInput(
            @NotNull(message = "Tipo de evidência é obrigatório.") TipoEvidencia tipo,
            @NotNull(message = "Origem da evidência é obrigatória.") OrigemEvidencia origem,
            @NotBlank(message = "Caminho ou referência do arquivo é obrigatório.") String arquivoCaminho,
            OffsetDateTime dataHoraCaptura,
            BigDecimal latitudeCaptura,
            BigDecimal longitudeCaptura,
            Integer duracaoSegundos
    ) {}

    public record CriarRelatoRequest(
            String protocolo,
            OffsetDateTime dataHoraRegistro,
            @NotNull(message = "Escolha o tipo de ocorrência.") TipoOcorrencia tipoOcorrencia,
            @Size(max = 300, message = "Descrição deve ter no máximo 300 caracteres.") String descricao,
            boolean identificado,
            @Size(max = 120) String nomeMorador,
            @Size(max = 15) String telefoneMorador,
            @NotNull(message = "Latitude é obrigatória.") BigDecimal latitude,
            @NotNull(message = "Longitude é obrigatória.") BigDecimal longitude,
            Integer precisaoMetros,
            @Size(max = 150) String logradouro,
            @Size(max = 80) String bairro,
            @Size(max = 100) String pontoReferencia,
            boolean posicaoManual,
            @NotEmpty(message = "Inclua ao menos uma foto, vídeo ou áudio.") @Valid List<EvidenciaInput> evidencias
    ) {}

    public record RelatoCriadoResponse(
            String protocolo,
            OffsetDateTime dataHoraRegistro,
            String codigoPonto,
            boolean agrupadoEmPontoExistente,
            boolean prioridadeReduzida,
            String mensagem
    ) {}
}
