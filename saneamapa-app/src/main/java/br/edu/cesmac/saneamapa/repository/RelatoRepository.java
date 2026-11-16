package br.edu.cesmac.saneamapa.repository;

import br.edu.cesmac.saneamapa.domain.model.Relato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface RelatoRepository extends JpaRepository<Relato, Long> {
    long countByTelefoneMoradorAndCriadoEmAfter(String telefoneMorador, OffsetDateTime desde);
    List<Relato> findByPontoIdOrderByDataHoraRegistroDesc(Long pontoId);
    boolean existsByPontoIdAndIdentificadoTrue(Long pontoId);
}
