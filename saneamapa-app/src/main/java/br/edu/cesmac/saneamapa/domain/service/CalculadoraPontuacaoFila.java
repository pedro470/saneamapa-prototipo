package br.edu.cesmac.saneamapa.domain.service;

import br.edu.cesmac.saneamapa.domain.model.Ponto;

import java.math.BigDecimal;

/**
 * Estratégia abstrata para cálculo da pontuação da fila de vistoria (RF005 nota 1).
 */
public interface CalculadoraPontuacaoFila {

    BigDecimal calcular(Ponto ponto, long pontosNaFilaNoBairro, boolean possuiRelatoIdentificado);
}
