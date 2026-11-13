package br.edu.cesmac.saneamapa.domain.model;

import br.edu.cesmac.saneamapa.domain.enums.TipoEventoAuditoria;
import br.edu.cesmac.saneamapa.domain.enums.TipoObjetoAuditoria;
import br.edu.cesmac.saneamapa.domain.exception.AcessoNegadoException;
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
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Registro de Auditoria LGPD imutável (RF019 / RS011).
 * Protegido tanto na camada JPA (@PreUpdate/@PreRemove) quanto por Trigger no PostgreSQL.
 */
@Entity
@Table(name = "log_auditoria")
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "ator_nome", nullable = false, length = 120)
    private String atorNome = "sistema";

    @Column(name = "data_hora", nullable = false)
    private OffsetDateTime dataHora = OffsetDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 40)
    private TipoEventoAuditoria tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_objeto", nullable = false, length = 20)
    private TipoObjetoAuditoria tipoObjeto;

    @Column(name = "objeto_id")
    private Long objetoId;

    @Column(nullable = false, length = 400)
    private String detalhes;

    protected LogAuditoria() {
    }

    public LogAuditoria(Usuario usuario, TipoEventoAuditoria tipoEvento,
                        TipoObjetoAuditoria tipoObjeto, Long objetoId, String detalhes) {
        this.usuario = usuario;
        this.atorNome = usuario != null ? usuario.getNomeCompleto() : "sistema";
        this.dataHora = OffsetDateTime.now();
        this.tipoEvento = tipoEvento;
        this.tipoObjeto = tipoObjeto;
        this.objetoId = objetoId;
        this.detalhes = detalhes != null && detalhes.length() > 400 ? detalhes.substring(0, 400) : detalhes;
    }

    @PreUpdate
    @PreRemove
    protected void impedirMutacao() {
        throw new AcessoNegadoException(
            "Registros de auditoria LGPD não podem ser alterados nem excluídos (RS011).",
            "Os logs são permanentes para conformidade legal."
        );
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getAtorNome() { return atorNome; }
    public OffsetDateTime getDataHora() { return dataHora; }
    public TipoEventoAuditoria getTipoEvento() { return tipoEvento; }
    public TipoObjetoAuditoria getTipoObjeto() { return tipoObjeto; }
    public Long getObjetoId() { return objetoId; }
    public String getDetalhes() { return detalhes; }
}
