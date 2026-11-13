package br.edu.cesmac.saneamapa.repository;

import br.edu.cesmac.saneamapa.domain.model.Vistoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VistoriaRepository extends JpaRepository<Vistoria, Long> {
    List<Vistoria> findByPontoIdOrderByDataHoraVistoriaDesc(Long pontoId);
}
