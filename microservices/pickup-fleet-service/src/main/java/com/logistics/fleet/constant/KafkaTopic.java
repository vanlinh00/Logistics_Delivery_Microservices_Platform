package com.logistics.fleet.constant;

/**
 * Enterprise Kafka Topic Constants for Fleet and Logistics Platform.
 */
public final class KafkaTopic {

    private KafkaTopic() {
        // Prevent instantiation
    }

    // ==========================================
    // FLEET & PICKUP DISPATCH TOPICS
    // ==========================================
    public static final String FLEET_PICKUP_ASSIGNED = "logistics.fleet.pickup-assigned";
    public static final String FLEET_COMMANDS = "logistics.fleet.commands";
    public static final String FLEET_PICKUP_RESULTS = "logistics.fleet.pickup-results";

    // ==========================================
    // ORDER, PAYMENT & TRACKING TOPICS
    // ==========================================
    public static final String ORDER_EVENTS = "logistics.orders.events";
    public static final String PAYMENT_COMMANDS = "logistics.payment.commands";
    public static final String PAYMENT_RESULTS = "logistics.payment.results";
    public static final String TRACKING_EVENT_RECORDED = "logistics.tracking.event-recorded";
    public static final String NOTIFICATIONS = "logistics.notifications";
}
