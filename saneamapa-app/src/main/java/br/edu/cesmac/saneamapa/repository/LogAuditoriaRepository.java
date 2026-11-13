package br.edu.cesmac.saneamapa.repository;

import br.edu.cesmac.saneamapa.domain.enums.TipoEventoAuditoria;
import br.edu.cesmac.saneamapa.domain.enums.TipoObjetoAuditoria;
import br.edu.cesmac.saneamapa.domain.model.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    @Query("SELECT l FROM LogAuditoria l WHERE " +
           "(:usuarioId IS NULL OR l.usuario.id = :usuarioId) AND " +
           "(:sistema IS FALSE OR l.usuario IS NULL) AND " +
           "(:tipoEvento IS NULL OR l.tipoEvento = :tipoEvento) AND " +
           "(:de IS NULL OR l.dataHora >= :de) AND " +
           "(:ate IS NULL OR l.dataHora <= :ate) " +
           "ORDER BY l.dataHora DESC")
    List<LogAuditoria> buscarFiltrado(@Param("usuarioId") Long usuarioId,
                                      @Param("sistema") boolean apenasSistema,
                                      @Param("tipoEvento") TipoEventoAuditoria tipoEvento,
                                      @Param("de") OffsetDateTime de,
                                      @Param("ate") OffsetDateTime ate);

    List<LogAuditoria> findTop20ByTipoObjetoAndObjetoIdOrderByDataHoraDesc(TipoObjetoAuditoria tipoObjeto, Long objetoId);
}
