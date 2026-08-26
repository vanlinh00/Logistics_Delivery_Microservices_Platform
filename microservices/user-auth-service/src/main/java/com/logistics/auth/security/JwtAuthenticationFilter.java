package com.logistics.auth.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenBlacklistService blacklistService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            try {
                JsonNode payload = parseUnverifiedPayload(token);
                if (payload != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    String jti = payload.path("jti").asText(null);
                    long exp = payload.path("exp").asLong(0);

                    boolean isBlacklisted = jti != null && blacklistService.isJtiBlacklisted(jti);
                    boolean isExpired = exp > 0 && exp < System.currentTimeMillis() / 1000;

                    if (!isBlacklisted && !isExpired) {
                        String username = payload.has("preferred_username")
                                ? payload.path("preferred_username").asText()
                                : payload.path("sub").asText();

                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

                        // Extract Realm Roles
                        JsonNode realmRoles = payload.path("realm_access").path("roles");
                        if (realmRoles.isArray()) {
                            realmRoles.forEach(role -> {
                                String r = role.asText();
                                authorities.add(new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r));
                            });
                        }

                        // Extract Direct Roles
                        JsonNode directRoles = payload.path("roles");
                        if (directRoles.isArray()) {
                            directRoles.forEach(role -> {
                                String r = role.asText();
                                authorities.add(new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r));
                            });
                        } else if (payload.has("role")) {
                            String r = payload.path("role").asText();
                            authorities.add(new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r));
                        }

                        // Extract Permissions
                        JsonNode permissions = payload.path("permissions");
                        if (permissions.isArray()) {
                            permissions.forEach(perm -> authorities.add(new SimpleGrantedAuthority(perm.asText())));
                        }

                        if (authorities.isEmpty()) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
                        }

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(username, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception ex) {
                log.debug("JWT Token processing failed in user-auth-service: {}", ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private JsonNode parseUnverifiedPayload(String token) {
        if (token == null || token.isBlank()) return null;
        String[] parts = token.split("\\.");
        if (parts.length < 2) return null;
        try {
            return objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
        } catch (Exception e) {
            return null;
        }
    }
}
