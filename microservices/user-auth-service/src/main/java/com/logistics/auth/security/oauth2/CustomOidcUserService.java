package com.logistics.auth.security.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 🔒 CustomOidcUserService:
 * Loads and validates verified user profiles from OpenID Connect (OIDC) ID token & UserInfo endpoints.
 */
@Service
@Slf4j
public class CustomOidcUserService extends OidcUserService {

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        try {
            GoogleOAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(oidcUser.getAttributes());
            if (userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
                throw new OAuth2AuthenticationException(new OAuth2Error("invalid_email", "Email not found from Google OIDC provider", null));
            }
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
            return new CustomOAuth2User(userInfo, authorities, oidcUser.getAttributes(), oidcUser.getIdToken(), oidcUser.getUserInfo());
        } catch (OAuth2AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error processing OIDC user: {}", ex.getMessage(), ex);
            throw new OAuth2AuthenticationException(new OAuth2Error("oidc_processing_error", ex.getMessage(), null));
        }
    }
}
