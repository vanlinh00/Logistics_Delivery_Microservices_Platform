package com.logistics.gateway.constant;

/**
 * Global Enterprise API Path Constants for API Gateway & Unified Reverse Proxy.
 * Aggregates all microservice route paths across Auth, Orders, Fleet, Fulfillment, Tracking, and Notifications.
 */
public final class ApiPath {

    private ApiPath() {
        // Prevent instantiation
    }

    // ==========================================
    // OAUTH & IAM GLOBAL PATHS
    // ==========================================
    public static final String OAUTH_TOKEN = "/oauth/get_token";
    public static final String OAUTH_LOGOUT = "/oauth/logout";
    public static final String OAUTH_LOGOUT_BY_LIST_USER = "/oauth/logout-by-list-user";
    public static final String OAUTH_INTROSPECT = "/oauth/introspect";
    public static final String OAUTH_USERINFO = "/oauth/userinfo";
    public static final String OAUTH_JWKS = "/oauth/jwks";

    // ==========================================
    // MICROSERVICE PREFIXES / BASE ROUTES
    // ==========================================
    public static final String AUTH_PREFIX = "/api/v1/auth/**";
    public static final String USERS_PREFIX = "/api/v1/users/**";
    public static final String ORDERS_PREFIX = "/api/v1/orders/**";
    public static final String FLEET_PREFIX = "/api/v1/fleet/**";
    public static final String FULFILLMENT_PREFIX = "/api/v1/fulfillment/**";
    public static final String TRACKING_PREFIX = "/api/v1/tracking/**";
    public static final String SEARCH_PREFIX = "/api/v1/search/**";
    public static final String NOTIFICATIONS_PREFIX = "/api/v1/notifications/**";

    // ==========================================
    // DIRECT SERVICE ENDPOINTS
    // ==========================================
    // 1. Auth Service
    public static final String AUTH_LOGIN = "/api/v1/auth/login";
    public static final String AUTH_REGISTER = "/api/v1/auth/register";
    public static final String AUTH_REFRESH = "/api/v1/auth/refresh";
    public static final String AUTH_LOGOUT = "/api/v1/auth/logout";
    public static final String AUTH_VALIDATE = "/api/v1/auth/validate";
    public static final String AUTH_ME = "/api/v1/auth/me";
    public static final String USERS_STATS = "/api/v1/users/stats";

    // 2. Order Service
    public static final String ORDERS = "/api/v1/orders";
    public static final String ORDER_TRACK = "/api/v1/orders/track/{trackingNumber}";
    public static final String ORDER_PRICE_ESTIMATE = "/api/v1/orders/calculate-price";

    // 3. Fleet Service
    public static final String FLEET_DRIVERS = "/api/v1/fleet/drivers";
    public static final String FLEET_PICKUPS = "/api/v1/fleet/pickups";
    public static final String FLEET_FIND_NEAREST = "/api/v1/fleet/find-nearest-driver";

    // 4. Fulfillment Service
    public static final String FULFILLMENT_POD = "/api/v1/fulfillment/pod";
    public static final String FULFILLMENT_HUB_SCAN = "/api/v1/fulfillment/hub-transit/scan";

    // 5. Tracking & Search Service
    public static final String TRACKING_EVENTS = "/api/v1/tracking/events";
    public static final String TRACKING_TIMELINE = "/api/v1/tracking/{trackingNumber}";
    public static final String SEARCH_PARCELS = "/api/v1/search/parcels";
    public static final String SEARCH_AUTOCOMPLETE = "/api/v1/search/autocomplete";

    // 6. Notification Service
    public static final String NOTIFICATIONS_LOGS = "/api/v1/notifications/logs";
    public static final String NOTIFICATIONS_SEND = "/api/v1/notifications/send-manual";
    public static final String NOTIFICATIONS_BROADCAST = "/api/v1/notifications/broadcast-parallel";
}
