package br.edu.cesmac.saneamapa.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // RNF03: falhas de acesso respondem com motivo + o que fazer, no mesmo formato do GlobalExceptionHandler.
            // 401 = sem login ou sessão expirada (o front volta à tela de login); 403 = logado, mas sem permissão.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> escreverErro(res, HttpServletResponse.SC_UNAUTHORIZED,
                        "Sua sessão expirou ou você não está conectado.",
                        "Entre novamente com seu e-mail e senha."))
                .accessDeniedHandler((req, res, e) -> escreverErro(res, HttpServletResponse.SC_FORBIDDEN,
                        "Seu perfil não tem permissão para esta operação.",
                        "Entre com uma conta do perfil correto."))
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .requestMatchers("/", "/index.html", "/morador.html", "/fiscal.html", "/painel.html",
                        "/css/**", "/js/**", "/ds/**", "/favicon.ico").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/redefinir-senha").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/morador/relatos", "/api/morador/midias").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/publico/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/midias/**").hasAnyRole("FISCAL", "PREFEITURA")
                .requestMatchers("/api/fiscal/**").hasRole("FISCAL")
                .requestMatchers("/api/prefeitura/**").hasRole("PREFEITURA")
                .requestMatchers("/api/auth/me").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static void escreverErro(HttpServletResponse res, int status, String erro, String orientacao)
            throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        Map<String, String> corpo = new LinkedHashMap<>();
        corpo.put("erro", erro);
        corpo.put("orientacao", orientacao);
        JSON.writeValue(res.getOutputStream(), corpo);
    }

    private static final ObjectMapper JSON = new ObjectMapper();
}
