package br.edu.cesmac.saneamapa.domain.model;

import br.edu.cesmac.saneamapa.domain.enums.OrigemEvidencia;
import br.edu.cesmac.saneamapa.domain.enums.TipoEvidencia;
import br.edu.cesmac.saneamapa.domain.exception.RegraNegocioException;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@DiscriminatorValue("AUDIO")
public class EvidenciaAudio extends Evidencia {

    public static final int DURACAO_MAXIMA_SEGUNDOS = 60;

    protected EvidenciaAudio() {
    }

    public EvidenciaAudio(OrigemEvidencia origem, String arquivoUsoCaminho, OffsetDateTime dataHoraCaptura,
                          BigDecimal latitudeCaptura, BigDecimal longitudeCaptura, Integer duracaoSegundos,
                          String transcricaoInicial) {
        super(origem, arquivoUsoCaminho, dataHoraCaptura, latitudeCaptura, longitudeCaptura, duracaoSegundos);
        this.transcricao = transcricaoInicial;
    }

    @Override
    public TipoEvidencia getTipoEvidencia() {
        return TipoEvidencia.AUDIO;
    }

    @Override
    public void validarRegrasCaptura() {
        if (getOrigem() == OrigemEvidencia.CAMERA) {
            throw new RegraNegocioException("Origem inválida para áudio.", "Grave pelo microfone ou escolha um áudio da galeria.");
        }
        if (duracaoSegundos != null && (duracaoSegundos <= 0 || duracaoSegundos > DURACAO_MAXIMA_SEGUNDOS)) {
            throw new RegraNegocioException(
                String.format("Áudio não aceito: a gravação tem %d segundos e o limite é %d segundos.", duracaoSegundos, DURACAO_MAXIMA_SEGUNDOS),
                "Grave um áudio mais curto, de até 1 minuto."
            );
        }
    }

    @Override
    public void aplicarTratamentoInicial() {
        // RF003 nota 5 / RS007: áudio fica restrito à equipe interna e transcrito sem enviar dados pessoais
        this.desfoqueAplicado = false;
        this.somRemovido = false;
    }

    public void definirTranscricao(String texto) {
        if (texto == null) {
            this.transcricao = null;
            return;
        }
        String limpo = texto.trim();
        this.transcricao = limpo.length() > 2000 ? limpo.substring(0, 2000) : limpo;
    }

    @Override
    public String descreverTratamento() {
        return "Áudio restrito à equipe interna com transcrição automática por IA";
    }
}
