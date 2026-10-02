package com.example.fakeNews.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Chave e codificadores dos tokens JWT (HMAC-SHA256). O próprio backend emite
 * os tokens no login e os valida em cada requisição autenticada.
 */
@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);
    private static final int TAMANHO_MINIMO_SEGREDO = 32;

    @Bean
    public SecretKey jwtSecretKey(@Value("${crivo.jwt.segredo:}") String segredo) {
        byte[] bytes;
        if (segredo == null || segredo.isBlank()) {
            bytes = new byte[TAMANHO_MINIMO_SEGREDO];
            new SecureRandom().nextBytes(bytes);
            log.warn("JWT_SECRET não definido: usando um segredo aleatório. "
                    + "Os tokens deixam de valer quando a aplicação reinicia.");
        } else {
            bytes = segredo.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < TAMANHO_MINIMO_SEGREDO) {
                throw new IllegalStateException(
                        "JWT_SECRET deve ter pelo menos " + TAMANHO_MINIMO_SEGREDO + " caracteres.");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
