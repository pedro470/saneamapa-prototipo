package br.edu.cesmac.saneamapa.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Classe abstrata base da hierarquia de exceções de domínio do SaneaMapa.
 * Demonstra Abstração e Herança para padronizar mensagens claras (RNF03: motivo + o que fazer).
 */
public abstract class SaneaMapaException extends RuntimeException {

    private final String orientacao;

    protected SaneaMapaException(String mensagem, String orientacao) {
        super(mensagem);
        this.orientacao = orientacao;
    }

    public String getOrientacao() {
        return orientacao;
    }

    public abstract HttpStatus getHttpStatus();
}
