package com.logistics.order.strategy;

import com.logistics.order.singleton.LogisticsConfigRegistry;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Heavy Freight / Pallet Cargo Delivery Pricing Strategy (>20kg or bulky parcels).
 * Incorporates forklift handling, tailgate truck surcharge, and volume calculations.
 */
@Component
public class HeavyFreightPricingStrategy implements ShippingPricingStrategy {

    private static final BigDecimal FREIGHT_BASE_RATE = new BigDecimal("120000"); // 120,000 VND base
    private static final BigDecimal FREIGHT_KM_RATE = new BigDecimal("8500"); // 8,500 VND / km
    private static final BigDecimal FREIGHT_KG_RATE = new BigDecimal("3500"); // 3,500 VND / kg
    private static final BigDecimal TAILGATE_HANDLING_FEE = new BigDecimal("50000"); // 50,000 VND

    @Override
    public PricingContext.DeliveryType getSupportedType() {
        return PricingContext.DeliveryType.HEAVY_FREIGHT;
    }

    @Override
    public PriceBreakdown calculatePrice(PricingContext context) {
        LogisticsConfigRegistry config = LogisticsConfigRegistry.getInstance();

        BigDecimal baseFee = FREIGHT_BASE_RATE;

        double distance = context.getDistanceKm() != null ? Math.max(1.0, context.getDistanceKm()) : 10.0;
        BigDecimal distanceSurcharge = FREIGHT_KM_RATE
                .multiply(BigDecimal.valueOf(distance))
                .setScale(2, RoundingMode.HALF_UP);

        double weight = context.getWeightKg() != null ? context.getWeightKg() : 25.0;
        BigDecimal weightSurcharge = FREIGHT_KG_RATE
                .multiply(BigDecimal.valueOf(weight))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal insuranceFee = BigDecimal.ZERO;
        if (context.getDeclaredValue() != null && context.getDeclaredValue().compareTo(BigDecimal.ZERO) > 0) {
            insuranceFee = context.getDeclaredValue()
                    .multiply(new BigDecimal("0.008")) // 0.8% freight insurance
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal specialHandling = TAILGATE_HANDLING_FEE;
        BigDecimal codFee = context.getCodAmount() != null && context.getCodAmount().compareTo(BigDecimal.ZERO) > 0 ?
                new BigDecimal("10000") : BigDecimal.ZERO;

        BigDecimal total = baseFee.add(distanceSurcharge).add(weightSurcharge).add(insuranceFee).add(specialHandling).add(codFee)
                .multiply(config.getFuelSurchargeIndex())
                .setScale(2, RoundingMode.HALF_UP);

        return PriceBreakdown.builder()
                .baseFee(baseFee)
                .distanceSurcharge(distanceSurcharge)
                .weightSurcharge(weightSurcharge)
                .insuranceFee(insuranceFee)
                .codFee(codFee)
                .specialHandlingFee(specialHandling)
                .totalShippingFee(total)
                .currency("VND")
                .estimatedDeliveryHours("48 - 72 hours (Freight Truck)")
                .appliedStrategyName("HEAVY_FREIGHT_LOGISTICS")
                .build();
    }
}
