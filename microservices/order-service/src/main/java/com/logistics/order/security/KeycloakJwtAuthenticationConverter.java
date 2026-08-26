package com.logistics.order.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 🛡️ KeycloakJwtAuthenticationConverter:
 * Converts Keycloak JWT Claims (realm_access.roles & resource_access.client.roles)
 * into Spring Security GrantedAuthority instances (e.g. ROLE_ADMIN, ROLE_COURIER, ROLE_MERCHANT),
 * and checks Redis token blacklist via TokenBlacklistService.
 */
@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final TokenBlacklistService tokenBlacklistService;
    private final JwtGrantedAuthoritiesConverter defaultAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    public KeycloakJwtAuthenticationConverter(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        // Validate against Redis Token Blacklist via jti
        String jti = jwt.getId();
        if (jti != null && tokenBlacklistService.isJtiBlacklisted(jti)) {
            throw new BadCredentialsException("Token has been revoked");
        }

        Collection<GrantedAuthority> authorities = Stream.concat(
                defaultAuthoritiesConverter.convert(jwt).stream(),
                extractKeycloakRoles(jwt).stream()
        ).collect(Collectors.toSet());

        String principalClaimName = jwt.hasClaim("preferred_username")
                ? "preferred_username"
                : JwtClaimNames.SUB;

        return new JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString(principalClaimName));
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractKeycloakRoles(Jwt jwt) {
        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();

        // 1. Extract Realm-Level Roles: { "realm_access": { "roles": ["ROLE_ADMIN", "ROLE_COURIER", "ROLE_MERCHANT"] } }
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            for (String role : roles) {
                String roleName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                grantedAuthorities.add(new SimpleGrantedAuthority(roleName));
            }
        }

        // 2. Extract Direct Roles from user-auth-service (e.g. "roles": ["CUSTOMER"], "role": "CUSTOMER")
        List<String> directRoles = jwt.getClaimAsStringList("roles");
        if (directRoles != null) {
            for (String role : directRoles) {
                String roleName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                grantedAuthorities.add(new SimpleGrantedAuthority(roleName));
            }
        } else {
            String singleRole = jwt.getClaimAsString("role");
            if (singleRole != null && !singleRole.isBlank()) {
                String roleName = singleRole.startsWith("ROLE_") ? singleRole : "ROLE_" + singleRole;
                grantedAuthorities.add(new SimpleGrantedAuthority(roleName));
            }
        }

        // 3. Extract authorities if present
        List<String> directAuthorities = jwt.getClaimAsStringList("authorities");
        if (directAuthorities != null) {
            for (String auth : directAuthorities) {
                String authName = auth.startsWith("ROLE_") ? auth : "ROLE_" + auth;
                grantedAuthorities.add(new SimpleGrantedAuthority(authName));
            }
        }

        // 4. Extract Client-Level Roles: { "resource_access": { "order-service": { "roles": ["orders:create"] } } }
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            resourceAccess.values().forEach(resource -> {
                if (resource instanceof Map) {
                    Map<String, Object> resourceMap = (Map<String, Object>) resource;
                    if (resourceMap.containsKey("roles")) {
                        List<String> clientRoles = (List<String>) resourceMap.get("roles");
                        for (String role : clientRoles) {
                            String roleName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                            grantedAuthorities.add(new SimpleGrantedAuthority(roleName));
                        }
                    }
                }
            });
        }

        // 5. Extract Fine-Grained Permissions (e.g. "orders:create", "orders:cancel", "orders:read")
        List<String> permissions = jwt.getClaimAsStringList("permissions");
        if (permissions != null) {
            permissions.forEach(perm -> grantedAuthorities.add(new SimpleGrantedAuthority(perm)));
        }

        return grantedAuthorities;
    }
}
