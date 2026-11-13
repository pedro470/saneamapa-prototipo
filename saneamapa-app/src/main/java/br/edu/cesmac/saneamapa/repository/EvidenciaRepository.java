package br.edu.cesmac.saneamapa.repository;

import br.edu.cesmac.saneamapa.domain.model.Evidencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvidenciaRepository extends JpaRepository<Evidencia, Long> {
    List<Evidencia> findByRelatoPontoIdOrderByIdAsc(Long pontoId);
}
