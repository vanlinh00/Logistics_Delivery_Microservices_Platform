package com.logistics.order.singleton;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton Pattern: Thread-Safe Global Logistics Configuration & Surcharge Registry.
 * 
 * Implemented using the Bill Pugh (Initialization-on-Demand Holder) idiom:
 * - Thread-safe without explicit synchronization overhead
 * - Lazy-loaded only when getInstance() is first called
 * - Guarantees a single instance per JVM across all services
 */
public class LogisticsConfigRegistry {

    @Getter
    @Setter
    private BigDecimal defaultBaseFee = new BigDecimal("25000"); // 25,000 VND

    @Getter
    @Setter
    private BigDecimal ratePerKm = new BigDecimal("4000"); // 4,000 VND / km

    @Getter
    @Setter
    private BigDecimal ratePerKgExtra = new BigDecimal("5000"); // 5,000 VND / kg beyond 2kg

    @Getter
    @Setter
    private BigDecimal insurancePercentage = new BigDecimal("0.005"); // 0.5%

    @Getter
    @Setter
    private BigDecimal flatCodFee = new BigDecimal("5000"); // 5,000 VND

    @Getter
    @Setter
    private BigDecimal fuelSurchargeIndex = new BigDecimal("1.05"); // +5% dynamic fuel adjustment

    private final ConcurrentHashMap<String, String> dynamicMetadata = new ConcurrentHashMap<>();

    // Private constructor prevents direct instantiation from other classes
    private LogisticsConfigRegistry() {
        dynamicMetadata.put("PLATFORM_VERSION", "v3.4.2-PROD");
        dynamicMetadata.put("REGION", "VN-SOUTHEAST");
    }

    /**
     * Bill Pugh Singleton Holder
     */
    private static class SingletonHolder {
        private static final LogisticsConfigRegistry INSTANCE = new LogisticsConfigRegistry();
    }

    /**
     * Global access point for the singleton instance.
     */
    public static LogisticsConfigRegistry getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void setMetadata(String key, String value) {
        dynamicMetadata.put(key, value);
    }

    public String getMetadata(String key) {
        return dynamicMetadata.get(key);
    }
}
