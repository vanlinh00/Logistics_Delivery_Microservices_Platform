package com.logistics.fulfillment.constant;

/**
 * Enterprise Kafka Topic Constants for Delivery Fulfillment & Hub Transit.
 */
public final class KafkaTopic {

    private KafkaTopic() {
        // Prevent instantiation
    }

    // ==========================================
    // FULFILLMENT & HUB TRANSIT TOPICS
    // ==========================================
    public static final String FULFILLMENT_DELIVERED = "logistics.fulfillment.delivered";
    public static final String FULFILLMENT_HUB_SCANNED = "logistics.fulfillment.hub-scanned";
    public static final String FULFILLMENT_FAILED = "logistics.fulfillment.failed";
    public static final String FULFILLMENT_TRANSIT_UPDATED = "logistics.fulfillment.transit-updated";

    // ==========================================
    // UPSTREAM & DOWNSTREAM ECOSYSTEM TOPICS
    // ==========================================
    public static final String ORDER_EVENTS = "logistics.orders.events";
    public static final String FLEET_PICKUP_ASSIGNED = "logistics.fleet.pickup-assigned";
    public static final String TRACKING_EVENT_RECORDED = "logistics.tracking.event-recorded";
    public static final String NOTIFICATIONS = "logistics.notifications";
}
