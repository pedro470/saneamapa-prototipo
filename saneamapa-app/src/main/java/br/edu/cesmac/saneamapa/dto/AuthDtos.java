package br.edu.cesmac.saneamapa.dto;

import br.edu.cesmac.saneamapa.domain.enums.PerfilUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    public record LoginRequest(
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") String email,
            @NotBlank(message = "Informe a senha.") String senha
    ) {}

    public record LoginResponse(
            String token,
            Long id,
            String nome,
            String email,
            PerfilUsuario perfil,
            int expiraEmMinutos
    ) {}

    public record RedefinirSenhaRequest(
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") String email
    ) {}

    public record RedefinirSenhaResponse(
            String mensagem
    ) {}
}
