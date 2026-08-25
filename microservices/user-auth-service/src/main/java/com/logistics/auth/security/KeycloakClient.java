package com.logistics.auth.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class KeycloakClient {

    @Value("${keycloak.server-url:http://localhost:8180}")
    private String serverUrl;

    @Value("${keycloak.realm:logistics-realm}")
    private String realm;

    @Value("${keycloak.client-id:logistics-api-gateway}")
    private String clientId;

    @Value("${keycloak.client-secret:logistics-gateway-secret-2024-enterprise-jwt}")
    private String clientSecret;

    @Value("${keycloak.enabled:true}")
    private boolean keycloakEnabled;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record KeycloakTokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            String scope
    ) {}

    /**
     * Authenticates with Keycloak via Direct Access Grants (Resource Owner Password Credentials).
     */
    public Optional<KeycloakTokenResponse> login(String username, String password) {
        if (!keycloakEnabled) {
            return Optional.empty();
        }

        try {
            String tokenEndpoint = String.format("%s/realms/%s/protocol/openid-connect/token", serverUrl, realm);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("username", username);
            body.add("password", password);
            body.add("scope", "openid profile email roles");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(tokenEndpoint, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                return Optional.of(new KeycloakTokenResponse(
                        root.path("access_token").asText(),
                        root.path("refresh_token").asText(),
                        root.path("token_type").asText("Bearer"),
                        root.path("expires_in").asLong(3600),
                        root.path("scope").asText("")
                ));
            }
        } catch (Exception e) {
            log.warn("Keycloak direct authentication attempt failed ({}), fallback to local authentication: {}",
                    username, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Refreshes access token via Keycloak refresh token grant.
     */
    public Optional<KeycloakTokenResponse> refreshToken(String refreshToken) {
        if (!keycloakEnabled) {
            return Optional.empty();
        }

        try {
            String tokenEndpoint = String.format("%s/realms/%s/protocol/openid-connect/token", serverUrl, realm);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("refresh_token", refreshToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(tokenEndpoint, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                return Optional.of(new KeycloakTokenResponse(
                        root.path("access_token").asText(),
                        root.path("refresh_token").asText(),
                        root.path("token_type").asText("Bearer"),
                        root.path("expires_in").asLong(3600),
                        root.path("scope").asText("")
                ));
            }
        } catch (Exception e) {
            log.warn("Keycloak refresh token exchange failed: {}", e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Revokes a session/refresh token in Keycloak.
     */
    public void logout(String refreshToken) {
        if (!keycloakEnabled || refreshToken == null) {
            return;
        }

        try {
            String logoutEndpoint = String.format("%s/realms/%s/protocol/openid-connect/logout", serverUrl, realm);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("refresh_token", refreshToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(logoutEndpoint, request, String.class);
            log.info("Successfully invalidated session in Keycloak");
        } catch (Exception e) {
            log.warn("Keycloak session logout call failed: {}", e.getMessage());
        }
    }
}
