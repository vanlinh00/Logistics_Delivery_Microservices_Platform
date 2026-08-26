package com.logistics.order.security;

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
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtProvider.validateToken(token)
                        && SecurityContextHolder.getContext().getAuthentication() == null) {
                    String username = jwtProvider.getUsernameFromToken(token);
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    jwtProvider.getAllRolesFromToken(token).forEach(role ->
                            authorities.add(new SimpleGrantedAuthority(
                                    role.startsWith("ROLE_") ? role : "ROLE_" + role)));
                    jwtProvider.getPermissionsFromToken(token).forEach(permission ->
                            authorities.add(new SimpleGrantedAuthority(permission)));

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ex) {
                log.debug("JWT Token validation failed: {}", ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
