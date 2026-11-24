package br.edu.cesmac.saneamapa.repository;

import br.edu.cesmac.saneamapa.domain.enums.StatusPonto;
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
     * RNF20: relatos ainda identificados cujo prazo de guarda venceu antes de {@code limite}. Há dois casos:
     * <ul>
     *   <li>o ponto recebeu, depois do relato, uma vistoria anterior ao limite (equivale a "a primeira vistoria
     *       após o relato é anterior ao limite");</li>
     *   <li>o relato chegou a um ponto já confirmado, que não recebe nova vistoria (reincidência, RF004), e foi
     *       registrado antes do limite.</li>
     * </ul>
     */
    @Query("""
            select r from Relato r
             where r.anonimizadoEm is null
               and (
                    exists (select v.id from Vistoria v
                             where v.ponto = r.ponto
                               and v.dataHoraVistoria >= r.dataHoraRegistro
                               and v.dataHoraVistoria < :limite)
                    or (r.ponto.status = :confirmado
                        and r.dataHoraRegistro < :limite
                        and exists (select v2.id from Vistoria v2
                                     where v2.ponto = r.ponto
                                       and v2.dataHoraVistoria < r.dataHoraRegistro))
               )
            """)
    List<Relato> listarComPrazoDeGuardaVencido(@Param("limite") OffsetDateTime limite,
                                               @Param("confirmado") StatusPonto confirmado);
}
