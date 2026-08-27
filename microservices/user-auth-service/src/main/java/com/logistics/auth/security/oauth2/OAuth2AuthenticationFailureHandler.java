package com.logistics.auth.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.logistics.auth.dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * ⚠️ OAuth2AuthenticationFailureHandler:
 * Handles failed OAuth 2.0 authentication attempts by writing a structured JSON error response.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        log.warn("OAuth2 Authentication failed: {}", exception.getMessage());

        cookieAuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String msg = exception.getMessage();
        if (msg != null && msg.contains("invalid_request")) {
            msg = "OAuth2 authentication failed: [invalid_request]. Please initiate login from '/oauth2/authorization/google' or provide valid Google OAuth2 code & state parameters.";
        } else {
            msg = "OAuth2 authentication failed: " + msg;
        }

        ApiResponse<Void> errorResponse = ApiResponse.error("401", msg);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush();
    }
}
