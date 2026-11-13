package br.edu.cesmac.saneamapa.domain.exception;

import org.springframework.http.HttpStatus;

public class AcessoNegadoException extends SaneaMapaException {

    public AcessoNegadoException(String mensagem, String orientacao) {
        super(mensagem, orientacao);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
