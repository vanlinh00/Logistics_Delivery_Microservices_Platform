package com.logistics.auth.constant;

/**
 * Enterprise API Path Constants for User & Authentication Service.
 * Centralized URI mapping for OAuth2/OIDC, User Management, Courier KYC, and Merchant Profiles.
 */
public final class ApiPath {

    private ApiPath() {
        // Prevent instantiation
    }

    // ==========================================
    // OAUTH & IAM SPECIFIC PATHS
    // ==========================================
    public static final String OAUTH_TOKEN = "/oauth/get_token";
    public static final String OAUTH_LOGOUT = "/oauth/logout";
    public static final String OAUTH_LOGOUT_BY_LIST_USER = "/oauth/logout-by-list-user";
    public static final String OAUTH_INTROSPECT = "/oauth/introspect";
    public static final String OAUTH_USERINFO = "/oauth/userinfo";
    public static final String OAUTH_JWKS = "/oauth/jwks";

    // ==========================================
    // AUTHENTICATION & SESSION PATHS (/api/v1/auth)
    // ==========================================
    public static final String AUTH_BASE = "/api/v1/auth";
    public static final String AUTH_LOGIN = "/api/v1/auth/login";
    public static final String AUTH_REGISTER = "/api/v1/auth/register";
    public static final String AUTH_REFRESH = "/api/v1/auth/refresh";
    public static final String AUTH_LOGOUT = "/api/v1/auth/logout";
    public static final String AUTH_VALIDATE = "/api/v1/auth/validate";
    public static final String AUTH_ME = "/api/v1/auth/me";
    public static final String AUTH_MFA_SETUP = "/api/v1/auth/mfa/setup";
    public static final String AUTH_MFA_VERIFY = "/api/v1/auth/mfa/verify";

    // Relative sub-paths for controller mappings
    public static final String LOGIN = "/login";
    public static final String REGISTER = "/register";
    public static final String REFRESH = "/refresh";
    public static final String LOGOUT = "/logout";
    public static final String VALIDATE = "/validate";
    public static final String ME = "/me";
    public static final String MFA_SETUP = "/mfa/setup";
    public static final String MFA_VERIFY = "/mfa/verify";

    // ==========================================
    // USER ADMINISTRATION PATHS (/api/v1/users)
    // ==========================================
    public static final String USERS_BASE = "/api/v1/users";
    public static final String USER_BY_ID = "/api/v1/users/{userId}";
    public static final String USER_CHANGE_PASSWORD = "/api/v1/users/password/change";
    public static final String USER_ADMIN_ALL = "/api/v1/users/admin/all";
    public static final String USER_ADMIN_STATUS = "/api/v1/users/admin/{userId}/status";
    public static final String USER_STATS = "/api/v1/users/stats";

    // Relative user sub-paths
    public static final String BY_ID = "/{userId}";
    public static final String PASSWORD_CHANGE = "/password/change";
    public static final String ADMIN_ALL = "/admin/all";
    public static final String ADMIN_STATUS = "/admin/{userId}/status";
    public static final String STATS = "/stats";

    // ==========================================
    // COURIER FLEET & KYC PATHS (/api/v1/users/couriers)
    // ==========================================
    public static final String COURIERS_BASE = "/api/v1/users/couriers";
    public static final String COURIER_KYC = "/api/v1/users/couriers/kyc";
    public static final String COURIER_SHIFT = "/api/v1/users/couriers/shift";
    public static final String COURIER_PROFILE_BY_ID = "/api/v1/users/couriers/profile/{userId}";
    public static final String COURIER_HUB_ACTIVE = "/api/v1/users/couriers/hub/{hubId}/active";

    // Relative courier sub-paths
    public static final String KYC = "/kyc";
    public static final String SHIFT = "/shift";
    public static final String PROFILE_BY_ID = "/profile/{userId}";
    public static final String HUB_ACTIVE = "/hub/{hubId}/active";

    // ==========================================
    // MERCHANT PROFILE PATHS (/api/v1/users/merchants)
    // ==========================================
    public static final String MERCHANTS_BASE = "/api/v1/users/merchants";
    public static final String MERCHANT_PROFILE = "/api/v1/users/merchants/profile";
    public static final String MERCHANT_PROFILE_BY_ID = "/api/v1/users/merchants/profile/{userId}";

    // Relative merchant sub-paths
    public static final String PROFILE = "/profile";
}
