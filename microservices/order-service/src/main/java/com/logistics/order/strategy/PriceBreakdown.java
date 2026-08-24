package com.logistics.order.strategy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Output calculation result breakdown.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceBreakdown {
    private BigDecimal baseFee;
    private BigDecimal distanceSurcharge;
    private BigDecimal weightSurcharge;
    private BigDecimal insuranceFee;
    private BigDecimal codFee;
    private BigDecimal specialHandlingFee;
    private BigDecimal totalShippingFee;
    private String currency;
    private String estimatedDeliveryHours;
    private String appliedStrategyName;
}
