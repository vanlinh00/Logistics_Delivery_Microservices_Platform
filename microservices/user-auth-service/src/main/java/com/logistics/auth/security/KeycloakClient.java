package com.logistics.auth.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class KeycloakClient {

    private final RestTemplate restTemplate;
    private final String serverUrl;
    private final String realm;
    private final String tokenUrl;
    private final String logoutUrl;
    private final String clientId;
    private final String clientSecret;
    private final String adminUsername;
    private final String adminPassword;
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
            @Value("${keycloak.admin-username:admin}") String adminUsername,
            @Value("${keycloak.admin-password:admin}") String adminPassword,
            @Value("${keycloak.enabled:true}") boolean enabled) {
        this.restTemplate = new RestTemplate();
        this.serverUrl = serverUrl;
        this.realm = realm;
        this.tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", serverUrl, realm);
        this.logoutUrl = String.format("%s/realms/%s/protocol/openid-connect/logout", serverUrl, realm);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.enabled = enabled;
    }

    /**
     * Create user in Keycloak realm and assign role
     */
    public Optional<String> createUser(String username, String email, String rawPassword, String fullName, String role) {
        if (!enabled) {
            return Optional.empty();
        }

        Optional<String> adminTokenOpt = getAdminAccessToken();
        if (adminTokenOpt.isEmpty()) {
            log.warn("Cannot create Keycloak user [{}]: unable to obtain admin token", username);
            return Optional.empty();
        }

        String adminToken = adminTokenOpt.get();

        String firstName = "";
        String lastName = "";
        if (fullName != null && !fullName.isBlank()) {
            String[] parts = fullName.trim().split("\\s+", 2);
            if (parts.length > 1) {
                firstName = parts[0];
                lastName = parts[1];
            } else {
                firstName = parts[0];
            }
        }

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("username", username);
        userPayload.put("email", email);
        userPayload.put("firstName", firstName);
        userPayload.put("lastName", lastName);
        userPayload.put("enabled", true);
        userPayload.put("emailVerified", true);

        if (rawPassword != null && !rawPassword.isBlank()) {
            Map<String, Object> credential = new HashMap<>();
            credential.put("type", "password");
            credential.put("value", rawPassword);
            credential.put("temporary", false);
            userPayload.put("credentials", List.of(credential));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(userPayload, headers);
        String usersUrl = String.format("%s/admin/realms/%s/users", serverUrl, realm);

        String userId = null;
        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(usersUrl, request, Void.class);
            if (response.getStatusCode().value() == 201) {
                String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
                if (location != null) {
                    userId = location.substring(location.lastIndexOf('/') + 1);
                    log.info("Successfully created user [{}] in Keycloak with ID [{}]", username, userId);
                }
            }
        } catch (Exception ex) {
            log.warn("Keycloak user creation for [{}] returned error (may already exist): {}", username, ex.getMessage());
        }

        if (userId == null) {
            userId = findUserIdByUsername(username, adminToken).orElse(null);
        }

        if (userId != null && role != null && !role.isBlank()) {
            assignRealmRole(userId, role, adminToken);
        }

        return Optional.ofNullable(userId);
    }

    public Optional<String> findUserIdByUsername(String username, String adminToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            String searchUrl = String.format("%s/admin/realms/%s/users?username=%s&exact=true", serverUrl, realm, username);
            ResponseEntity<List> response = restTemplate.exchange(searchUrl, HttpMethod.GET, request, List.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isEmpty()) {
                Object firstUser = response.getBody().get(0);
                if (firstUser instanceof Map<?, ?> userMap) {
                    return Optional.ofNullable((String) userMap.get("id"));
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to find Keycloak user ID for [{}]: {}", username, ex.getMessage());
        }
        return Optional.empty();
    }

    public void assignRealmRole(String userId, String roleName, String adminToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<Void> getRoleReq = new HttpEntity<>(headers);

            String roleUrl = String.format("%s/admin/realms/%s/roles/%s", serverUrl, realm, roleName);
            ResponseEntity<Map> roleResp = restTemplate.exchange(roleUrl, HttpMethod.GET, getRoleReq, Map.class);
            if (roleResp.getStatusCode().is2xxSuccessful() && roleResp.getBody() != null) {
                Map<String, Object> roleData = roleResp.getBody();

                HttpHeaders postHeaders = new HttpHeaders();
                postHeaders.setContentType(MediaType.APPLICATION_JSON);
                postHeaders.setBearerAuth(adminToken);

                HttpEntity<List<Map<String, Object>>> postReq = new HttpEntity<>(List.of(roleData), postHeaders);
                String mapRoleUrl = String.format("%s/admin/realms/%s/users/%s/role-mappings/realm", serverUrl, realm, userId);
                restTemplate.postForEntity(mapRoleUrl, postReq, Void.class);
                log.info("Assigned realm role [{}] to user ID [{}] in Keycloak", roleName, userId);
            }
        } catch (Exception ex) {
            log.warn("Failed to assign role [{}] to Keycloak user ID [{}]: {}", roleName, userId, ex.getMessage());
        }
    }

    private Optional<String> getAdminAccessToken() {
        if (!enabled) {
            return Optional.empty();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", "admin-cli");
        body.add("username", adminUsername);
        body.add("password", adminPassword);

        try {
            String adminTokenUrl = String.format("%s/realms/master/protocol/openid-connect/token", serverUrl);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<KeycloakTokenResponse> response = restTemplate.postForEntity(adminTokenUrl, request, KeycloakTokenResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.ofNullable(response.getBody().accessToken());
            }
        } catch (Exception ex) {
            log.warn("Failed to obtain Keycloak master admin token: {}", ex.getMessage());
        }
        return Optional.empty();
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
