package br.edu.cesmac.saneamapa.domain.service;

import br.edu.cesmac.saneamapa.domain.model.Ponto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class CalculadoraPontuacaoPadrao implements CalculadoraPontuacaoFila {

    private final double pesoRelatos;
    private final double pesoDiasEspera;
    private final double pesoUrgencia;
    private final double pesoDensidadeBairro;
    private final double bonusIdentificado;

    public CalculadoraPontuacaoPadrao(
            @Value("${saneamapa.fila.peso-relatos:6.0}") double pesoRelatos,
            @Value("${saneamapa.fila.peso-dias-espera:1.5}") double pesoDiasEspera,
            @Value("${saneamapa.fila.peso-urgencia:15.0}") double pesoUrgencia,
            @Value("${saneamapa.fila.peso-densidade-bairro:2.0}") double pesoDensidadeBairro,
            @Value("${saneamapa.fila.bonus-identificado:10.0}") double bonusIdentificado) {
        this.pesoRelatos = pesoRelatos;
        this.pesoDiasEspera = pesoDiasEspera;
        this.pesoUrgencia = pesoUrgencia;
        this.pesoDensidadeBairro = pesoDensidadeBairro;
        this.bonusIdentificado = bonusIdentificado;
    }

    @Override
    public BigDecimal calcular(Ponto ponto, long pontosNaFilaNoBairro, boolean possuiRelatoIdentificado) {
        long diasEspera = Math.max(0, ChronoUnit.DAYS.between(ponto.getDataPrimeiroRelato(), LocalDate.now()));
        double soma = (ponto.getQuantidadeRelatos() * pesoRelatos)
                + (diasEspera * pesoDiasEspera)
                + (ponto.getPesoUrgencia() * pesoUrgencia)
                + (pontosNaFilaNoBairro * pesoDensidadeBairro)
                + (possuiRelatoIdentificado ? bonusIdentificado : 0.0);

        return BigDecimal.valueOf(Math.max(0.0, soma)).setScale(2, RoundingMode.HALF_UP);
    }
}
