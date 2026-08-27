package com.logistics.auth.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.logistics.auth.dto.ApiResponse;
import com.logistics.auth.dto.AuthDTOs.AuthResponse;
import com.logistics.auth.service.GoogleOAuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 🎯 OAuth2AuthenticationSuccessHandler:
 * Handles successful Google OAuth 2.0 authentication by syncing user profile data,
 * generating enterprise JWT tokens, and writing a pure JSON API response (no HTML redirects).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final GoogleOAuthService googleOAuthService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        try {
            Object principal = authentication.getPrincipal();
            GoogleOAuth2UserInfo userInfo;

            if (principal instanceof CustomOAuth2User customOAuth2User) {
                userInfo = customOAuth2User.getGoogleUserInfo();
            } else if (principal instanceof OAuth2User oAuth2User) {
                userInfo = new GoogleOAuth2UserInfo(oAuth2User.getAttributes());
            } else {
                throw new IllegalStateException("Unsupported OAuth2 principal type: " + principal.getClass().getName());
            }

            String sub = userInfo.getId();
            String email = userInfo.getEmail();
            String name = userInfo.getName();
            String picture = userInfo.getPicture();

            log.info("Google OAuth2 authentication success for email={}, sub={}", email, sub);

            AuthResponse authResponse = googleOAuthService.processGoogleUser(sub, email, name, picture, request);
            ApiResponse<AuthResponse> apiResponse = ApiResponse.ok(authResponse, "Google authentication successful");

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
            response.getWriter().flush();
        } catch (Exception ex) {
            log.error("Failed to process Google OAuth2 authentication success: {}", ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());

            ApiResponse<Void> errorResponse = ApiResponse.error("401", "OAuth2 login processing failed: " + ex.getMessage());
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            response.getWriter().flush();
        }
    }
}
