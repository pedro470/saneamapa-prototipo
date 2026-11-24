package br.edu.cesmac.saneamapa.dto;

import br.edu.cesmac.saneamapa.domain.enums.NivelUrgencia;
import br.edu.cesmac.saneamapa.domain.enums.StatusPonto;
import br.edu.cesmac.saneamapa.domain.enums.TipoOcorrencia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class PainelDtos {

    public record PontoResumoDto(
            Long id,
            String codigoExibicao,
            TipoOcorrencia tipoOcorrencia,
            String tipoRotulo,
            StatusPonto status,
            String statusRotulo,
            NivelUrgencia nivelUrgencia,
            String nivelRotulo,
            int nivelMedidor,
            String motivoUrgencia,
            String trechoRua,
            String bairro,
            int quantidadeRelatos,
            long diasEspera,
            BigDecimal pontuacao,
            Integer posicaoFila,
            Long fiscalId,
            String fiscalNome,
            boolean reincidencia,
            boolean posicaoManual,
            LocalDate dataPrimeiroRelato,
            LocalDate dataUltimoRelato,
            BigDecimal latitudeAproximada,
            BigDecimal longitudeAproximada
    ) {}

    public record EvidenciaResumoDto(
            Long id,
            String tipo,
            String origem,
            boolean divergente,
            String descricaoTratamento,
            String transcricao,
            String regiaoDesfoqueManual,
            Integer duracaoSegundos,
            String arquivoUsoCaminho
    ) {}

    public record RelatoVinculadoDto(
            Long id,
            String protocolo,
            OffsetDateTime dataHora,
            String descricao,
            String logradouro,
            String bairro,
            String pontoReferencia
    ) {}

    public record HistoricoEventoDto(
            OffsetDateTime dataHora,
            String tipoEvento,
            String atorNome,
            String detalhes
    ) {}

    public record PontoDetalheDto(
            PontoResumoDto resumo,
            String censoResumo,
            List<EvidenciaResumoDto> evidencias,
            List<RelatoVinculadoDto> relatos,
            List<HistoricoEventoDto> historico
    ) {}

    public record CoordenadaExataDto(
            BigDecimal latitude,
            BigDecimal longitude,
            String avisoAuditoria
    ) {}

    public record ContatoIdentificadoDto(
            Long relatoId,
            String nomeMorador,
            String telefoneMorador
    ) {}

    public record AcessoContatosResponse(
            List<ContatoIdentificadoDto> contatos,
            String avisoAuditoria
    ) {}

    public record AcessoAudioResponse(
            Long evidenciaId,
            String arquivoUsoCaminho,
            String transcricao,
            String avisoAuditoria
    ) {}

    public record AtribuirFiscalRequest(
            Long fiscalId
    ) {}

    public record ItemIndicadorDto(
            String chave,
            String rotulo,
            long quantidade
    ) {}

    public record IndicadoresResponse(
            long totalPontos,
            List<ItemIndicadorDto> porStatus,
            List<ItemIndicadorDto> porTipo,
            List<ItemIndicadorDto> porUrgencia,
            List<ItemIndicadorDto> porBairro
    ) {}
}
