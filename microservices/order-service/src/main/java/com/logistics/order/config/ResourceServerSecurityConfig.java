package com.logistics.order.config;

import com.logistics.order.security.KeycloakJwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 🛡️ ResourceServerSecurityConfig:
 * Standard Spring Security OAuth2 Resource Server configuration validating Keycloak RS256 JWTs
 * and converting realm/client roles via KeycloakJwtAuthenticationConverter.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class ResourceServerSecurityConfig {

    private final KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints: Swagger, Actuator, Public price calculator & tracking query
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/actuator/**"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/orders/calculate-price").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/orders/calculate-tiers-concurrently").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/orders/track/**").permitAll()
                // All other business endpoints require authenticated Keycloak JWT
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter))
            );

        return http.build();
    }
}
