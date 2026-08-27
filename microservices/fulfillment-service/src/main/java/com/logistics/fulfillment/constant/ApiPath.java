package com.logistics.fulfillment.constant;

/**
 * Enterprise API Path Constants for Delivery Fulfillment & Hub Transit Service.
 */
public final class ApiPath {

    private ApiPath() {
        // Prevent instantiation
    }

    // Base Path
    public static final String FULFILLMENT_BASE = "/api/v1/fulfillment";

    // Relative Sub-paths for Controller mappings
    public static final String POD = "/pod";
    public static final String POD_BY_TRACKING = "/pod/{trackingNumber}";
    public static final String HUB_TRANSIT_SCAN = "/hub-transit/scan";
    public static final String HUB_TRANSIT_BY_TRACKING = "/hub-transit/{trackingNumber}";
}
