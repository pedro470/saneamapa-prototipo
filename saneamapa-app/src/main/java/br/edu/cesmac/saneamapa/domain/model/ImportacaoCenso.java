package br.edu.cesmac.saneamapa.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "importacao_censo")
public class ImportacaoCenso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_arquivo", nullable = false, length = 200)
    private String nomeArquivo;

    @Column(nullable = false, length = 10)
    private String formato;

    @Column(name = "data_importacao", nullable = false)
    private OffsetDateTime dataImportacao = OffsetDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "importado_por_id", nullable = false)
    private Usuario importadoPor;

    @Column(name = "quantidade_setores", nullable = false)
    private int quantidadeSetores;

    @Column(nullable = false)
    private boolean ativa;

    @OneToMany(mappedBy = "importacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SetorCensitario> setores = new ArrayList<>();

    protected ImportacaoCenso() {
    }

    public ImportacaoCenso(String nomeArquivo, String formato, Usuario importadoPor, int quantidadeSetores, boolean ativa) {
        this.nomeArquivo = nomeArquivo;
        this.formato = formato;
        this.importadoPor = importadoPor;
        this.quantidadeSetores = quantidadeSetores;
        this.ativa = ativa;
        this.dataImportacao = OffsetDateTime.now();
    }

    public void adicionarSetor(SetorCensitario setor) {
        setor.vincular(this);
        this.setores.add(setor);
        this.quantidadeSetores = this.setores.size();
    }

    public void definirAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    public Long getId() { return id; }
    public String getNomeArquivo() { return nomeArquivo; }
    public String getFormato() { return formato; }
    public OffsetDateTime getDataImportacao() { return dataImportacao; }
    public Usuario getImportadoPor() { return importadoPor; }
    public int getQuantidadeSetores() { return quantidadeSetores; }
    public boolean isAtiva() { return ativa; }
    public List<SetorCensitario> getSetores() { return Collections.unmodifiableList(setores); }
}
