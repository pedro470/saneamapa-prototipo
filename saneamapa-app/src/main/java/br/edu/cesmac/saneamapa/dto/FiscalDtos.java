package br.edu.cesmac.saneamapa.dto;

import br.edu.cesmac.saneamapa.domain.enums.OrigemEvidencia;
import br.edu.cesmac.saneamapa.domain.enums.ResultadoVistoria;
import br.edu.cesmac.saneamapa.domain.enums.TipoOcorrencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class FiscalDtos {

    public record RegistrarVistoriaRequest(
            @NotNull(message = "Informe o resultado da vistoria.") ResultadoVistoria resultado,
            TipoOcorrencia tipoOcorrenciaValidado,
            BigDecimal novaLatitudePonto,
            BigDecimal novaLongitudePonto,
            @NotNull(message = "Latitude do fiscal é obrigatória.") BigDecimal latitudeFiscal,
            @NotNull(message = "Longitude do fiscal é obrigatória.") BigDecimal longitudeFiscal,
            @Size(max = 1000) String observacoes,
            List<String> midiasCamera,
            List<OrigemEvidencia> origensMidia
    ) {}

    public record VistoriaRegistradaResponse(
            Long vistoriaId,
            String codigoPonto,
            String novoStatus,
            BigDecimal distanciaMetros,
            OffsetDateTime dataHora
    ) {}

    public record NovoDesfoqueRequest(
            @NotBlank(message = "Informe a região selecionada para o novo desfoque.") @Size(max = 200) String regiao
    ) {}

    public record CorrigirTranscricaoRequest(
            @NotBlank(message = "Informe o texto da transcrição.") @Size(max = 2000) String transcricao
    ) {}
}
