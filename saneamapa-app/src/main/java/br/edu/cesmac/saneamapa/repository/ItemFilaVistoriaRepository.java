package br.edu.cesmac.saneamapa.repository;

import br.edu.cesmac.saneamapa.domain.model.ItemFilaVistoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemFilaVistoriaRepository extends JpaRepository<ItemFilaVistoria, Long> {
    Optional<ItemFilaVistoria> findByPontoId(Long pontoId);

    @Query("SELECT f FROM ItemFilaVistoria f JOIN FETCH f.ponto p LEFT JOIN FETCH f.fiscalAtribuido " +
           "WHERE p.status IN ('AGUARDANDO_VISTORIA', 'EM_VISTORIA') " +
           "ORDER BY f.pontuacao DESC, f.dataEntrada ASC")
    List<ItemFilaVistoria> listarFilaOrdenada();

    @Query("SELECT f FROM ItemFilaVistoria f JOIN FETCH f.ponto p " +
           "WHERE f.fiscalAtribuido.id = :fiscalId AND p.status = 'EM_VISTORIA' " +
           "ORDER BY f.pontuacao DESC, f.dataEntrada ASC")
    List<ItemFilaVistoria> listarAtribuidosAoFiscal(@Param("fiscalId") Long fiscalId);
}
