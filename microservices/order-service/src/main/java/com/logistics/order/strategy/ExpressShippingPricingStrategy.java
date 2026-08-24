package com.logistics.order.strategy;

import com.logistics.order.singleton.LogisticsConfigRegistry;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Express / Priority Delivery Pricing Strategy (Fast air/express courier, 4-8h).
 * Applies priority dispatch surcharge and guaranteed delivery SLA.
 */
@Component
public class ExpressShippingPricingStrategy implements ShippingPricingStrategy {

    private static final BigDecimal EXPRESS_BASE_RATE = new BigDecimal("45000"); // 45,000 VND
    private static final BigDecimal EXPRESS_KM_RATE = new BigDecimal("6500"); // 6,500 VND / km
    private static final BigDecimal PRIORITY_MULTIPLIER = new BigDecimal("1.35"); // +35% priority SLA

    @Override
    public PricingContext.DeliveryType getSupportedType() {
        return PricingContext.DeliveryType.EXPRESS;
    }

    @Override
    public PriceBreakdown calculatePrice(PricingContext context) {
        LogisticsConfigRegistry config = LogisticsConfigRegistry.getInstance();

        BigDecimal baseFee = EXPRESS_BASE_RATE;

        double distance = context.getDistanceKm() != null ? Math.max(1.0, context.getDistanceKm()) : 5.0;
        BigDecimal distanceSurcharge = EXPRESS_KM_RATE
                .multiply(BigDecimal.valueOf(distance))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal weightSurcharge = BigDecimal.ZERO;
        if (context.getWeightKg() != null && context.getWeightKg() > 1.0) {
            double extraKg = context.getWeightKg() - 1.0;
            weightSurcharge = new BigDecimal("8000") // 8k per extra kg for express
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
        BigDecimal total = subtotal.multiply(PRIORITY_MULTIPLIER)
                .multiply(config.getFuelSurchargeIndex())
                .setScale(2, RoundingMode.HALF_UP);

        return PriceBreakdown.builder()
                .baseFee(baseFee)
                .distanceSurcharge(distanceSurcharge)
                .weightSurcharge(weightSurcharge)
                .insuranceFee(insuranceFee)
                .codFee(codFee)
                .specialHandlingFee(BigDecimal.ZERO)
                .totalShippingFee(total)
                .currency("VND")
                .estimatedDeliveryHours("4 - 8 hours (Same Day)")
                .appliedStrategyName("EXPRESS_PRIORITY_DISPATCH")
                .build();
    }
}
