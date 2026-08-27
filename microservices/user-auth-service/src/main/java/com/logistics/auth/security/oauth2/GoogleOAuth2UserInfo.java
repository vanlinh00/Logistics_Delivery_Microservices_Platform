package com.logistics.auth.security.oauth2;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * 👤 GoogleOAuth2UserInfo:
 * Extracts and encapsulates verified profile attributes received from Google Identity Provider.
 */
@Getter
@AllArgsConstructor
public class GoogleOAuth2UserInfo {

    private final Map<String, Object> attributes;

    public String getId() {
        Object sub = attributes.get("sub");
        return sub != null ? String.valueOf(sub) : null;
    }

    public String getEmail() {
        Object email = attributes.get("email");
        return email != null ? String.valueOf(email).trim().toLowerCase() : null;
    }

    public String getName() {
        Object name = attributes.get("name");
        if (name != null && !String.valueOf(name).isBlank()) {
            return String.valueOf(name).trim();
        }
        Object givenName = attributes.get("given_name");
        Object familyName = attributes.get("family_name");
        if (givenName != null || familyName != null) {
            String gn = givenName != null ? String.valueOf(givenName) : "";
            String fn = familyName != null ? String.valueOf(familyName) : "";
            return (gn + " " + fn).trim();
        }
        return getEmail() != null ? getEmail().split("@")[0] : "Google User";
    }

    public String getPicture() {
        Object picture = attributes.get("picture");
        return picture != null ? String.valueOf(picture) : null;
    }

    public boolean isEmailVerified() {
        Object verified = attributes.get("email_verified");
        if (verified instanceof Boolean b) {
            return b;
        }
        if (verified != null) {
            return "true".equalsIgnoreCase(String.valueOf(verified));
        }
        return true; // Default true if provided by Google OpenID Connect
    }
}
