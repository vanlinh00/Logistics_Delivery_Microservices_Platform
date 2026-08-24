package com.logistics.order.strategy;

import com.logistics.order.singleton.LogisticsConfigRegistry;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Standard Delivery Pricing Strategy (Road courier, standard speed 24-48h).
 * Demonstrates SOLID: Single Responsibility Principle (SRP).
 */
@Component
public class StandardShippingPricingStrategy implements ShippingPricingStrategy {

    @Override
    public PricingContext.DeliveryType getSupportedType() {
        return PricingContext.DeliveryType.STANDARD;
    }

    @Override
    public PriceBreakdown calculatePrice(PricingContext context) {
        LogisticsConfigRegistry config = LogisticsConfigRegistry.getInstance();

        BigDecimal baseFee = config.getDefaultBaseFee();

        double distance = context.getDistanceKm() != null ? Math.max(1.0, context.getDistanceKm()) : 5.0;
        BigDecimal distanceSurcharge = config.getRatePerKm()
                .multiply(BigDecimal.valueOf(distance))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal weightSurcharge = BigDecimal.ZERO;
        if (context.getWeightKg() != null && context.getWeightKg() > 2.0) {
            double extraKg = context.getWeightKg() - 2.0;
            weightSurcharge = config.getRatePerKgExtra()
                    .multiply(BigDecimal.valueOf(extraKg))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal insuranceFee = BigDecimal.ZERO;
        if (context.getDeclaredValue() != null && context.getDeclaredValue().compareTo(BigDecimal.ZERO) > 0) {
            insuranceFee = context.getDeclaredValue()
                    .multiply(config.getInsurancePercentage())
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal codFee = BigDecimal.ZERO;
        if (context.getCodAmount() != null && context.getCodAmount().compareTo(BigDecimal.ZERO) > 0) {
            codFee = config.getFlatCodFee();
        }

        BigDecimal subtotal = baseFee.add(distanceSurcharge).add(weightSurcharge).add(insuranceFee).add(codFee);
        BigDecimal total = subtotal.multiply(config.getFuelSurchargeIndex()).setScale(2, RoundingMode.HALF_UP);

        return PriceBreakdown.builder()
                .baseFee(baseFee)
                .distanceSurcharge(distanceSurcharge)
                .weightSurcharge(weightSurcharge)
                .insuranceFee(insuranceFee)
                .codFee(codFee)
                .specialHandlingFee(BigDecimal.ZERO)
                .totalShippingFee(total)
                .currency("VND")
                .estimatedDeliveryHours("24 - 48 hours")
                .appliedStrategyName("STANDARD_ROAD_DISPATCH")
                .build();
    }
}
