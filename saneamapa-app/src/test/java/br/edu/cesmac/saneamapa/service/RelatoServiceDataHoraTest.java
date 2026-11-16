package br.edu.cesmac.saneamapa.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF001/RS001: a data/hora informada pelo aparelho só vale dentro de uma janela plausível.
 */
class RelatoServiceDataHoraTest {

    private static boolean pertoDeAgora(OffsetDateTime valor) {
        return Duration.between(valor, OffsetDateTime.now()).abs().toSeconds() < 5;
    }

    @Test
    void aceitaHoraDoAparelhoDeRelatoGuardadoSemConexao() {
        OffsetDateTime ontem = OffsetDateTime.now().minusDays(1);
        assertEquals(ontem, RelatoService.dataHoraConfiavel(ontem));
    }

    @Test
    void usaHoraDoServidorQuandoDataEstaForaDaJanela() {
        assertTrue(pertoDeAgora(RelatoService.dataHoraConfiavel(null)));
        assertTrue(pertoDeAgora(RelatoService.dataHoraConfiavel(OffsetDateTime.now().minusDays(30))));
        assertTrue(pertoDeAgora(RelatoService.dataHoraConfiavel(OffsetDateTime.now().plusHours(2))));
    }
}
