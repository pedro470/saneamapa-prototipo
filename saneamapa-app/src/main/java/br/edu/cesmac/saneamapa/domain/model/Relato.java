package br.edu.cesmac.saneamapa.domain.model;

import br.edu.cesmac.saneamapa.domain.enums.SituacaoEnvio;
import br.edu.cesmac.saneamapa.domain.enums.TipoOcorrencia;
import br.edu.cesmac.saneamapa.domain.exception.RegraNegocioException;
import br.edu.cesmac.saneamapa.domain.service.CpfUtils;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Relato enviado pelo morador (RF001, RF002).
 * Toda denúncia é identificada (RF001, nota 5): nome e telefone são obrigatórios.
 */
@Entity
@Table(name = "relato")
public class Relato extends EntidadeAuditavel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ponto_id", nullable = false)
    private Ponto ponto;

    @Column(nullable = false, unique = true, length = 20)
    private String protocolo;

    @Column(name = "data_hora_registro", nullable = false)
    private OffsetDateTime dataHoraRegistro;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ocorrencia", nullable = false, length = 30)
    private TipoOcorrencia tipoOcorrencia;

    @Column(length = 2350)
    private String descricao;

    @Column(name = "nome_morador", nullable = false, length = 120)
    private String nomeMorador;

    @Column(name = "telefone_morador", nullable = false, length = 15)
    private String telefoneMorador;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao_envio", nullable = false, length = 15)
    private SituacaoEnvio situacaoEnvio = SituacaoEnvio.ENVIADO;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "precisao_metros", nullable = false)
    private int precisaoMetros;

    @Column(length = 150)
    private String logradouro;

    @Column(length = 80)
    private String bairro;

    @Column(name = "ponto_referencia", length = 100)
    private String pontoReferencia;

    @Column(name = "posicao_manual", nullable = false)
    private boolean posicaoManual;

    @OneToMany(mappedBy = "relato", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Evidencia> evidencias = new ArrayList<>();

    protected Relato() {
    }

    public Relato(Ponto ponto, String protocolo, OffsetDateTime dataHoraRegistro, TipoOcorrencia tipoOcorrencia,
                  String descricao, String nomeMorador, String telefoneMorador,
                  BigDecimal latitude, BigDecimal longitude, int precisaoMetros,
                  String logradouro, String bairro, String pontoReferencia, boolean posicaoManual) {
        if (nomeMorador == null || nomeMorador.isBlank()) {
            throw new RegraNegocioException(
                "Nome do morador é obrigatório.",
                "Preencha seu nome. Ele só é visto pela equipe da prefeitura."
            );
        }
        if (!CpfUtils.isTelefoneValido(telefoneMorador)) {
            throw new RegraNegocioException(
                "Telefone com DDD inválido.",
                "Informe o telefone com DDD no formato (xx) xxxxx-xxxx ou (xx) xxxx-xxxx."
            );
        }
        this.nomeMorador = nomeMorador.trim();
        this.telefoneMorador = telefoneMorador.trim();

        this.ponto = ponto;
        this.protocolo = protocolo;
        this.dataHoraRegistro = dataHoraRegistro != null ? dataHoraRegistro : OffsetDateTime.now();
        this.tipoOcorrencia = tipoOcorrencia;
        this.descricao = descricao != null ? descricao.trim() : null;
        this.situacaoEnvio = SituacaoEnvio.ENVIADO;
        this.latitude = latitude;
        this.longitude = longitude;
        this.precisaoMetros = Math.max(0, precisaoMetros);
        this.logradouro = logradouro != null ? logradouro.trim() : null;
        this.bairro = bairro != null ? bairro.trim() : null;
        this.pontoReferencia = pontoReferencia != null ? pontoReferencia.trim() : null;
        this.posicaoManual = posicaoManual;
    }

    public void adicionarEvidencia(Evidencia evidencia) {
        evidencia.validarRegrasCaptura();
        evidencia.aplicarTratamentoInicial();
        evidencia.vincularAoRelato(this);
        this.evidencias.add(evidencia);
    }

    public void acrescentarTranscricao(String transcricao) {
        if (transcricao == null || transcricao.isBlank()) {
            return;
        }
        String prefixo = (this.descricao == null || this.descricao.isBlank()) ? "" : this.descricao + "\n";
        String combinado = prefixo + "[Transcrição IA]: " + transcricao.trim();
        this.descricao = combinado.length() > 2350 ? combinado.substring(0, 2350) : combinado;
    }

    public Ponto getPonto() { return ponto; }
    public String getProtocolo() { return protocolo; }
    public OffsetDateTime getDataHoraRegistro() { return dataHoraRegistro; }
    public TipoOcorrencia getTipoOcorrencia() { return tipoOcorrencia; }
    public String getDescricao() { return descricao; }
    public String getNomeMorador() { return nomeMorador; }
    public String getTelefoneMorador() { return telefoneMorador; }
    public SituacaoEnvio getSituacaoEnvio() { return situacaoEnvio; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public int getPrecisaoMetros() { return precisaoMetros; }
    public String getLogradouro() { return logradouro; }
    public String getBairro() { return bairro; }
    public String getPontoReferencia() { return pontoReferencia; }
    public boolean isPosicaoManual() { return posicaoManual; }
    public List<Evidencia> getEvidencias() { return Collections.unmodifiableList(evidencias); }
}
