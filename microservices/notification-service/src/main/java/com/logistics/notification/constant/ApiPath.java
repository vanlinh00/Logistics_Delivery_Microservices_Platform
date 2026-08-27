package com.logistics.notification.constant;

/**
 * Enterprise API Path Constants for Notification & Multi-Channel Alert Service.
 * Centralized URI mapping for alert dispatching, multi-channel concurrent broadcast, and delivery logs.
 */
public final class ApiPath {

    private ApiPath() {
        // Prevent instantiation
    }

    // ==========================================
    // NOTIFICATION PATHS (/api/v1/notifications)
    // ==========================================
    public static final String NOTIFICATIONS_BASE = "/api/v1/notifications";
    public static final String NOTIFICATION_LOGS = "/api/v1/notifications/logs";
    public static final String NOTIFICATION_SEND_MANUAL = "/api/v1/notifications/send-manual";
    public static final String NOTIFICATION_BROADCAST_PARALLEL = "/api/v1/notifications/broadcast-parallel";

    // Relative sub-paths for controller mappings
    public static final String LOGS = "/logs";
    public static final String LOGS_BY_ID = "/logs/{id}";
    public static final String SEND_MANUAL = "/send-manual";
    public static final String BROADCAST_PARALLEL = "/broadcast-parallel";
}
