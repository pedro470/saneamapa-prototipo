package br.edu.cesmac.saneamapa.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "item_fila_vistoria")
public class ItemFilaVistoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ponto_id", nullable = false, unique = true)
    private Ponto ponto;

    @Column(name = "data_entrada", nullable = false)
    private OffsetDateTime dataEntrada = OffsetDateTime.now();

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal pontuacao = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_atribuido_id")
    private Usuario fiscalAtribuido;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();

    protected ItemFilaVistoria() {
    }

    public ItemFilaVistoria(Ponto ponto, BigDecimal pontuacao) {
        this.ponto = ponto;
        this.dataEntrada = OffsetDateTime.now();
        this.pontuacao = pontuacao;
        this.fiscalAtribuido = ponto.getFiscalAtribuido();
        this.atualizadoEm = OffsetDateTime.now();
    }

    public void atualizarPontuacao(BigDecimal novaPontuacao) {
        this.pontuacao = novaPontuacao;
        this.atualizadoEm = OffsetDateTime.now();
    }

    public void sincronizarFiscal(Usuario fiscal) {
        this.fiscalAtribuido = fiscal;
        this.atualizadoEm = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Ponto getPonto() { return ponto; }
    public OffsetDateTime getDataEntrada() { return dataEntrada; }
    public BigDecimal getPontuacao() { return pontuacao; }
    public Usuario getFiscalAtribuido() { return fiscalAtribuido; }
    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
}
