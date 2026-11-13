package br.edu.cesmac.saneamapa.domain.exception;

import org.springframework.http.HttpStatus;

public class RegraNegocioException extends SaneaMapaException {

    public RegraNegocioException(String mensagem, String orientacao) {
        super(mensagem, orientacao);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
