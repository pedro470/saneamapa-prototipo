package br.edu.cesmac.saneamapa.domain.model;

import br.edu.cesmac.saneamapa.domain.exception.RegraNegocioException;
import br.edu.cesmac.saneamapa.domain.service.GeoUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "setor_censitario")
public class SetorCensitario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "importacao_id", nullable = false)
    private ImportacaoCenso importacao;

    @Column(name = "codigo_setor", nullable = false, length = 15)
    private String codigoSetor;

    @Column(length = 80)
    private String bairro;

    @Column(name = "poligono_coordenadas", nullable = false, columnDefinition = "TEXT")
    private String poligonoCoordenadas;

    @Column(name = "pct_rede_agua", nullable = false, precision = 5, scale = 2)
    private BigDecimal pctRedeAgua;

    @Column(name = "pct_rede_esgoto", nullable = false, precision = 5, scale = 2)
    private BigDecimal pctRedeEsgoto;

    protected SetorCensitario() {
    }

    public SetorCensitario(String codigoSetor, String bairro, String poligonoCoordenadas,
                           BigDecimal pctRedeAgua, BigDecimal pctRedeEsgoto) {
        if (codigoSetor == null || !codigoSetor.matches("^\\d{15}$")) {
            throw new RegraNegocioException(
                "Código de setor censitário inválido: " + codigoSetor,
                "O código do setor do IBGE deve conter exatamente 15 dígitos numéricos."
            );
        }
        if (pctRedeAgua == null || pctRedeAgua.doubleValue() < 0 || pctRedeAgua.doubleValue() > 100
                || pctRedeEsgoto == null || pctRedeEsgoto.doubleValue() < 0 || pctRedeEsgoto.doubleValue() > 100) {
            throw new RegraNegocioException(
                "Proporção inválida no setor " + codigoSetor,
                "Os percentuais de água e esgoto devem estar entre 0 e 100."
            );
        }
        this.codigoSetor = codigoSetor;
        this.bairro = bairro;
        this.poligonoCoordenadas = poligonoCoordenadas;
        this.pctRedeAgua = pctRedeAgua;
        this.pctRedeEsgoto = pctRedeEsgoto;
    }

    void vincular(ImportacaoCenso importacao) {
        this.importacao = importacao;
    }

    public boolean contemCoordenada(double lat, double lon) {
        return GeoUtils.pontoDentroDoPoligono(lat, lon, poligonoCoordenadas);
    }

    public Long getId() { return id; }
    public ImportacaoCenso getImportacao() { return importacao; }
    public String getCodigoSetor() { return codigoSetor; }
    public String getBairro() { return bairro; }
    public String getPoligonoCoordenadas() { return poligonoCoordenadas; }
    public BigDecimal getPctRedeAgua() { return pctRedeAgua; }
    public BigDecimal getPctRedeEsgoto() { return pctRedeEsgoto; }
}
