package com.logistics.order.strategy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Encapsulates calculation inputs to decouple strategy implementation from HTTP DTOs.
 * Adheres to Single Responsibility Principle (SRP).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingContext {
    private Double weightKg;
    private Double distanceKm;
    private BigDecimal declaredValue;
    private BigDecimal codAmount;
    private boolean fragile;
    private boolean coldChainRequired;
    private DeliveryType deliveryType;

    public enum DeliveryType {
        STANDARD,
        EXPRESS,
        HEAVY_FREIGHT,
        COLD_CHAIN
    }
}
