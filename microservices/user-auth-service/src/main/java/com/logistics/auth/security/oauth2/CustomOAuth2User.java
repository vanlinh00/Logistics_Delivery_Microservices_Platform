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

    private final GoogleOAuth2UserInfo userInfo;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;
    private final OidcIdToken idToken;
    private final OidcUserInfo oidcUserInfo;

    public CustomOAuth2User(GoogleOAuth2UserInfo userInfo,
                            Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes,
                            OidcIdToken idToken,
                            OidcUserInfo oidcUserInfo) {
        this.userInfo = userInfo;
        this.authorities = authorities != null ? authorities : Collections.emptyList();
        this.attributes = attributes;
        this.idToken = idToken;
        this.oidcUserInfo = oidcUserInfo;
    }

    public CustomOAuth2User(GoogleOAuth2UserInfo userInfo,
                            Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes) {
        this(userInfo, authorities, attributes, null, null);
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
        return userInfo.getId() != null ? userInfo.getId() : userInfo.getEmail();
    }

    public String getEmail() {
        return userInfo.getEmail();
    }

    public String getFullName() {
        return userInfo.getName();
    }

    public String getPicture() {
        return userInfo.getPicture();
    }

    public String getGoogleId() {
        return userInfo.getId();
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
