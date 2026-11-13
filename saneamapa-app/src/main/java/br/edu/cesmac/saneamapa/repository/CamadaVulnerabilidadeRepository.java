package br.edu.cesmac.saneamapa.repository;

import br.edu.cesmac.saneamapa.domain.model.CamadaVulnerabilidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CamadaVulnerabilidadeRepository extends JpaRepository<CamadaVulnerabilidade, Long> {
    List<CamadaVulnerabilidade> findByAtivaTrue();
    List<CamadaVulnerabilidade> findAllByOrderByAtivaDescNomeAsc();
}
