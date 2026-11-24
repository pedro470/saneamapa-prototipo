package br.edu.cesmac.saneamapa.repository;

import br.edu.cesmac.saneamapa.domain.model.Relato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface RelatoRepository extends JpaRepository<Relato, Long> {
    long countByTelefoneMoradorAndCriadoEmAfter(String telefoneMorador, OffsetDateTime desde);
    List<Relato> findByPontoIdOrderByDataHoraRegistroDesc(Long pontoId);

    /**
     * RNF20: relatos ainda identificados cujo ponto recebeu, depois do relato, uma vistoria anterior a {@code limite}.
     * Existir uma vistoria assim equivale a "a primeira vistoria após o relato é anterior ao limite".
     */
    @Query("""
            select r from Relato r
             where r.anonimizadoEm is null
               and exists (select v.id from Vistoria v
                            where v.ponto = r.ponto
                              and v.dataHoraVistoria >= r.dataHoraRegistro
                              and v.dataHoraVistoria < :limite)
            """)
    List<Relato> listarComPrazoDeGuardaVencido(@Param("limite") OffsetDateTime limite);
}
