package com.logistics.auth.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.auth.dto.AuthDTOs.AuthResponse;
import com.logistics.auth.security.oauth2.CustomOAuth2User;
import com.logistics.auth.security.oauth2.GoogleOAuth2UserInfo;
import com.logistics.auth.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.logistics.auth.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.logistics.auth.service.GoogleOAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private GoogleOAuthService googleOAuthService;

    @Mock
    private HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler successHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should return pure JSON AuthResponse on authentication success")
    void testOnAuthenticationSuccess_ReturnsJsonAuthResponse() throws Exception {
        Map<String, Object> attributes = Map.of(
                "sub", "10987654321",
                "email", "developer@google.com",
                "name", "Google Developer",
                "picture", "https://lh3.googleusercontent.com/avatar.jpg"
        );
        GoogleOAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(attributes);
        CustomOAuth2User customOAuth2User = new CustomOAuth2User(
                userInfo, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")), attributes);

        when(authentication.getPrincipal()).thenReturn(customOAuth2User);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("jwt.test.token")
                .refreshToken("jwt.refresh.token")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .userId(UUID.randomUUID())
                .username("developer")
                .email("developer@google.com")
                .role("ROLE_CUSTOMER")
                .fullName("Google Developer")
                .permissions(List.of("orders:read", "orders:create"))
                .build();

        when(googleOAuthService.processGoogleUser(
                eq("10987654321"), eq("developer@google.com"), eq("Google Developer"),
                eq("https://lh3.googleusercontent.com/avatar.jpg"), any(HttpServletRequest.class)))
                .thenReturn(authResponse);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(request, response, authentication);

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentType().contains("application/json"));

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertTrue(json.get("success").asBoolean());
        assertEquals("jwt.test.token", json.get("data").get("accessToken").asText());
        assertEquals("developer@google.com", json.get("data").get("email").asText());
        assertEquals("ROLE_CUSTOMER", json.get("data").get("role").asText());
    }

    @Test
    @DisplayName("Should return 401 JSON error response on authentication failure")
    void testOnAuthenticationFailure_ReturnsJsonErrorResponse() throws Exception {
        OAuth2AuthenticationFailureHandler failureHandler = new OAuth2AuthenticationFailureHandler(cookieAuthorizationRequestRepository);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException exception = new OAuth2AuthenticationException(
                new OAuth2Error("invalid_token"), "Google OAuth2 authorization was denied");

        failureHandler.onAuthenticationFailure(request, response, exception);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().contains("application/json"));

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertFalse(json.get("success").asBoolean());
        assertEquals("401", json.get("code").asText());
        assertTrue(json.get("message").asText().contains("Google OAuth2 authorization was denied"));
    }
}
