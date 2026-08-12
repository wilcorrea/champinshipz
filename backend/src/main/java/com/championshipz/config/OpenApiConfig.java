package com.championshipz.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    public static final String BEARER = "clerkJwt";

    @Bean
    public OpenAPI championshipzOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Championshipz API")
                .description("""
                    Championships, teams, matches and computed standings (leagues and cups).

                    Reads (GET) are public. Every write requires a Clerk-issued JWT and is \
                    restricted to the owner of the championship.""")
                .version("v1"))
            .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Session token issued by Clerk, validated against the issuer's JWKS")));
    }
}
