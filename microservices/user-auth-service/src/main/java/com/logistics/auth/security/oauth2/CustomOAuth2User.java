package com.logistics.auth.security.oauth2;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * 🛡️ CustomOAuth2User:
 * Bridges Google OAuth2 and OIDC user details to Spring Security authentication context.
 */
@Getter
public class CustomOAuth2User implements OAuth2User, OidcUser {

    private final GoogleOAuth2UserInfo googleUserInfo;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;
    private final OidcIdToken idToken;
    private final OidcUserInfo oidcUserInfo;

    public CustomOAuth2User(GoogleOAuth2UserInfo googleUserInfo,
                            Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes,
                            OidcIdToken idToken,
                            OidcUserInfo oidcUserInfo) {
        this.googleUserInfo = googleUserInfo;
        this.authorities = authorities != null ? authorities : Collections.emptyList();
        this.attributes = attributes;
        this.idToken = idToken;
        this.oidcUserInfo = oidcUserInfo;
    }

    public CustomOAuth2User(GoogleOAuth2UserInfo googleUserInfo,
                            Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes) {
        this(googleUserInfo, authorities, attributes, null, null);
    }

    public GoogleOAuth2UserInfo getGoogleUserInfo() {
        return googleUserInfo;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return googleUserInfo.getId() != null ? googleUserInfo.getId() : googleUserInfo.getEmail();
    }

    public String getEmail() {
        return googleUserInfo.getEmail();
    }

    public String getFullName() {
        return googleUserInfo.getName();
    }

    public String getPicture() {
        return googleUserInfo.getPicture();
    }

    public String getGoogleId() {
        return googleUserInfo.getId();
    }

    @Override
    public Map<String, Object> getClaims() {
        return attributes;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return oidcUserInfo;
    }

    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }
}
