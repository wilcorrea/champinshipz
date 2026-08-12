package com.championshipz.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private static final String[] PUBLIC_DOCS = {
        "/v3/api-docs/**", "/v3/api-docs.yaml", "/swagger-ui/**", "/swagger-ui.html"
    };

    private final String issuerUri;

    public SecurityConfig(@Value("${app.auth.issuer-uri:}") String issuerUri) {
        this.issuerUri = issuerUri == null ? "" : issuerUri.trim();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean identityConfigured = !issuerUri.isBlank();

        if (!identityConfigured) {
            log.warn("app.auth.issuer-uri não configurado: leitura segue pública e toda escrita será recusada. "
                + "Defina CLERK_ISSUER_URI para habilitar login.");
        }

        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(PUBLIC_DOCS).permitAll();
                auth.requestMatchers(HttpMethod.GET, "/api/**").permitAll();
                if (identityConfigured) {
                    auth.anyRequest().authenticated();
                } else {
                    auth.anyRequest().denyAll();
                }
            });

        if (identityConfigured) {
            http.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.decoder(jwtDecoder())));
        }

        return http.build();
    }

    /**
     * Monta o decoder a partir do JWKS conhecido do issuer em vez de descobri-lo por
     * {@code /.well-known/openid-configuration}. A descoberta é uma chamada de rede
     * bloqueante na criação do bean: sem internet, a API inteira deixa de subir — até a
     * leitura pública, que não depende de identidade nenhuma. Aqui as chaves são buscadas
     * na primeira validação de token e ficam em cache.
     */
    private JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
            .withJwkSetUri(issuerUri + "/.well-known/jwks.json")
            .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
        return decoder;
    }
}
