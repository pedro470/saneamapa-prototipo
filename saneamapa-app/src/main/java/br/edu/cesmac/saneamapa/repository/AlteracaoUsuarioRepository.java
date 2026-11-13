package br.edu.cesmac.saneamapa.repository;

import br.edu.cesmac.saneamapa.domain.model.AlteracaoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlteracaoUsuarioRepository extends JpaRepository<AlteracaoUsuario, Long> {
    List<AlteracaoUsuario> findTop30ByOrderByDataHoraDesc();
}
