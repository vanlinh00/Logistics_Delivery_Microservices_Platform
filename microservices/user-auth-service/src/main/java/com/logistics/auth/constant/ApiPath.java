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
    // AUTHENTICATION & SESSION PATHS (/api/v1/auth)
    // ==========================================
    public static final String AUTH_BASE = "/api/v1/auth";
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
    public static final String BY_ID = "/{userId}";
    public static final String PASSWORD_CHANGE = "/password/change";
    public static final String ADMIN_ALL = "/admin/all";
    public static final String ADMIN_STATUS = "/admin/{userId}/status";
    public static final String STATS = "/stats";

    // ==========================================
    // COURIER FLEET & KYC PATHS (/api/v1/users/couriers)
    // ==========================================
    public static final String COURIERS_BASE = "/api/v1/users/couriers";
    public static final String KYC = "/kyc";
    public static final String SHIFT = "/shift";
    public static final String PROFILE_BY_ID = "/profile/{userId}";
    public static final String HUB_ACTIVE = "/hub/{hubId}/active";

    // ==========================================
    // MERCHANT PROFILE PATHS (/api/v1/users/merchants)
    // ==========================================
    public static final String MERCHANTS_BASE = "/api/v1/users/merchants";
    public static final String PROFILE = "/profile";
}
