package br.edu.cesmac.saneamapa.repository;

import br.edu.cesmac.saneamapa.domain.model.ImportacaoCenso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImportacaoCensoRepository extends JpaRepository<ImportacaoCenso, Long> {
    Optional<ImportacaoCenso> findFirstByAtivaTrue();
    List<ImportacaoCenso> findAllByOrderByDataImportacaoDesc();
}
