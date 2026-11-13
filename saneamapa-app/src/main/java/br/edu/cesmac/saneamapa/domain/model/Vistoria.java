package br.edu.cesmac.saneamapa.domain.model;

import br.edu.cesmac.saneamapa.domain.enums.OrigemEvidencia;
import br.edu.cesmac.saneamapa.domain.enums.PerfilUsuario;
import br.edu.cesmac.saneamapa.domain.enums.ResultadoVistoria;
import br.edu.cesmac.saneamapa.domain.enums.TipoOcorrencia;
import br.edu.cesmac.saneamapa.domain.exception.AcessoNegadoException;
import br.edu.cesmac.saneamapa.domain.exception.DistanciaVistoriaExcedidaException;
import br.edu.cesmac.saneamapa.domain.exception.RegraNegocioException;
import br.edu.cesmac.saneamapa.domain.service.GeoUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Vistoria em campo realizada por um Fiscal (RF007 / RS008).
 * Encapsula a validação de raio <= 100m, captura exclusiva pela câmera e evidência obrigatória quando Confirmado.
 */
@Entity
@Table(name = "vistoria")
public class Vistoria {

    public static final double RAIO_MAXIMO_METROS = 100.0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ponto_id", nullable = false)
    private Ponto ponto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fiscal_id", nullable = false)
    private Usuario fiscal;

    @Column(name = "data_hora_vistoria", nullable = false)
    private OffsetDateTime dataHoraVistoria = OffsetDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultadoVistoria resultado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ocorrencia_validado", nullable = false, length = 30)
    private TipoOcorrencia tipoOcorrenciaValidado;

    @Column(name = "latitude_ponto", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitudePonto;

    @Column(name = "longitude_ponto", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitudePonto;

    @Column(name = "latitude_fiscal", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitudeFiscal;

    @Column(name = "longitude_fiscal", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitudeFiscal;

    @Column(name = "distancia_fiscal_metros", nullable = false, precision = 8, scale = 2)
    private BigDecimal distanciaFiscalMetros;

    @Column(length = 1000)
    private String observacoes;

    @Column(name = "evidencia_camera_caminhos", columnDefinition = "TEXT")
    private String evidenciaCameraCaminhos;

    protected Vistoria() {
    }

    public Vistoria(Ponto ponto, Usuario fiscal, ResultadoVistoria resultado, TipoOcorrencia tipoValidado,
                    BigDecimal novaLatPonto, BigDecimal novaLonPonto,
                    BigDecimal latitudeFiscal, BigDecimal longitudeFiscal,
                    String observacoes, List<String> midiasCamera, List<OrigemEvidencia> origensMidia) {

        if (fiscal.getPerfil() != PerfilUsuario.FISCAL || !fiscal.isAtivo()) {
            throw new AcessoNegadoException(
                "Apenas fiscais ativos podem registrar vistorias.",
                "Entre com uma conta de perfil Fiscal ativa."
            );
        }
        if (ponto.getFiscalAtribuido() == null || !mesmoFiscal(ponto.getFiscalAtribuido(), fiscal)) {
            throw new AcessoNegocioHelper().negarPontoNaoAtribuido();
        }
        if (origensMidia != null && origensMidia.stream().anyMatch(o -> o != OrigemEvidencia.CAMERA)) {
            throw new RegraNegocioException(
                "Fotos e vídeos da galeria não são aceitos na vistoria (RS008).",
                "Capture as evidências diretamente pela câmera do aparelho no local."
            );
        }
        if (resultado == ResultadoVistoria.CONFIRMADO && (midiasCamera == null || midiasCamera.isEmpty())) {
            throw new RegraNegocioException(
                "É obrigatório anexar ao menos uma foto ou vídeo pela câmera para confirmar o ponto.",
                "Tire uma foto ou grave um vídeo do problema antes de salvar."
            );
        }

        BigDecimal latAlvo = novaLatPonto != null ? novaLatPonto : ponto.getLatitude();
        BigDecimal lonAlvo = novaLonPonto != null ? novaLonPonto : ponto.getLongitude();
        double distancia = GeoUtils.calcularDistanciaMetros(
                latAlvo.doubleValue(), lonAlvo.doubleValue(),
                latitudeFiscal.doubleValue(), longitudeFiscal.doubleValue()
        );
        if (distancia > RAIO_MAXIMO_METROS) {
            throw new DistanciaVistoriaExcedidaException(distancia);
        }

        this.ponto = ponto;
        this.fiscal = fiscal;
        this.dataHoraVistoria = OffsetDateTime.now();
        this.resultado = resultado;
        this.tipoOcorrenciaValidado = tipoValidado != null ? tipoValidado : ponto.getTipoOcorrencia();
        this.latitudePonto = latAlvo;
        this.longitudePonto = lonAlvo;
        this.latitudeFiscal = latitudeFiscal;
        this.longitudeFiscal = longitudeFiscal;
        this.distanciaFiscalMetros = BigDecimal.valueOf(distancia).setScale(2, RoundingMode.HALF_UP);
        this.observacoes = observacoes != null ? observacoes.trim() : null;
        this.evidenciaCameraCaminhos = (midiasCamera != null && !midiasCamera.isEmpty())
                ? String.join(";", midiasCamera) : null;
    }

    private static boolean mesmoFiscal(Usuario atribuido, Usuario atual) {
        if (atribuido.getId() != null && atual.getId() != null) {
            return atribuido.getId().equals(atual.getId());
        }
        return atribuido == atual;
    }

    private static class AcessoNegocioHelper {
        AcessoNegadoException negarPontoNaoAtribuido() {
            return new AcessoNegadoException(
                "Este ponto não está atribuído a você.",
                "O fiscal só pode vistoriar os pontos atribuídos pela Prefeitura."
            );
        }
    }

    public Long getId() { return id; }
    public Ponto getPonto() { return ponto; }
    public Usuario getFiscal() { return fiscal; }
    public OffsetDateTime getDataHoraVistoria() { return dataHoraVistoria; }
    public ResultadoVistoria getResultado() { return resultado; }
    public TipoOcorrencia getTipoOcorrenciaValidado() { return tipoOcorrenciaValidado; }
    public BigDecimal getLatitudePonto() { return latitudePonto; }
    public BigDecimal getLongitudePonto() { return longitudePonto; }
    public BigDecimal getLatitudeFiscal() { return latitudeFiscal; }
    public BigDecimal getLongitudeFiscal() { return longitudeFiscal; }
    public BigDecimal getDistanciaFiscalMetros() { return distanciaFiscalMetros; }
    public String getObservacoes() { return observacoes; }
    public String getEvidenciaCameraCaminhos() { return evidenciaCameraCaminhos; }
}
