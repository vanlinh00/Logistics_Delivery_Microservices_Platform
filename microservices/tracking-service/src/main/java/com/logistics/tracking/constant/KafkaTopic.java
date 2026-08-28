package com.logistics.tracking.constant;

/**
 * Centralized Enterprise Kafka Topic Constants for Tracking & Search Service.
 */
public final class KafkaTopic {

    private KafkaTopic() {
        // Prevent instantiation
    }

    // ==========================================
    // TRACKING & GPS TELEMETRY TOPICS
    // ==========================================
    public static final String TRACKING_EVENT_RECORDED = "logistics.tracking.event-recorded";
    public static final String TRACKING_GPS_PING = "logistics.tracking.gps-ping";

    // ==========================================
    // ORDER & SAGA LIFECYCLE TOPICS
    // ==========================================
    public static final String ORDER_CREATED = "logistics.orders.order_created";
    public static final String ORDER_EVENTS = "logistics.orders.events";

    // ==========================================
    // FULFILLMENT & TRANSIT TOPICS
    // ==========================================
    public static final String FULFILLMENT_DELIVERED = "logistics.fulfillment.delivered";
    public static final String FULFILLMENT_FAILED = "logistics.fulfillment.failed";
    public static final String FULFILLMENT_HUB_SCANNED = "logistics.fulfillment.hub-scanned";
    public static final String FULFILLMENT_TRANSIT_UPDATED = "logistics.fulfillment.transit-updated";

    // ==========================================
    // FLEET & DISPATCH TOPICS
    // ==========================================
    public static final String FLEET_PICKUP_ASSIGNED = "logistics.fleet.pickup-assigned";
    public static final String FLEET_PICKUP_RESULTS = "logistics.fleet.pickup-results";

    // ==========================================
    // NOTIFICATION TOPICS
    // ==========================================
    public static final String NOTIFICATIONS = "logistics.notifications";
}
