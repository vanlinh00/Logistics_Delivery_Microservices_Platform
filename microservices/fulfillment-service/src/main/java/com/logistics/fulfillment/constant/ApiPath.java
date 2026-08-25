package com.logistics.fulfillment.constant;

/**
 * Enterprise API Path Constants for Delivery Fulfillment & Hub Transit Service.
 * Centralized URI mapping for Proof of Delivery (POD), sorting hub scans, and linehaul tracking.
 */
public final class ApiPath {

    private ApiPath() {
        // Prevent instantiation
    }

    // ==========================================
    // OAUTH & GATEWAY CONTEXT PATHS
    // ==========================================
    public static final String OAUTH_TOKEN = "/oauth/get_token";
    public static final String OAUTH_LOGOUT = "/oauth/logout";
    public static final String OAUTH_LOGOUT_BY_LIST_USER = "/oauth/logout-by-list-user";

    // ==========================================
    // FULFILLMENT & TRANSIT PATHS (/api/v1/fulfillment)
    // ==========================================
    public static final String FULFILLMENT_BASE = "/api/v1/fulfillment";
    public static final String FULFILLMENT_POD = "/api/v1/fulfillment/pod";
    public static final String FULFILLMENT_POD_BY_TRACKING = "/api/v1/fulfillment/pod/{trackingNumber}";
    public static final String FULFILLMENT_HUB_SCAN = "/api/v1/fulfillment/hub-transit/scan";
    public static final String FULFILLMENT_HUB_BY_TRACKING = "/api/v1/fulfillment/hub-transit/{trackingNumber}";

    // Relative sub-paths for controller mappings
    public static final String POD = "/pod";
    public static final String POD_BY_TRACKING = "/pod/{trackingNumber}";
    public static final String HUB_TRANSIT_SCAN = "/hub-transit/scan";
    public static final String HUB_TRANSIT_BY_TRACKING = "/hub-transit/{trackingNumber}";
}
