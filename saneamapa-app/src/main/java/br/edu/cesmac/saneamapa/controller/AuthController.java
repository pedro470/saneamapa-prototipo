package br.edu.cesmac.saneamapa.controller;

import br.edu.cesmac.saneamapa.dto.AuthDtos.LoginRequest;
import br.edu.cesmac.saneamapa.dto.AuthDtos.LoginResponse;
import br.edu.cesmac.saneamapa.dto.AuthDtos.RedefinirSenhaRequest;
import br.edu.cesmac.saneamapa.dto.AuthDtos.RedefinirSenhaResponse;
import br.edu.cesmac.saneamapa.security.UsuarioAutenticado;
import br.edu.cesmac.saneamapa.service.AutenticacaoService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AutenticacaoService autenticacaoService;

    public AuthController(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return autenticacaoService.autenticar(request);
    }

    @PostMapping("/redefinir-senha")
    public RedefinirSenhaResponse redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        return autenticacaoService.solicitarRedefinicao(request.email());
    }

    @GetMapping("/me")
    public UsuarioAutenticado me(@AuthenticationPrincipal UsuarioAutenticado principal) {
        return principal;
    }
}
