package com.logistics.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.auth.dto.RolePermissionDTOs.*;
import com.logistics.auth.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 🛡️ KeycloakRolePermissionService:
 * Production-grade service implementing Keycloak RBAC, Roles, Composites, and User-Role Mappings.
 */
@Service
@Slf4j
public class KeycloakRolePermissionService {

    private final RestTemplate restTemplate;
    private final String serverUrl;
    private final String realm;
    private final String adminUsername;
    private final String adminPassword;
    private final boolean enabled;
    private final ObjectMapper objectMapper;

    public KeycloakRolePermissionService(
            @Value("${keycloak.server-url:http://localhost:8180}") String serverUrl,
            @Value("${keycloak.realm:logistics-realm}") String realm,
            @Value("${keycloak.admin-username:admin}") String adminUsername,
            @Value("${keycloak.admin-password:admin}") String adminPassword,
            @Value("${keycloak.enabled:true}") boolean enabled,
            ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.serverUrl = serverUrl;
        this.realm = realm;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.enabled = enabled;
        this.objectMapper = objectMapper;
    }

    /**
     * Obtains an Admin access token via the master realm admin-cli.
     */
    private String getAdminToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", "admin-cli");
        body.add("username", adminUsername);
        body.add("password", adminPassword);

        String tokenUrl = String.format("%s/realms/master/protocol/openid-connect/token", serverUrl);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }
        } catch (Exception e) {
            log.error("Failed to obtain Keycloak admin token: {}", e.getMessage());
        }
        throw new IllegalStateException("Unable to authenticate with Keycloak admin-cli");
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getAdminToken());
        return headers;
    }

    // =========================================================================
    // 1. REALM ROLES CRUD
    // =========================================================================

    public RoleRepresentationDTO createRealmRole(CreateRoleRequest request) {
        String url = String.format("%s/admin/realms/%s/roles", serverUrl, realm);

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", request.getName());
        payload.put("description", request.getDescription());
        payload.put("composite", request.getComposite() != null ? request.getComposite() : false);
        if (request.getAttributes() != null) {
            payload.put("attributes", request.getAttributes());
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, createAuthHeaders());
        try {
            restTemplate.postForEntity(url, entity, Void.class);
            log.info("Created realm role: [{}]", request.getName());
            return getRealmRole(request.getName());
        } catch (HttpClientErrorException.Conflict conflict) {
            throw new IllegalArgumentException("Role already exists: " + request.getName());
        } catch (Exception e) {
            log.error("Error creating realm role [{}]: {}", request.getName(), e.getMessage());
            throw new RuntimeException("Failed to create realm role: " + e.getMessage(), e);
        }
    }

    public List<RoleRepresentationDTO> getAllRealmRoles(int first, int max, String search) {
        StringBuilder urlBuilder = new StringBuilder(String.format("%s/admin/realms/%s/roles?first=%d&max=%d", serverUrl, realm, first, max));
        if (search != null && !search.isBlank()) {
            urlBuilder.append("&search=").append(search);
        }

        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders());
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    urlBuilder.toString(),
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<RoleRepresentationDTO> roles = new ArrayList<>();
            if (response.getBody() != null) {
                for (Map<String, Object> map : response.getBody()) {
                    roles.add(mapToDto(map));
                }
            }
            return roles;
        } catch (Exception e) {
            log.error("Failed to list realm roles: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public RoleRepresentationDTO getRealmRole(String roleName) {
        String url = String.format("%s/admin/realms/%s/roles/%s", serverUrl, realm, roleName);
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders());

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getBody() != null) {
                return mapToDto(response.getBody());
            }
            throw new ResourceNotFoundException("Role not found: " + roleName);
        } catch (HttpClientErrorException.NotFound nf) {
            throw new ResourceNotFoundException("Role not found: " + roleName);
        }
    }

    public RoleRepresentationDTO updateRealmRole(String roleName, UpdateRoleRequest request) {
        String url = String.format("%s/admin/realms/%s/roles/%s", serverUrl, realm, roleName);

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", roleName);
        if (request.getDescription() != null) {
            payload.put("description", request.getDescription());
        }
        if (request.getAttributes() != null) {
            payload.put("attributes", request.getAttributes());
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, createAuthHeaders());
        try {
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            log.info("Updated realm role: [{}]", roleName);
            return getRealmRole(roleName);
        } catch (HttpClientErrorException.NotFound nf) {
            throw new ResourceNotFoundException("Role not found: " + roleName);
        }
    }

    public void deleteRealmRole(String roleName) {
        String url = String.format("%s/admin/realms/%s/roles/%s", serverUrl, realm, roleName);
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders());

        try {
            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            log.info("Deleted realm role: [{}]", roleName);
        } catch (HttpClientErrorException.NotFound nf) {
            throw new ResourceNotFoundException("Role not found: " + roleName);
        }
    }

    // =========================================================================
    // 2. CLIENT ROLES
    // =========================================================================

    public RoleRepresentationDTO createClientRole(String clientId, CreateRoleRequest request) {
        String clientUuid = resolveClientUuid(clientId);
        String url = String.format("%s/admin/realms/%s/clients/%s/roles", serverUrl, realm, clientUuid);

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", request.getName());
        payload.put("description", request.getDescription());
        payload.put("clientRole", true);
        if (request.getAttributes() != null) {
            payload.put("attributes", request.getAttributes());
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, createAuthHeaders());
        restTemplate.postForEntity(url, entity, Void.class);
        log.info("Created client role [{}] for client [{}]", request.getName(), clientId);

        return getClientRole(clientId, request.getName());
    }

    public List<RoleRepresentationDTO> getClientRoles(String clientId) {
        String clientUuid = resolveClientUuid(clientId);
        String url = String.format("%s/admin/realms/%s/clients/%s/roles", serverUrl, realm, clientUuid);

        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders());
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        List<RoleRepresentationDTO> roles = new ArrayList<>();
        if (response.getBody() != null) {
            for (Map<String, Object> map : response.getBody()) {
                roles.add(mapToDto(map));
            }
        }
        return roles;
    }

    public RoleRepresentationDTO getClientRole(String clientId, String roleName) {
        String clientUuid = resolveClientUuid(clientId);
        String url = String.format("%s/admin/realms/%s/clients/%s/roles/%s", serverUrl, realm, clientUuid, roleName);

        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders());
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        if (response.getBody() != null) {
            return mapToDto(response.getBody());
        }
        throw new ResourceNotFoundException(String.format("Client role [%s] not found for client [%s]", roleName, clientId));
    }

    public void deleteClientRole(String clientId, String roleName) {
        String clientUuid = resolveClientUuid(clientId);
        String url = String.format("%s/admin/realms/%s/clients/%s/roles/%s", serverUrl, realm, clientUuid, roleName);

        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders());
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
        log.info("Deleted client role [{}] from client [{}]", roleName, clientId);
    }

    // =========================================================================
    // 3. COMPOSITE ROLES
    // =========================================================================

    public void addCompositeRoles(String parentRoleName, List<String> childRoleNames) {
        String url = String.format("%s/admin/realms/%s/roles/%s/composites", serverUrl, realm, parentRoleName);

        List<Map<String, Object>> childRolesPayload = new ArrayList<>();
        for (String childRole : childRoleNames) {
            RoleRepresentationDTO dto = getRealmRole(childRole);
            Map<String, Object> childMap = new HashMap<>();
            childMap.put("id", dto.getId());
            childMap.put("name", dto.getName());
            childRolesPayload.add(childMap);
        }

        HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(childRolesPayload, createAuthHeaders());
        restTemplate.postForEntity(url, entity, Void.class);
        log.info("Added child composites {} to parent role [{}]", childRoleNames, parentRoleName);
    }

    public List<RoleRepresentationDTO> getRoleComposites(String parentRoleName) {
        String url = String.format("%s/admin/realms/%s/roles/%s/composites", serverUrl, realm, parentRoleName);
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders());

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        List<RoleRepresentationDTO> composites = new ArrayList<>();
        if (response.getBody() != null) {
            for (Map<String, Object> map : response.getBody()) {
                composites.add(mapToDto(map));
            }
        }
        return composites;
    }

    public void removeCompositeRoles(String parentRoleName, List<String> childRoleNames) {
        String url = String.format("%s/admin/realms/%s/roles/%s/composites", serverUrl, realm, parentRoleName);

        List<Map<String, Object>> childRolesPayload = new ArrayList<>();
        for (String childRole : childRoleNames) {
            RoleRepresentationDTO dto = getRealmRole(childRole);
            Map<String, Object> childMap = new HashMap<>();
            childMap.put("id", dto.getId());
            childMap.put("name", dto.getName());
            childRolesPayload.add(childMap);
        }

        HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(childRolesPayload, createAuthHeaders());
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
        log.info("Removed child composites {} from parent role [{}]", childRoleNames, parentRoleName);
    }

    // =========================================================================
    // 4. USER ROLE ASSIGNMENTS
    // =========================================================================

    public void assignRoleToUser(String userId, String roleName) {
        RoleRepresentationDTO role = getRealmRole(roleName);
        String url = String.format("%s/admin/realms/%s/users/%s/role-mappings/realm", serverUrl, realm, userId);

        Map<String, Object> roleMap = new HashMap<>();
        roleMap.put("id", role.getId());
        roleMap.put("name", role.getName());

        HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(List.of(roleMap), createAuthHeaders());
        restTemplate.postForEntity(url, entity, Void.class);
        log.info("Assigned role [{}] to user [{}]", roleName, userId);
    }

    public void removeRoleFromUser(String userId, String roleName) {
        RoleRepresentationDTO role = getRealmRole(roleName);
        String url = String.format("%s/admin/realms/%s/users/%s/role-mappings/realm", serverUrl, realm, userId);

        Map<String, Object> roleMap = new HashMap<>();
        roleMap.put("id", role.getId());
        roleMap.put("name", role.getName());

        HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(List.of(roleMap), createAuthHeaders());
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
        log.info("Removed role [{}] from user [{}]", roleName, userId);
    }

    public List<RoleRepresentationDTO> getUserRoles(String userId) {
        String url = String.format("%s/admin/realms/%s/users/%s/role-mappings/realm", serverUrl, realm, userId);
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders());

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        List<RoleRepresentationDTO> roles = new ArrayList<>();
        if (response.getBody() != null) {
            for (Map<String, Object> map : response.getBody()) {
                roles.add(mapToDto(map));
            }
        }
        return roles;
    }

    public List<Map<String, Object>> getRoleUsers(String roleName) {
        String url = String.format("%s/admin/realms/%s/roles/%s/users", serverUrl, realm, roleName);
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders());

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        return response.getBody() != null ? response.getBody() : Collections.emptyList();
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private String resolveClientUuid(String clientId) {
        String url = String.format("%s/admin/realms/%s/clients?clientId=%s", serverUrl, realm, clientId);
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders());

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        if (response.getBody() != null && !response.getBody().isEmpty()) {
            return (String) response.getBody().get(0).get("id");
        }
        throw new ResourceNotFoundException("Keycloak client not found for clientId: " + clientId);
    }

    @SuppressWarnings("unchecked")
    private RoleRepresentationDTO mapToDto(Map<String, Object> map) {
        return RoleRepresentationDTO.builder()
                .id((String) map.get("id"))
                .name((String) map.get("name"))
                .description((String) map.get("description"))
                .composite((Boolean) map.get("composite"))
                .clientRole((Boolean) map.get("clientRole"))
                .containerId((String) map.get("containerId"))
                .attributes((Map<String, List<String>>) map.get("attributes"))
                .build();
    }
}
