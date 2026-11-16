package br.edu.cesmac.saneamapa;

import br.edu.cesmac.saneamapa.domain.enums.*;
import br.edu.cesmac.saneamapa.domain.exception.AcessoNegadoException;
import br.edu.cesmac.saneamapa.domain.exception.DistanciaVistoriaExcedidaException;
import br.edu.cesmac.saneamapa.domain.exception.RegraNegocioException;
import br.edu.cesmac.saneamapa.domain.model.*;
import br.edu.cesmac.saneamapa.domain.service.CpfUtils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DominioPooTest {

    @Test
    void deveValidarPolimorfismoDeEvidenciasETratamentoLgpd() {
        Evidencia videoLongo = new EvidenciaVideo(OrigemEvidencia.CAMERA, "/v.mp4", OffsetDateTime.now(), null, null, 48);
        assertThrows(RegraNegocioException.class, videoLongo::validarRegrasCaptura);

        Evidencia videoOk = new EvidenciaVideo(OrigemEvidencia.CAMERA, "/ok.mp4", OffsetDateTime.now(), null, null, 20);
        videoOk.validarRegrasCaptura();
        videoOk.aplicarTratamentoInicial();
        assertTrue(videoOk.isSomRemovido());
        assertTrue(videoOk.isDesfoqueAplicado());

        Evidencia audioLongo = new EvidenciaAudio(OrigemEvidencia.MICROFONE, "/a.ogg", OffsetDateTime.now(), null, null, 75, null);
        assertThrows(RegraNegocioException.class, audioLongo::validarRegrasCaptura);

        EvidenciaFoto foto = new EvidenciaFoto(OrigemEvidencia.CAMERA, "/f.jpg", OffsetDateTime.now(), null, null);
        foto.aplicarTratamentoInicial();
        foto.aplicarNovoDesfoque("Placa de moto no canto esquerdo");
        assertTrue(foto.descreverTratamento().contains("Placa de moto"));
    }

    @Test
    void deveValidarRegraEpidemiologicaSomentePoligonoEContencaoPolimorfica() {
        assertThrows(RegraNegocioException.class, () -> new CamadaVulnerabilidade(
                "Surto dengue", CategoriaCamada.VULNERABILIDADE_EPIDEMIOLOGICA, TipoGeometria.PONTO_RAIO,
                new BigDecimal("-9.67"), new BigDecimal("-35.74"), 100, null, 3, "Boletim", LocalDate.now(), true
        ));

        CamadaVulnerabilidade camadaPol = new CamadaVulnerabilidade(
                "Área de risco", CategoriaCamada.VULNERABILIDADE_EPIDEMIOLOGICA, TipoGeometria.POLIGONO,
                null, null, null, "[[-9.68,-35.75],[-9.66,-35.75],[-9.66,-35.73],[-9.68,-35.73]]",
                3, "Boletim", LocalDate.now(), true
        );
        assertTrue(camadaPol.obterGeometria().contemPonto(-9.67, -35.74));
        assertFalse(camadaPol.obterGeometria().contemPonto(-9.55, -35.72));
    }

    @Test
    void deveBloquearVistoriaForaDe100MetrosEExigirCamera() {
        Usuario fiscal = new Usuario("456.789.002-77", "Carlos Mendes", "carlos@gov.br", "(82) 99876-1020", "hash", PerfilUsuario.FISCAL, true);
        Ponto ponto = new Ponto("P-9000", TipoOcorrencia.ESGOTO_CEU_ABERTO, new BigDecimal("-9.6712400"), new BigDecimal("-35.7489300"),
                "Rua Teste", "Vergel", LocalDate.now(), false);
        ponto.atribuirFiscal(fiscal);

        assertThrows(DistanciaVistoriaExcedidaException.class, () -> new Vistoria(
                ponto, fiscal, ResultadoVistoria.CONFIRMADO, TipoOcorrencia.ESGOTO_CEU_ABERTO,
                null, null, new BigDecimal("-9.6740000"), new BigDecimal("-35.7489300"),
                "Longe", List.of("/cam.jpg"), List.of(OrigemEvidencia.CAMERA)
        ));

        // RS008: enviar a própria posição como "nova posição do ponto" não zera a distância.
        assertThrows(DistanciaVistoriaExcedidaException.class, () -> new Vistoria(
                ponto, fiscal, ResultadoVistoria.CONFIRMADO, TipoOcorrencia.ESGOTO_CEU_ABERTO,
                new BigDecimal("-9.6740000"), new BigDecimal("-35.7489300"),
                new BigDecimal("-9.6740000"), new BigDecimal("-35.7489300"),
                "Correção forjada", List.of("/cam.jpg"), List.of(OrigemEvidencia.CAMERA)
        ));

        assertThrows(RegraNegocioException.class, () -> new Vistoria(
                ponto, fiscal, ResultadoVistoria.CONFIRMADO, TipoOcorrencia.ESGOTO_CEU_ABERTO,
                null, null, new BigDecimal("-9.6712500"), new BigDecimal("-35.7489300"),
                "Galeria", List.of("/gal.jpg"), List.of(OrigemEvidencia.GALERIA)
        ));
    }

    @Test
    void deveBloquearUsuarioApos5TentativasEValidarCpf() {
        assertTrue(CpfUtils.isCpfValido("218.334.001-00"));
        assertFalse(CpfUtils.isCpfValido("123.456.789-00"));

        Usuario u = new Usuario("218.334.001-00", "Ana Lima", "ana@gov.br", "(82) 99120-4455", "hash", PerfilUsuario.PREFEITURA, true);
        OffsetDateTime agora = OffsetDateTime.now();
        for (int i = 0; i < 5; i++) {
            u.registrarFalhaAutenticacao(agora);
        }
        assertTrue(u.estaBloqueado(agora.plusMinutes(5)));
        assertThrows(AcessoNegadoException.class, () -> u.validarPodeAutenticar(agora.plusMinutes(5)));
    }
}
