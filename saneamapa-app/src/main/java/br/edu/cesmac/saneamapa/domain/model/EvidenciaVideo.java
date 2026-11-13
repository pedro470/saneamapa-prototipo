package br.edu.cesmac.saneamapa.domain.model;

import br.edu.cesmac.saneamapa.domain.enums.OrigemEvidencia;
import br.edu.cesmac.saneamapa.domain.enums.TipoEvidencia;
import br.edu.cesmac.saneamapa.domain.exception.RegraNegocioException;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@DiscriminatorValue("VIDEO")
public class EvidenciaVideo extends Evidencia {

    public static final int DURACAO_MAXIMA_SEGUNDOS = 30;

    protected EvidenciaVideo() {
    }

    public EvidenciaVideo(OrigemEvidencia origem, String arquivoUsoCaminho, OffsetDateTime dataHoraCaptura,
                          BigDecimal latitudeCaptura, BigDecimal longitudeCaptura, Integer duracaoSegundos) {
        super(origem, arquivoUsoCaminho, dataHoraCaptura, latitudeCaptura, longitudeCaptura, duracaoSegundos);
    }

    @Override
    public TipoEvidencia getTipoEvidencia() {
        return TipoEvidencia.VIDEO;
    }

    @Override
    public void validarRegrasCaptura() {
        if (getOrigem() == OrigemEvidencia.MICROFONE) {
            throw new RegraNegocioException("Origem inválida para vídeo.", "Grave pela câmera ou escolha da galeria.");
        }
        if (duracaoSegundos != null && (duracaoSegundos <= 0 || duracaoSegundos > DURACAO_MAXIMA_SEGUNDOS)) {
            throw new RegraNegocioException(
                String.format("Vídeo não aceito: o arquivo tem %d segundos e o limite é %d segundos.", duracaoSegundos, DURACAO_MAXIMA_SEGUNDOS),
                "Grave ou selecione um trecho mais curto, de até 30 segundos, mostrando apenas o problema."
            );
        }
    }

    @Override
    public void aplicarTratamentoInicial() {
        // RF003 nota 2: desfoque aplicado em todos os quadros e faixa de áudio removida
        this.desfoqueAplicado = true;
        this.somRemovido = true;
    }

    @Override
    public String descreverTratamento() {
        return "Vídeo sem som e com desfoque em todos os quadros";
    }
}
