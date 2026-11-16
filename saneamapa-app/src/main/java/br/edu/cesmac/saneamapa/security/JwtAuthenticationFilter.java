package br.edu.cesmac.saneamapa.security;

import br.edu.cesmac.saneamapa.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if ("GET".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI() != null
                && request.getRequestURI().startsWith("/api/midias/")) {
            token = request.getParameter("token");
        }

        if (token != null && !token.isBlank()) {
            try {
                UsuarioAutenticado doToken = jwtService.validarToken(token);
                // O perfil vale o que está no banco, não o que veio no token: mudança de perfil ou
                // desativação tem efeito imediato, e um claim adulterado não eleva o acesso.
                usuarioRepository.findById(doToken.id())
                        .filter(u -> u.isAtivo())
                        .ifPresent(u -> {
                            var principal = new UsuarioAutenticado(u.getId(), u.getEmail(), u.getNomeCompleto(), u.getPerfil());
                            var authority = new SimpleGrantedAuthority("ROLE_" + u.getPerfil().name());
                            var authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of(authority));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        });
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
