package br.edu.cesmac.saneamapa.domain.exception;

import org.springframework.http.HttpStatus;

public class DistanciaVistoriaExcedidaException extends SaneaMapaException {

    private final double distanciaMetros;

    public DistanciaVistoriaExcedidaException(double distanciaMetros) {
        super(
            String.format("Você está a %.0f m do ponto. A vistoria só é aceita a até 100 m do local.", distanciaMetros),
            "Aproxime-se do ponto denunciado e tente salvar novamente; os dados preenchidos não serão perdidos."
        );
        this.distanciaMetros = distanciaMetros;
    }

    public double getDistanciaMetros() {
        return distanciaMetros;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
