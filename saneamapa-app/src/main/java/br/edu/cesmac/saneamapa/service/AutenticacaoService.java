package br.edu.cesmac.saneamapa.service;

import br.edu.cesmac.saneamapa.domain.exception.AcessoNegadoException;
import br.edu.cesmac.saneamapa.domain.model.Usuario;
import br.edu.cesmac.saneamapa.dto.AuthDtos.LoginRequest;
import br.edu.cesmac.saneamapa.dto.AuthDtos.LoginResponse;
import br.edu.cesmac.saneamapa.dto.AuthDtos.RedefinirSenhaResponse;
import br.edu.cesmac.saneamapa.repository.UsuarioRepository;
import br.edu.cesmac.saneamapa.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AutenticacaoService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AutenticacaoService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(noRollbackFor = AcessoNegadoException.class)
    public LoginResponse autenticar(LoginRequest request) {
        OffsetDateTime agora = OffsetDateTime.now();
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new AcessoNegadoException(
                        "E-mail ou senha não conferem.",
                        "Confira o e-mail e a senha digitados e tente novamente."
                ));

        usuario.validarPodeAutenticar(agora);

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            usuario.registrarFalhaAutenticacao(agora);
            usuarioRepository.save(usuario);
            if (usuario.estaBloqueado(agora)) {
                throw new AcessoNegadoException(
                        "Conta bloqueada por 15 minutos após 5 tentativas com a senha errada.",
                        "Aguarde 15 minutos ou redefina sua senha pelo e-mail cadastrado."
                );
            }
            throw new AcessoNegadoException(
                    String.format("E-mail ou senha não conferem. Restam %d tentativas antes do bloqueio.", usuario.tentativasRestantes()),
                    "Verifique a senha ou clique em 'Esqueci minha senha'."
            );
        }

        usuario.registrarSucessoAutenticacao();
        usuarioRepository.save(usuario);

        String token = jwtService.gerarToken(usuario);
        return new LoginResponse(token, usuario.getId(), usuario.getNomeCompleto(), usuario.getEmail(), usuario.getPerfil(), 30);
    }

    @Transactional(readOnly = true)
    public RedefinirSenhaResponse solicitarRedefinicao(String email) {
        return new RedefinirSenhaResponse(
                "Se o e-mail " + email.trim() + " estiver cadastrado e ativo, as instruções de redefinição foram enviadas."
        );
    }
}
