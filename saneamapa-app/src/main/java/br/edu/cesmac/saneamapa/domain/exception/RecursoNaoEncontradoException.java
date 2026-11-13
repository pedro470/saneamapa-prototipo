package br.edu.cesmac.saneamapa.domain.exception;

import org.springframework.http.HttpStatus;

public class RecursoNaoEncontradoException extends SaneaMapaException {

    public RecursoNaoEncontradoException(String mensagem, String orientacao) {
        super(mensagem, orientacao);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
