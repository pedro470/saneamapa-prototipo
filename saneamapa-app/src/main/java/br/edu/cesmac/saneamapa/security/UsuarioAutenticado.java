package br.edu.cesmac.saneamapa.security;

import br.edu.cesmac.saneamapa.domain.enums.PerfilUsuario;

public record UsuarioAutenticado(
        Long id,
        String email,
        String nome,
        PerfilUsuario perfil
) {
}
