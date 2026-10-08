package com.example.fakeNews.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Habilita o botão "Authorize" no Swagger UI para testar as rotas protegidas com o token JWT. */
@Configuration
@OpenAPIDefinition(info = @Info(title = "CrivoNews API", version = "0.1.0",
        description = "Verificação de desinformação com IA — TFG Ciência da Computação (UFN)"))
@SecurityScheme(name = OpenApiConfig.BEARER, type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

    public static final String BEARER = "bearerAuth";
}
