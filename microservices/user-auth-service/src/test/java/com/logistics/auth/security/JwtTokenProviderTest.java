package com.logistics.auth.security;

import com.logistics.auth.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
                86400000L,
                604800000L
        );
    }

    @Test
    @DisplayName("Should generate valid access token and parse claims")
    void testGenerateAndValidateAccessToken() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("test_user")
                .email("test@example.com")
                .role(User.UserRole.ROLE_CUSTOMER)
                .build();

        List<String> permissions = List.of("orders:create", "orders:read");

        String token = jwtTokenProvider.generateAccessToken(user, permissions);
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));

        assertEquals("test_user", jwtTokenProvider.getUsernameFromToken(token));
        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token));
        assertEquals("ROLE_CUSTOMER", jwtTokenProvider.getRoleFromToken(token));
        assertEquals("test@example.com", jwtTokenProvider.getEmailFromToken(token));

        List<String> claimsPermissions = jwtTokenProvider.getPermissionsFromToken(token);
        assertEquals(2, claimsPermissions.size());
        assertTrue(claimsPermissions.contains("orders:create"));
    }

    @Test
    @DisplayName("Should generate and validate refresh token")
    void testGenerateAndValidateRefreshToken() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("refresh_user")
                .email("refresh@example.com")
                .role(User.UserRole.ROLE_CUSTOMER)
                .build();

        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        assertNotNull(refreshToken);
        assertTrue(jwtTokenProvider.validateToken(refreshToken));
        assertEquals("refresh_user", jwtTokenProvider.getUsernameFromToken(refreshToken));
    }
}
