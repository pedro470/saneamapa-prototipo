package br.edu.cesmac.saneamapa.repository;

import br.edu.cesmac.saneamapa.domain.model.SetorCensitario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SetorCensitarioRepository extends JpaRepository<SetorCensitario, Long> {
    List<SetorCensitario> findByImportacaoAtivaTrue();
}
