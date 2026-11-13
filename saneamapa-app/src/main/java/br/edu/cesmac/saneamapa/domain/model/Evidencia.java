package br.edu.cesmac.saneamapa.domain.model;

import br.edu.cesmac.saneamapa.domain.enums.OrigemEvidencia;
import br.edu.cesmac.saneamapa.domain.enums.TipoEvidencia;
import br.edu.cesmac.saneamapa.domain.service.GeoUtils;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Classe abstrata polimórfica de Evidência (Abstração, Herança e Polimorfismo).
 * Subclasses especializam as regras de captura (RF001) e tratamento LGPD (RF003).
 */
@Entity
@Table(name = "evidencia")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_evidencia", discriminatorType = DiscriminatorType.STRING)
public abstract class Evidencia extends EntidadeAuditavel {

    private static final long MAX_DIAS_DIVERGENCIA_GALERIA = 7;
    private static final double MAX_METROS_DIVERGENCIA_GALERIA = 500.0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "relato_id", nullable = false)
    private Relato relato;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private OrigemEvidencia origem;

    @Column(name = "evidencia_divergente", nullable = false)
    private boolean evidenciaDivergente = false;

    @Column(name = "arquivo_uso_caminho", nullable = false, length = 300)
    private String arquivoUsoCaminho;

    @Column(name = "data_hora_captura", nullable = false)
    private OffsetDateTime dataHoraCaptura;

    @Column(name = "latitude_captura", precision = 10, scale = 7)
    private BigDecimal latitudeCaptura;

    @Column(name = "longitude_captura", precision = 10, scale = 7)
    private BigDecimal longitudeCaptura;

    @Column(name = "duracao_segundos")
    protected Integer duracaoSegundos;

    @Column(name = "desfoque_aplicado", nullable = false)
    protected boolean desfoqueAplicado = false;

    @Column(name = "regiao_desfoque_manual", length = 200)
    protected String regiaoDesfoqueManual;

    @Column(name = "som_removido", nullable = false)
    protected boolean somRemovido = false;

    @Column(length = 2000)
    protected String transcricao;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();

    protected Evidencia() {
    }

    protected Evidencia(OrigemEvidencia origem, String arquivoUsoCaminho, OffsetDateTime dataHoraCaptura,
                        BigDecimal latitudeCaptura, BigDecimal longitudeCaptura, Integer duracaoSegundos) {
        this.origem = origem;
        this.arquivoUsoCaminho = arquivoUsoCaminho;
        this.dataHoraCaptura = dataHoraCaptura != null ? dataHoraCaptura : OffsetDateTime.now();
        this.latitudeCaptura = latitudeCaptura;
        this.longitudeCaptura = longitudeCaptura;
        this.duracaoSegundos = duracaoSegundos;
    }

    @PreUpdate
    protected void aoAtualizar() {
        this.atualizadoEm = OffsetDateTime.now();
    }

    public abstract TipoEvidencia getTipoEvidencia();

    public abstract void validarRegrasCaptura();

    public abstract void aplicarTratamentoInicial();

    public abstract String descreverTratamento();

    void vincularAoRelato(Relato relato) {
        this.relato = relato;
        verificarDivergenciaGaleria(relato);
    }

    /**
     * RS002 / RF003 nota 6: evidência da galeria com data > 7 dias ou distância > 500 m é marcada como divergente.
     */
    private void verificarDivergenciaGaleria(Relato relato) {
        if (this.origem != OrigemEvidencia.GALERIA) {
            this.evidenciaDivergente = false;
            return;
        }
        boolean divergiuData = false;
        if (this.dataHoraCaptura != null && relato.getDataHoraRegistro() != null) {
            long dias = Math.abs(Duration.between(this.dataHoraCaptura, relato.getDataHoraRegistro()).toDays());
            divergiuData = dias > MAX_DIAS_DIVERGENCIA_GALERIA;
        }
        boolean divergiuLocal = false;
        if (this.latitudeCaptura != null && this.longitudeCaptura != null) {
            double dist = GeoUtils.calcularDistanciaMetros(
                    this.latitudeCaptura.doubleValue(), this.longitudeCaptura.doubleValue(),
                    relato.getLatitude().doubleValue(), relato.getLongitude().doubleValue()
            );
            divergiuLocal = dist > MAX_METROS_DIVERGENCIA_GALERIA;
        }
        this.evidenciaDivergente = divergiuData || divergiuLocal;
    }

    public Relato getRelato() { return relato; }
    public OrigemEvidencia getOrigem() { return origem; }
    public boolean isEvidenciaDivergente() { return evidenciaDivergente; }
    public String getArquivoUsoCaminho() { return arquivoUsoCaminho; }
    public OffsetDateTime getDataHoraCaptura() { return dataHoraCaptura; }
    public BigDecimal getLatitudeCaptura() { return latitudeCaptura; }
    public BigDecimal getLongitudeCaptura() { return longitudeCaptura; }
    public Integer getDuracaoSegundos() { return duracaoSegundos; }
    public boolean isDesfoqueAplicado() { return desfoqueAplicado; }
    public String getRegiaoDesfoqueManual() { return regiaoDesfoqueManual; }
    public boolean isSomRemovido() { return somRemovido; }
    public String getTranscricao() { return transcricao; }
    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
}
