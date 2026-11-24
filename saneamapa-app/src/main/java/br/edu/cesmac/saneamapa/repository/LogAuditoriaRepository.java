package br.edu.cesmac.saneamapa.repository;

import br.edu.cesmac.saneamapa.domain.enums.TipoEventoAuditoria;
import br.edu.cesmac.saneamapa.domain.enums.TipoObjetoAuditoria;
import br.edu.cesmac.saneamapa.domain.model.LogAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    String FILTRO = "FROM LogAuditoria l WHERE " +
           "(:usuarioId IS NULL OR l.usuario.id = :usuarioId) AND " +
           "(:sistema IS FALSE OR l.usuario IS NULL) AND " +
           "(:tipoEvento IS NULL OR l.tipoEvento = :tipoEvento) AND " +
           "l.dataHora >= :de AND " +
           "l.dataHora <= :ate";

    @Query(value = "SELECT l " + FILTRO + " ORDER BY l.dataHora DESC",
           countQuery = "SELECT COUNT(l) " + FILTRO)
    Page<LogAuditoria> buscarFiltrado(@Param("usuarioId") Long usuarioId,
                                      @Param("sistema") boolean apenasSistema,
                                      @Param("tipoEvento") TipoEventoAuditoria tipoEvento,
                                      @Param("de") OffsetDateTime de,
                                      @Param("ate") OffsetDateTime ate,
                                      Pageable pagina);

    List<LogAuditoria> findTop20ByTipoObjetoAndObjetoIdOrderByDataHoraDesc(TipoObjetoAuditoria tipoObjeto, Long objetoId);
}
