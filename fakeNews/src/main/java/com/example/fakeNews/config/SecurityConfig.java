package com.example.fakeNews.config;

import com.example.fakeNews.dto.ErroResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Regras de acesso da API:
 *  - análise de texto/URL é pública (o cidadão pode verificar sem cadastro);
 *  - histórico, validação e anotações exigem login (Seção IV.H: recurso individual e privado);
 *  - a autenticação é stateless, via token JWT no header Authorization.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationEntryPoint entryPoint,
                                                   AccessDeniedHandler accessDeniedHandler) throws Exception {
        PathPatternRequestMatcher.Builder caminho = PathPatternRequestMatcher.withDefaults();

        http
            // Sem cookies de sessão: o token vai no header, então não há risco de CSRF
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Permite o console do H2 (perfil "h2"), que é exibido dentro de um frame
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(caminho.matcher(HttpMethod.POST, "/api/auth/cadastro"),
                                 caminho.matcher(HttpMethod.POST, "/api/auth/login")).permitAll()
                .requestMatchers(caminho.matcher(HttpMethod.POST, "/api/analise/texto"),
                                 caminho.matcher(HttpMethod.POST, "/api/analise/texto/local")).permitAll()
                .requestMatchers(caminho.matcher("/swagger-ui/**"),
                                 caminho.matcher("/swagger-ui.html"),
                                 caminho.matcher("/v3/api-docs/**"),
                                 caminho.matcher("/actuator/health"),
                                 caminho.matcher("/h2-console/**"),
                                 caminho.matcher("/error")).permitAll()
                .requestMatchers(caminho.matcher(HttpMethod.OPTIONS, "/**")).permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .exceptionHandling(e -> e
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${crivo.cors.origens}") String origens) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origens.split(",")).map(String::trim).filter(o -> !o.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /** 401 no mesmo formato JSON (ErroResponse) usado pelo restante da API. */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, ex) -> {
            String mensagem = ex instanceof InvalidBearerTokenException
                    ? "Sua sessão expirou ou é inválida. Entre novamente."
                    : "Faça login para acessar este recurso.";
            escreverErro(response, objectMapper, HttpStatus.UNAUTHORIZED, mensagem);
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, ex) ->
                escreverErro(response, objectMapper, HttpStatus.FORBIDDEN, "Você não tem permissão para acessar este recurso.");
    }

    private static void escreverErro(HttpServletResponse response, ObjectMapper objectMapper,
                                     HttpStatus status, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), new ErroResponse(status.value(), mensagem, LocalDateTime.now()));
    }
}
