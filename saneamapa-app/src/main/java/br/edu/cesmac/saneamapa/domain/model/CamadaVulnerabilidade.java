package br.edu.cesmac.saneamapa.domain.model;

import br.edu.cesmac.saneamapa.domain.enums.CategoriaCamada;
import br.edu.cesmac.saneamapa.domain.enums.TipoGeometria;
import br.edu.cesmac.saneamapa.domain.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Camada de sensibilidade social (RF006 / RS012).
 * Garante que camadas epidemiológicas aceitem apenas polígono e delega a verificação espacial polimorficamente.
 */
@Entity
@Table(name = "camada_vulnerabilidade")
public class CamadaVulnerabilidade extends EntidadeAuditavel {

    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CategoriaCamada categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_geometria", nullable = false, length = 15)
    private TipoGeometria tipoGeometria;

    @Column(name = "centro_latitude", precision = 10, scale = 7)
    private BigDecimal centroLatitude;

    @Column(name = "centro_longitude", precision = 10, scale = 7)
    private BigDecimal centroLongitude;

    @Column(name = "raio_metros")
    private Integer raioMetros;

    @Column(name = "poligono_coordenadas", columnDefinition = "TEXT")
    private String poligonoCoordenadas;

    @Column(name = "peso_urgencia", nullable = false)
    private short pesoUrgencia;

    @Column(name = "fonte_dado", nullable = false, length = 200)
    private String fonteDado;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDate dataAtualizacao;

    @Column(nullable = false)
    private boolean ativa = true;

    protected CamadaVulnerabilidade() {
    }

    public CamadaVulnerabilidade(String nome, CategoriaCamada categoria, TipoGeometria tipoGeometria,
                                 BigDecimal centroLatitude, BigDecimal centroLongitude, Integer raioMetros,
                                 String poligonoCoordenadas, int pesoUrgencia, String fonteDado,
                                 LocalDate dataAtualizacao, boolean ativa) {
        validar(categoria, tipoGeometria, centroLatitude, centroLongitude, raioMetros, poligonoCoordenadas, pesoUrgencia);
        this.nome = nome.trim();
        this.categoria = categoria;
        this.tipoGeometria = tipoGeometria;
        this.centroLatitude = tipoGeometria == TipoGeometria.PONTO_RAIO ? centroLatitude : null;
        this.centroLongitude = tipoGeometria == TipoGeometria.PONTO_RAIO ? centroLongitude : null;
        this.raioMetros = tipoGeometria == TipoGeometria.PONTO_RAIO ? raioMetros : null;
        this.poligonoCoordenadas = tipoGeometria == TipoGeometria.POLIGONO ? poligonoCoordenadas : null;
        this.pesoUrgencia = (short) pesoUrgencia;
        this.fonteDado = fonteDado.trim();
        this.dataAtualizacao = dataAtualizacao != null ? dataAtualizacao : LocalDate.now();
        this.ativa = ativa;
    }

    private static void validar(CategoriaCamada categoria, TipoGeometria tipoGeometria,
                                BigDecimal lat, BigDecimal lon, Integer raio,
                                String poligono, int peso) {
        if (peso < 1 || peso > 3) {
            throw new RegraNegocioException("Peso de urgência inválido.", "Informe um peso entre 1 e 3.");
        }
        if (categoria.isExigePoligono() && tipoGeometria != TipoGeometria.POLIGONO) {
            throw new RegraNegocioException(
                "A categoria Vulnerabilidade epidemiológica aceita somente polígono (RS012).",
                "Altere o tipo de geometria para Polígono para não expor endereços individuais."
            );
        }
        if (tipoGeometria == TipoGeometria.PONTO_RAIO && (lat == null || lon == null || raio == null || raio <= 0)) {
            throw new RegraNegocioException(
                "Geometria ponto com raio incompleta.",
                "Informe latitude, longitude e raio de influência maior que zero."
            );
        }
        if (tipoGeometria == TipoGeometria.POLIGONO && (poligono == null || poligono.isBlank())) {
            throw new RegraNegocioException(
                "Polígono da camada não informado.",
                "Desenhe ou forneça as coordenadas do polígono territorial."
            );
        }
    }

    public GeometriaCamada obterGeometria() {
        if (tipoGeometria == TipoGeometria.PONTO_RAIO) {
            return new GeometriaPontoRaio(centroLatitude.doubleValue(), centroLongitude.doubleValue(), raioMetros);
        }
        return new GeometriaPoligono(poligonoCoordenadas);
    }

    public void alterarSituacao(boolean ativa) {
        this.ativa = ativa;
    }

    public String getNome() { return nome; }
    public CategoriaCamada getCategoria() { return categoria; }
    public TipoGeometria getTipoGeometria() { return tipoGeometria; }
    public BigDecimal getCentroLatitude() { return centroLatitude; }
    public BigDecimal getCentroLongitude() { return centroLongitude; }
    public Integer getRaioMetros() { return raioMetros; }
    public String getPoligonoCoordenadas() { return poligonoCoordenadas; }
    public short getPesoUrgencia() { return pesoUrgencia; }
    public String getFonteDado() { return fonteDado; }
    public LocalDate getDataAtualizacao() { return dataAtualizacao; }
    public boolean isAtiva() { return ativa; }
}
