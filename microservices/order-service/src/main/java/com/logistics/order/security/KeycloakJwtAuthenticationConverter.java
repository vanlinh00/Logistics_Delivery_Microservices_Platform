package com.logistics.order.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
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
 * into Spring Security GrantedAuthority instances (e.g. ROLE_ADMIN, ROLE_COURIER, ROLE_MERCHANT).
 */
@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter defaultAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
                defaultAuthoritiesConverter.convert(jwt).stream(),
                extractKeycloakRoles(jwt).stream()
        ).collect(Collectors.toSet());

        String principalClaimName = jwt.hasClaim(JwtClaimNames.PREFERRED_USERNAME)
                ? JwtClaimNames.PREFERRED_USERNAME
                : JwtClaimNames.SUB;

        return new JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString(principalClaimName));
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractKeycloakRoles(Jwt jwt) {
        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();

        // 1. Extract Realm-Level Roles: { "realm_access": { "roles": ["ROLE_ADMIN", "ROLE_COURIER"] } }
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            for (String role : roles) {
                String roleName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                grantedAuthorities.add(new SimpleGrantedAuthority(roleName));
            }
        }

        // 2. Extract Client-Level Roles: { "resource_access": { "order-service": { "roles": ["order:create"] } } }
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

        return grantedAuthorities;
    }
}
