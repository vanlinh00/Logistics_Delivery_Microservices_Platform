package com.logistics.auth.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component
@Slf4j
public class KeycloakClient {

    private final RestTemplate restTemplate;
    private final String tokenUrl;
    private final String logoutUrl;
    private final String clientId;
    private final String clientSecret;
    private final boolean enabled;

    public record KeycloakTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in") long expiresIn,
            @JsonProperty("refresh_expires_in") long refreshExpiresIn,
            @JsonProperty("token_type") String tokenType
    ) {}

    public KeycloakClient(
            @Value("${keycloak.server-url:http://localhost:8180}") String serverUrl,
            @Value("${keycloak.realm:logistics-realm}") String realm,
            @Value("${keycloak.client-id:logistics-api-gateway}") String clientId,
            @Value("${keycloak.client-secret:logistics-gateway-secret-2024-enterprise-jwt}") String clientSecret,
            @Value("${keycloak.enabled:true}") boolean enabled) {
        this.restTemplate = new RestTemplate();
        this.tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", serverUrl, realm);
        this.logoutUrl = String.format("%s/realms/%s/protocol/openid-connect/logout", serverUrl, realm);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.enabled = enabled;
    }

    public Optional<KeycloakTokenResponse> login(String username, String password) {
        if (!enabled) {
            return Optional.empty();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("username", username);
        body.add("password", password);

        try {
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<KeycloakTokenResponse> response = restTemplate.postForEntity(tokenUrl, request, KeycloakTokenResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(response.getBody());
            }
        } catch (Exception ex) {
            log.warn("Keycloak Direct Grant login failed for user [{}]: {}", username, ex.getMessage());
        }

        return Optional.empty();
    }

    public Optional<KeycloakTokenResponse> refreshToken(String refreshToken) {
        if (!enabled || refreshToken == null || refreshToken.isBlank()) {
            return Optional.empty();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);

        try {
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<KeycloakTokenResponse> response = restTemplate.postForEntity(tokenUrl, request, KeycloakTokenResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(response.getBody());
            }
        } catch (Exception ex) {
            log.warn("Keycloak refresh_token failed: {}", ex.getMessage());
        }

        return Optional.empty();
    }

    public void logout(String refreshToken) {
        if (!enabled || refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);

        try {
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(logoutUrl, request, String.class);
            log.info("Successfully revoked session in Keycloak");
        } catch (Exception ex) {
            log.warn("Keycloak logout failed: {}", ex.getMessage());
        }
    }
}
