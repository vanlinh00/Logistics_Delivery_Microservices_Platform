package com.logistics.fleet.constant;

/**
 * Enterprise API Path Constants for Pickup & Fleet Dispatch Service.
 * Centralized URI mapping for driver management, pickup assignments, and nearest driver matching.
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
    // FLEET & PICKUP DISPATCH PATHS (/api/v1/fleet)
    // ==========================================
    public static final String FLEET_BASE = "/api/v1/fleet";
    public static final String FLEET_DRIVERS = "/api/v1/fleet/drivers";
    public static final String FLEET_PICKUPS = "/api/v1/fleet/pickups";
    public static final String FLEET_ASSIGN_PICKUP = "/api/v1/fleet/pickups/{taskId}/assign/{driverId}";
    public static final String FLEET_FIND_NEAREST_DRIVER = "/api/v1/fleet/find-nearest-driver";
    public static final String FLEET_RANK_DRIVERS_CONCURRENTLY = "/api/v1/fleet/rank-drivers-concurrently";

    // Relative sub-paths for controller mappings
    public static final String DRIVERS = "/drivers";
    public static final String PICKUPS = "/pickups";
    public static final String ASSIGN_PICKUP = "/pickups/{taskId}/assign/{driverId}";
    public static final String FIND_NEAREST_DRIVER = "/find-nearest-driver";
    public static final String RANK_DRIVERS_CONCURRENTLY = "/rank-drivers-concurrently";
}
