package br.edu.cesmac.saneamapa.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "alteracao_usuario")
public class AlteracaoUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_alvo_id", nullable = false)
    private Usuario usuarioAlvo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(name = "campo_alterado", nullable = false, length = 60)
    private String campoAlterado;

    @Column(name = "valor_anterior", length = 255)
    private String valorAnterior;

    @Column(name = "valor_novo", length = 255)
    private String valorNovo;

    @Column(name = "data_hora", nullable = false)
    private OffsetDateTime dataHora = OffsetDateTime.now();

    protected AlteracaoUsuario() {
    }

    public AlteracaoUsuario(Usuario usuarioAlvo, Usuario autor, String campoAlterado, String valorAnterior, String valorNovo) {
        this.usuarioAlvo = usuarioAlvo;
        this.autor = autor;
        this.campoAlterado = campoAlterado;
        this.valorAnterior = valorAnterior;
        this.valorNovo = valorNovo;
        this.dataHora = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Usuario getUsuarioAlvo() { return usuarioAlvo; }
    public Usuario getAutor() { return autor; }
    public String getCampoAlterado() { return campoAlterado; }
    public String getValorAnterior() { return valorAnterior; }
    public String getValorNovo() { return valorNovo; }
    public OffsetDateTime getDataHora() { return dataHora; }
}
