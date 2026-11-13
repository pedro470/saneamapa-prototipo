package br.edu.cesmac.saneamapa.security;

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
            .authorizeHttpRequests(auth -> auth
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
}
