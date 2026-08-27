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
    // FLEET & PICKUP DISPATCH PATHS (/api/v1/fleet)
    // ==========================================
    public static final String FLEET_BASE = "/api/v1/fleet";
    public static final String DRIVERS = "/drivers";
    public static final String PICKUPS = "/pickups";
    public static final String ASSIGN_PICKUP = "/pickups/{taskId}/assign/{driverId}";
    public static final String FIND_NEAREST_DRIVER = "/find-nearest-driver";
    public static final String RANK_DRIVERS_CONCURRENTLY = "/rank-drivers-concurrently";
}
