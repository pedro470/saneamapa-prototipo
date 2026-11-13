package br.edu.cesmac.saneamapa.repository;

import br.edu.cesmac.saneamapa.domain.enums.PerfilUsuario;
import br.edu.cesmac.saneamapa.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCase(String email);
    boolean existsByCpf(String cpf);
    boolean existsByEmailIgnoreCase(String email);
    List<Usuario> findByPerfilAndAtivoTrueOrderByNomeCompletoAsc(PerfilUsuario perfil);
    List<Usuario> findAllByOrderByNomeCompletoAsc();
}
