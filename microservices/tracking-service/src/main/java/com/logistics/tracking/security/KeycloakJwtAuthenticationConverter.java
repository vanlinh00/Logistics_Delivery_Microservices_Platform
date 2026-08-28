package com.logistics.tracking.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Parses Keycloak Realm & Resource Access client roles and verifies token blacklist status in Redis.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter defaultGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        // 1. Check if token or JTI is blacklisted in Redis
        String jti = jwt.getId();
        if (jti != null && tokenBlacklistService.isJtiBlacklisted(jti)) {
            log.warn("Rejected blacklisted JWT token jti: {}", jti);
            throw new BadCredentialsException("Token has been revoked/blacklisted");
        }

        // 2. Extract standard scope authorities + Keycloak realm and client roles
        Collection<GrantedAuthority> authorities = Stream.concat(
                defaultGrantedAuthoritiesConverter.convert(jwt).stream(),
                extractKeycloakRoles(jwt).stream()
        ).collect(Collectors.toSet());

        String principalClaimName = getPrincipalClaimName(jwt);
        return new JwtAuthenticationToken(jwt, authorities, principalClaimName);
    }

    private String getPrincipalClaimName(Jwt jwt) {
        if (jwt.hasClaim("preferred_username")) {
            return jwt.getClaimAsString("preferred_username");
        }
        return jwt.getClaimAsString(JwtClaimNames.SUB);
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractKeycloakRoles(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Realm Roles
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
            for (Object role : roles) {
                if (role instanceof String roleName) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase()));
                }
            }
        }

        // Client / Resource Access Roles
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            resourceAccess.values().forEach(clientObj -> {
                if (clientObj instanceof Map<?, ?> clientMap && clientMap.get("roles") instanceof List<?> clientRoles) {
                    for (Object role : clientRoles) {
                        if (role instanceof String roleName) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase()));
                        }
                    }
                }
            });
        }

        // Direct roles claim
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            for (String role : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
            }
        }

        // Direct authorities claim
        List<String> directAuthorities = jwt.getClaimAsStringList("authorities");
        if (directAuthorities != null) {
            for (String auth : directAuthorities) {
                authorities.add(new SimpleGrantedAuthority(auth));
            }
        }

        // Permissions / Scopes claim
        List<String> permissions = jwt.getClaimAsStringList("permissions");
        if (permissions != null) {
            for (String perm : permissions) {
                authorities.add(new SimpleGrantedAuthority(perm));
            }
        }

        return authorities;
    }
}
