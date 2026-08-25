package com.logistics.order.constant;

/**
 * Enterprise API Path Constants for Order Management Service.
 * Centralized URI mapping for order lifecycle, price estimation, parallel tier matching, and search.
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
    // ORDER MANAGEMENT PATHS (/api/v1/orders)
    // ==========================================
    public static final String ORDERS_BASE = "/api/v1/orders";
    public static final String ORDER_CREATE = "/api/v1/orders";
    public static final String ORDER_TRACK = "/api/v1/orders/track/{trackingNumber}";
    public static final String ORDER_CUSTOMER = "/api/v1/orders/customer/{customerId}";
    public static final String ORDER_STATUS = "/api/v1/orders/{orderId}/status";
    public static final String ORDER_BY_ID = "/api/v1/orders/{orderId}";
    public static final String ORDER_CALCULATE_PRICE = "/api/v1/orders/calculate-price";
    public static final String ORDER_CALCULATE_TIERS_CONCURRENTLY = "/api/v1/orders/calculate-tiers-concurrently";

    // Relative sub-paths for controller mappings
    public static final String ROOT = "";
    public static final String TRACK_BY_NUMBER = "/track/{trackingNumber}";
    public static final String BY_CUSTOMER_ID = "/customer/{customerId}";
    public static final String STATUS_BY_ID = "/{orderId}/status";
    public static final String BY_ID = "/{orderId}";
    public static final String CALCULATE_PRICE = "/calculate-price";
    public static final String CALCULATE_TIERS_CONCURRENTLY = "/calculate-tiers-concurrently";
}
