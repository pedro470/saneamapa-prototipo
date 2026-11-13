package br.edu.cesmac.saneamapa.domain.exception;

import org.springframework.http.HttpStatus;

public class ConflitoDadosException extends SaneaMapaException {

    public ConflitoDadosException(String mensagem, String orientacao) {
        super(mensagem, orientacao);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
