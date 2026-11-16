package br.edu.cesmac.saneamapa.domain.model;

import br.edu.cesmac.saneamapa.domain.enums.OrigemEvidencia;
import br.edu.cesmac.saneamapa.domain.enums.TipoEvidencia;
import br.edu.cesmac.saneamapa.domain.exception.RegraNegocioException;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@DiscriminatorValue("FOTO")
public class EvidenciaFoto extends Evidencia {

    protected EvidenciaFoto() {
    }

    public EvidenciaFoto(OrigemEvidencia origem, String arquivoUsoCaminho, OffsetDateTime dataHoraCaptura,
                         BigDecimal latitudeCaptura, BigDecimal longitudeCaptura) {
        super(origem, arquivoUsoCaminho, dataHoraCaptura, latitudeCaptura, longitudeCaptura, null);
    }

    @Override
    public TipoEvidencia getTipoEvidencia() {
        return TipoEvidencia.FOTO;
    }

    @Override
    public void validarRegrasCaptura() {
        if (getOrigem() == OrigemEvidencia.MICROFONE) {
            throw new RegraNegocioException(
                "Origem inválida para fotografia.",
                "Envie a foto pela câmera ou pela galeria do aparelho."
            );
        }
    }

    @Override
    public void aplicarTratamentoInicial() {
        // RF003 nota 2: desfoque automático de rostos e placas antes da gravação definitiva
        this.desfoqueAplicado = true;
    }

    /**
     * RF003 nota 3 / RS006: fiscal marca nova região de desfoque; versão anterior é descartada.
     */
    public void aplicarNovoDesfoque(String regiao) {
        if (regiao == null || regiao.isBlank()) {
            throw new RegraNegocioException(
                "Região de desfoque não informada.",
                "Selecione na imagem a área onde deseja aplicar o novo desfoque."
            );
        }
        this.regiaoDesfoqueManual = regiao.trim();
        this.desfoqueAplicado = true;
    }

    @Override
    public String descreverTratamento() {
        // Protótipo: o desfoque é registrado mas não processado; o arquivo servido é o original.
        return regiaoDesfoqueManual != null
                ? "Desfoque de rosto/placa e novo desfoque (" + regiaoDesfoqueManual + ") simulados no protótipo: a imagem exibida é a original"
                : "Desfoque de rosto/placa simulado no protótipo: a imagem exibida é a original";
    }
}
