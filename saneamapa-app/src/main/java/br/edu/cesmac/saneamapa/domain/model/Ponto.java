package br.edu.cesmac.saneamapa.domain.model;

import br.edu.cesmac.saneamapa.domain.enums.NivelUrgencia;
import br.edu.cesmac.saneamapa.domain.enums.PerfilUsuario;
import br.edu.cesmac.saneamapa.domain.enums.ResultadoVistoria;
import br.edu.cesmac.saneamapa.domain.enums.StatusPonto;
import br.edu.cesmac.saneamapa.domain.enums.TipoOcorrencia;
import br.edu.cesmac.saneamapa.domain.exception.RegraNegocioException;
import br.edu.cesmac.saneamapa.domain.service.GeoUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Ponto geográfico que agrupa relatos do mesmo tipo num raio de 50 m (RF004).
 * Encapsula a máquina de estados do Ciclo de Vida do Ponto (Seção 10.1).
 */
@Entity
@Table(name = "ponto")
public class Ponto extends EntidadeAuditavel {

    @Column(name = "codigo_exibicao", nullable = false, unique = true, length = 12)
    private String codigoExibicao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ocorrencia", nullable = false, length = 30)
    private TipoOcorrencia tipoOcorrencia;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "trecho_rua", nullable = false, length = 200)
    private String trechoRua;

    @Column(nullable = false, length = 80)
    private String bairro;

    @Column(name = "quantidade_relatos", nullable = false)
    private int quantidadeRelatos = 1;

    @Column(name = "data_primeiro_relato", nullable = false)
    private LocalDate dataPrimeiroRelato;

    @Column(name = "data_ultimo_relato", nullable = false)
    private LocalDate dataUltimoRelato;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private StatusPonto status = StatusPonto.AGUARDANDO_VISTORIA;

    @Column(nullable = false)
    private boolean reincidencia = false;

    @Column(name = "peso_urgencia", nullable = false)
    private short pesoUrgencia = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_urgencia", nullable = false, length = 15)
    private NivelUrgencia nivelUrgencia = NivelUrgencia.NORMAL;

    @Column(name = "motivo_urgencia", length = 300)
    private String motivoUrgencia = "Normal: nenhuma camada de vulnerabilidade no local";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_atribuido_id")
    private Usuario fiscalAtribuido;

    @Column(name = "posicao_manual_algum_relato", nullable = false)
    private boolean posicaoManualAlgumRelato = false;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();

    protected Ponto() {
    }

    public Ponto(String codigoExibicao, TipoOcorrencia tipoOcorrencia, BigDecimal latitude, BigDecimal longitude,
                 String trechoRua, String bairro, LocalDate dataInicial, boolean posicaoManual) {
        this.codigoExibicao = codigoExibicao;
        this.tipoOcorrencia = tipoOcorrencia;
        this.latitude = latitude;
        this.longitude = longitude;
        this.trechoRua = trechoRua;
        this.bairro = (bairro != null && !bairro.isBlank()) ? bairro.trim() : "Maceió";
        this.quantidadeRelatos = 1;
        this.dataPrimeiroRelato = dataInicial;
        this.dataUltimoRelato = dataInicial;
        this.status = StatusPonto.AGUARDANDO_VISTORIA;
        this.posicaoManualAlgumRelato = posicaoManual;
        this.atualizadoEm = OffsetDateTime.now();
    }

    @PreUpdate
    protected void aoAtualizar() {
        this.atualizadoEm = OffsetDateTime.now();
    }

    public void registrarVinculoNovoRelato(LocalDate dataRelato, boolean posicaoManual) {
        this.quantidadeRelatos++;
        if (dataRelato.isAfter(this.dataUltimoRelato)) {
            this.dataUltimoRelato = dataRelato;
        }
        if (posicaoManual) {
            this.posicaoManualAlgumRelato = true;
        }
        // RF004: relato em ponto CONFIRMADO marca reincidência sem alterar status; em NAO_CONFIRMADO retorna à fila.
        if (this.status == StatusPonto.CONFIRMADO) {
            this.reincidencia = true;
        } else if (this.status == StatusPonto.NAO_CONFIRMADO) {
            transicionarPara(StatusPonto.AGUARDANDO_VISTORIA);
            this.fiscalAtribuido = null;
        }
    }

    public void atribuirFiscal(Usuario fiscal) {
        if (fiscal == null) {
            retirarAtribuicao();
            return;
        }
        if (fiscal.getPerfil() != PerfilUsuario.FISCAL || !fiscal.isAtivo()) {
            throw new RegraNegocioException(
                "A atribuição exige um usuário ativo com perfil Fiscal.",
                "Escolha um fiscal ativo na lista para atribuir a vistoria."
            );
        }
        if (this.status == StatusPonto.CONFIRMADO || this.status == StatusPonto.NAO_CONFIRMADO) {
            throw new RegraNegocioException(
                "Ponto já vistoriado não pode receber atribuição na fila.",
                "Apenas pontos aguardando ou em vistoria podem ser atribuídos."
            );
        }
        transicionarPara(StatusPonto.EM_VISTORIA);
        this.fiscalAtribuido = fiscal;
    }

    public void retirarAtribuicao() {
        if (this.status == StatusPonto.EM_VISTORIA) {
            transicionarPara(StatusPonto.AGUARDANDO_VISTORIA);
        }
        this.fiscalAtribuido = null;
    }

    public void aplicarResultadoVistoria(ResultadoVistoria resultado, TipoOcorrencia tipoCorrigido,
                                         BigDecimal novaLat, BigDecimal novaLon) {
        if (this.status != StatusPonto.EM_VISTORIA) {
            throw new RegraNegocioException(
                "Somente pontos em vistoria podem receber registro de vistoria.",
                "Atribua o ponto ao fiscal antes de registrar a vistoria."
            );
        }
        if (tipoCorrigido != null) {
            this.tipoOcorrencia = tipoCorrigido;
        }
        if (novaLat != null && novaLon != null) {
            this.latitude = novaLat;
            this.longitude = novaLon;
        }
        transicionarPara(resultado.getStatusResultante());
    }

    public void aplicarClassificacaoUrgencia(int peso, String motivo) {
        this.pesoUrgencia = (short) Math.max(0, Math.min(3, peso));
        this.nivelUrgencia = NivelUrgencia.porPeso(this.pesoUrgencia);
        this.motivoUrgencia = motivo;
    }

    public double distanciaEmMetrosPara(double lat, double lon) {
        return GeoUtils.calcularDistanciaMetros(this.latitude.doubleValue(), this.longitude.doubleValue(), lat, lon);
    }

    private void transicionarPara(StatusPonto destino) {
        if (!this.status.podeTransicionarPara(destino)) {
            throw new RegraNegocioException(
                String.format("Transição de status inválida: %s para %s.", this.status.getRotulo(), destino.getRotulo()),
                "Verifique o ciclo de vida permitido para o ponto."
            );
        }
        this.status = destino;
    }

    public String getCodigoExibicao() { return codigoExibicao; }
    public TipoOcorrencia getTipoOcorrencia() { return tipoOcorrencia; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public String getTrechoRua() { return trechoRua; }
    public String getBairro() { return bairro; }
    public int getQuantidadeRelatos() { return quantidadeRelatos; }
    public LocalDate getDataPrimeiroRelato() { return dataPrimeiroRelato; }
    public LocalDate getDataUltimoRelato() { return dataUltimoRelato; }
    public StatusPonto getStatus() { return status; }
    public boolean isReincidencia() { return reincidencia; }
    public short getPesoUrgencia() { return pesoUrgencia; }
    public NivelUrgencia getNivelUrgencia() { return nivelUrgencia; }
    public String getMotivoUrgencia() { return motivoUrgencia; }
    public Usuario getFiscalAtribuido() { return fiscalAtribuido; }
    public boolean isPosicaoManualAlgumRelato() { return posicaoManualAlgumRelato; }
    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
}
