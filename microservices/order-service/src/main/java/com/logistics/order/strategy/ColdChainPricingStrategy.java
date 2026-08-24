package com.logistics.order.strategy;

import com.logistics.order.singleton.LogisticsConfigRegistry;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cold-Chain Refrigerated Logistics Pricing Strategy.
 * Includes refrigerated container cooling costs, dry ice packaging surcharge, and temperature telemetry SLA.
 */
@Component
public class ColdChainPricingStrategy implements ShippingPricingStrategy {

    private static final BigDecimal COLD_BASE_RATE = new BigDecimal("85000"); // 85,000 VND
    private static final BigDecimal REFRIGERATION_SENSITIVITY_FEE = new BigDecimal("40000"); // 40,000 VND thermal packaging

    @Override
    public PricingContext.DeliveryType getSupportedType() {
        return PricingContext.DeliveryType.COLD_CHAIN;
    }

    @Override
    public PriceBreakdown calculatePrice(PricingContext context) {
        LogisticsConfigRegistry config = LogisticsConfigRegistry.getInstance();

        BigDecimal baseFee = COLD_BASE_RATE;

        double distance = context.getDistanceKm() != null ? Math.max(1.0, context.getDistanceKm()) : 5.0;
        BigDecimal distanceSurcharge = new BigDecimal("6000")
                .multiply(BigDecimal.valueOf(distance))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal weightSurcharge = BigDecimal.ZERO;
        if (context.getWeightKg() != null && context.getWeightKg() > 1.0) {
            double extraKg = context.getWeightKg() - 1.0;
            weightSurcharge = new BigDecimal("7000")
                    .multiply(BigDecimal.valueOf(extraKg))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal specialHandling = REFRIGERATION_SENSITIVITY_FEE;

        BigDecimal insuranceFee = BigDecimal.ZERO;
        if (context.getDeclaredValue() != null && context.getDeclaredValue().compareTo(BigDecimal.ZERO) > 0) {
            insuranceFee = context.getDeclaredValue()
                    .multiply(new BigDecimal("0.01")) // 1% for temperature-sensitive perishables
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal codFee = context.getCodAmount() != null && context.getCodAmount().compareTo(BigDecimal.ZERO) > 0 ?
                config.getFlatCodFee() : BigDecimal.ZERO;

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
                .estimatedDeliveryHours("6 - 12 hours (Refrigerated Transit)")
                .appliedStrategyName("COLD_CHAIN_TEMPERATURE_CONTROLLED")
                .build();
    }
}
