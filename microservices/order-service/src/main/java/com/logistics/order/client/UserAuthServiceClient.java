package com.logistics.order.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.order.dto.ApiResponse;
import com.logistics.order.dto.UserProfileDTO;
import com.logistics.order.exception.ResourceNotFoundException;
import com.logistics.order.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * 🌐 UserAuthServiceClient:
 * Dispatches load-balanced HTTP requests to USER-AUTH-SERVICE via Eureka Service Discovery and Spring Cloud LoadBalancer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAuthServiceClient {

    // Virtual service hostname registered with Eureka
    private static final String USER_SERVICE_BASE_URL = "http://USER-AUTH-SERVICE";

    private final RestTemplate restTemplate;

    /**
     * Fetches user profile from user-auth-service using Eureka virtual service resolution.
     *
     * @param userId Target user unique ID
     * @return UserProfileDTO retrieved from user-auth-service
     */
    public UserProfileDTO fetchUserProfile(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        String url = String.format("%s/api/v1/users/%s", USER_SERVICE_BASE_URL, userId);
        log.info("Sending load-balanced request to [{}]", url);

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ApiResponse<UserProfileDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<UserProfileDTO>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().getData() != null) {
                UserProfileDTO userProfile = response.getBody().getData();
                log.info("Successfully fetched user profile for userId [{}]: {}", userId, userProfile.getUsername());
                return userProfile;
            }

            throw new ResourceNotFoundException("User not found in user-auth-service for id: " + userId);

        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("User ID [{}] not found in user-auth-service: {}", userId, ex.getMessage());
            throw new ResourceNotFoundException("User ID not found: " + userId);

        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            log.error("Authentication/Authorization error contacting user-auth-service: {}", ex.getMessage());
            throw new ServiceUnavailableException("Unauthorized inter-service communication with user-auth-service", ex);

        } catch (HttpServerErrorException ex) {
            log.error("user-auth-service responded with HTTP {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ServiceUnavailableException("user-auth-service encountered an internal error", ex);

        } catch (ResourceAccessException ex) {
            log.error("Network I/O timeout or Eureka instance unavailable: {}", ex.getMessage());
            throw new ServiceUnavailableException("user-auth-service is temporarily unreachable via Eureka", ex);

        } catch (Exception ex) {
            log.error("Unexpected error during inter-service call to user-auth-service: {}", ex.getMessage(), ex);
            throw new ServiceUnavailableException("Failed to communicate with user-auth-service", ex);
        }
    }

    /**
     * Overloaded method to fetch user profile by String ID.
     */
    public UserProfileDTO fetchUserProfile(String userId) {
        try {
            return fetchUserProfile(UUID.fromString(userId));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID format for userId: {}", userId);
            throw new ResourceNotFoundException("Invalid user ID format: " + userId);
        }
    }

    /**
     * Propagates Bearer JWT Token from incoming request SecurityContext to downstream service.
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            headers.setBearerAuth(jwt.getTokenValue());
        }

        return headers;
    }
}
