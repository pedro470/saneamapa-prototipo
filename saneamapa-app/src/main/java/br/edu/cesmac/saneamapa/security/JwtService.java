package br.edu.cesmac.saneamapa.security;

import br.edu.cesmac.saneamapa.domain.enums.PerfilUsuario;
import br.edu.cesmac.saneamapa.domain.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Emissão e validação de tokens JWT com expiração de 30 minutos (RNF16).
 */
@Service
public class JwtService {

    private final Key signingKey;
    private final long expirationMinutes;

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    public JwtService(
            @Value("${saneamapa.jwt.secret:}") String secret,
            @Value("${saneamapa.jwt.expiration-minutes:30}") long expirationMinutes) {
        this.signingKey = criarChave(secret);
        this.expirationMinutes = expirationMinutes;
    }

    /**
     * Sem JWT_SECRET definido, gera uma chave aleatória a cada inicialização: nenhum segredo fica no repositório,
     * e os tokens emitidos antes de um reinício deixam de valer (o usuário entra de novo).
     */
    private static Key criarChave(String secret) {
        if (secret == null || secret.isBlank()) {
            log.warn("JWT_SECRET não definido: usando chave aleatória gerada nesta inicialização.");
            return Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expira = agora.plus(expirationMinutes, ChronoUnit.MINUTES);
        return Jwts.builder()
                .setSubject(String.valueOf(usuario.getId()))
                .claim("email", usuario.getEmail())
                .claim("nome", usuario.getNomeCompleto())
                .claim("perfil", usuario.getPerfil().name())
                .setIssuedAt(Date.from(agora))
                .setExpiration(Date.from(expira))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public UsuarioAutenticado validarToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        Long id = Long.valueOf(claims.getSubject());
        String email = claims.get("email", String.class);
        String nome = claims.get("nome", String.class);
        PerfilUsuario perfil = PerfilUsuario.valueOf(claims.get("perfil", String.class));
        return new UsuarioAutenticado(id, email, nome, perfil);
    }
}
