package br.edu.cesmac.saneamapa.domain.service;

import br.edu.cesmac.saneamapa.domain.model.Ponto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** RF005, nota 1: pesos da fila. */
class CalculadoraPontuacaoPadraoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 23);

    private final CalculadoraPontuacaoPadrao calc = new CalculadoraPontuacaoPadrao(6.0, 10, 1.5, 15.0, 2.0, 10);

    private static Ponto ponto(int relatos, int diasEspera, int pesoUrgencia) {
        Ponto p = mock(Ponto.class);
        when(p.getQuantidadeRelatos()).thenReturn(relatos);
        when(p.getDataPrimeiroRelato()).thenReturn(HOJE.minusDays(diasEspera));
        when(p.getPesoUrgencia()).thenReturn((short) pesoUrgencia);
        return p;
    }

    @Test
    void pontoCriticoNovoComUmRelato() {
        assertEquals(new BigDecimal("51.00"), calc.calcular(ponto(1, 0, 3), 0, HOJE));
    }

    @Test
    void pontoNormalAlcancaOCriticoEmTrintaDias() {
        assertEquals(new BigDecimal("51.00"), calc.calcular(ponto(1, 30, 0), 0, HOJE));
    }

    @Test
    void relatosContamNoMaximoDez() {
        assertEquals(calc.calcular(ponto(10, 0, 0), 0, HOJE), calc.calcular(ponto(50, 0, 0), 0, HOJE));
        assertEquals(new BigDecimal("60.00"), calc.calcular(ponto(50, 0, 0), 0, HOJE));
    }

    @Test
    void pontosNoBairroContamNoMaximoDez() {
        assertEquals(new BigDecimal("26.00"), calc.calcular(ponto(1, 0, 0), 30, HOJE));
    }

    @Test
    void esperaNaoTemLimite() {
        assertEquals(new BigDecimal("96.00"), calc.calcular(ponto(1, 60, 0), 0, HOJE));
    }
}
