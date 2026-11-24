package br.edu.cesmac.saneamapa.domain.service;

import br.edu.cesmac.saneamapa.domain.model.Ponto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Pontuação da fila de vistoria (RF005, nota 1; valores iniciais do TBD-17):
 * 6 pontos por relato, contando no máximo 10; 1,5 ponto por dia de espera, sem limite;
 * 15 pontos por unidade do peso de urgência (0 a 3); 2 pontos por ponto na fila no mesmo
 * bairro, contando no máximo 10. Os limites impedem que uma enxurrada de relatos passe à
 * frente dos pontos urgentes (RS001); a espera sem limite impede que um ponto fique parado (RS009).
 */
@Component
public class CalculadoraPontuacaoPadrao implements CalculadoraPontuacaoFila {

    private final double pesoRelatos;
    private final long limiteRelatos;
    private final double pesoDiasEspera;
    private final double pesoUrgencia;
    private final double pesoDensidadeBairro;
    private final long limiteDensidadeBairro;

    public CalculadoraPontuacaoPadrao(
            @Value("${saneamapa.fila.peso-relatos:6.0}") double pesoRelatos,
            @Value("${saneamapa.fila.limite-relatos:10}") long limiteRelatos,
            @Value("${saneamapa.fila.peso-dias-espera:1.5}") double pesoDiasEspera,
            @Value("${saneamapa.fila.peso-urgencia:15.0}") double pesoUrgencia,
            @Value("${saneamapa.fila.peso-densidade-bairro:2.0}") double pesoDensidadeBairro,
            @Value("${saneamapa.fila.limite-densidade-bairro:10}") long limiteDensidadeBairro) {
        this.pesoRelatos = pesoRelatos;
        this.limiteRelatos = limiteRelatos;
        this.pesoDiasEspera = pesoDiasEspera;
        this.pesoUrgencia = pesoUrgencia;
        this.pesoDensidadeBairro = pesoDensidadeBairro;
        this.limiteDensidadeBairro = limiteDensidadeBairro;
    }

    @Override
    public BigDecimal calcular(Ponto ponto, long pontosNaFilaNoBairro) {
        return calcular(ponto, pontosNaFilaNoBairro, LocalDate.now());
    }

    BigDecimal calcular(Ponto ponto, long pontosNaFilaNoBairro, LocalDate hoje) {
        long diasEspera = Math.max(0, ChronoUnit.DAYS.between(ponto.getDataPrimeiroRelato(), hoje));
        long relatos = Math.min(Math.max(0, ponto.getQuantidadeRelatos()), limiteRelatos);
        long noBairro = Math.min(Math.max(0, pontosNaFilaNoBairro), limiteDensidadeBairro);
        double soma = (relatos * pesoRelatos)
                + (diasEspera * pesoDiasEspera)
                + (ponto.getPesoUrgencia() * pesoUrgencia)
                + (noBairro * pesoDensidadeBairro);

        return BigDecimal.valueOf(soma).setScale(2, RoundingMode.HALF_UP);
    }
}
