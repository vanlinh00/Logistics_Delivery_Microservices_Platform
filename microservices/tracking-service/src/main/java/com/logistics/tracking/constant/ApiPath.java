package com.logistics.tracking.constant;

/**
 * Enterprise API Path Constants for Real-time Tracking & Elasticsearch Search Service.
 * Centralized URI mapping for GPS telemetry, event streams, fuzzy parcel search, and autocomplete.
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
    // TRACKING & TELEMETRY PATHS (/api/v1/tracking)
    // ==========================================
    public static final String TRACKING_BASE = "/api/v1/tracking";
    public static final String TRACKING_EVENTS = "/api/v1/tracking/events";
    public static final String TRACKING_HISTORY_BY_NUMBER = "/api/v1/tracking/{trackingNumber}";
    public static final String TRACKING_AGGREGATE_CONCURRENTLY = "/api/v1/tracking/aggregate-concurrently/{trackingNumber}";

    // Relative tracking sub-paths
    public static final String EVENTS = "/events";
    public static final String BY_TRACKING_NUMBER = "/{trackingNumber}";
    public static final String AGGREGATE_CONCURRENTLY = "/aggregate-concurrently/{trackingNumber}";

    // ==========================================
    // ELASTICSEARCH SEARCH PATHS (/api/v1/search)
    // ==========================================
    public static final String SEARCH_BASE = "/api/v1/search";
    public static final String SEARCH_PARCELS = "/api/v1/search/parcels";
    public static final String SEARCH_AUTOCOMPLETE = "/api/v1/search/autocomplete";
    public static final String SEARCH_GEO_NEARBY = "/api/v1/search/geo-nearby";
    public static final String SEARCH_INDEX = "/api/v1/search/index";
    public static final String SEARCH_SEED = "/api/v1/search/seed";
    public static final String SEARCH_CLUSTER_HEALTH = "/api/v1/search/cluster-health";

    // Relative search sub-paths
    public static final String PARCELS = "/parcels";
    public static final String AUTOCOMPLETE = "/autocomplete";
    public static final String GEO_NEARBY = "/geo-nearby";
    public static final String INDEX = "/index";
    public static final String SEED = "/seed";
    public static final String CLUSTER_HEALTH = "/cluster-health";
}
