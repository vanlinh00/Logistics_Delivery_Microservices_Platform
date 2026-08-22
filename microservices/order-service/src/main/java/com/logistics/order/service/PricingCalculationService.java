package com.logistics.order.service;

import com.logistics.order.dto.OrderDTOs;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PricingCalculationService {

    private static final BigDecimal BASE_RATE = new BigDecimal("25000"); // 25,000 VND base
    private static final BigDecimal RATE_PER_KM = new BigDecimal("4000"); // 4,000 VND/km
    private static final BigDecimal RATE_PER_KG_EXTRA = new BigDecimal("5000"); // 5,000 VND/kg beyond 2kg
    private static final BigDecimal INSURANCE_RATE = new BigDecimal("0.005"); // 0.5% of declared value

    public OrderDTOs.PriceCalculationResponse calculatePrice(OrderDTOs.PriceCalculationRequest request) {
        BigDecimal baseFee = BASE_RATE;
        
        // Distance fee
        double distance = request.getDistanceKm() != null ? Math.max(1.0, request.getDistanceKm()) : 5.0;
        BigDecimal distanceSurcharge = RATE_PER_KM.multiply(BigDecimal.valueOf(distance)).setScale(2, RoundingMode.HALF_UP);

        // Weight fee: items above 2kg have extra charge
        BigDecimal weightSurcharge = BigDecimal.ZERO;
        if (request.getWeightKg() > 2.0) {
            double extraKg = request.getWeightKg() - 2.0;
            weightSurcharge = RATE_PER_KG_EXTRA.multiply(BigDecimal.valueOf(extraKg)).setScale(2, RoundingMode.HALF_UP);
        }

        // Insurance fee
        BigDecimal insuranceFee = BigDecimal.ZERO;
        if (request.getDeclaredValue() != null && request.getDeclaredValue().compareTo(BigDecimal.ZERO) > 0) {
            insuranceFee = request.getDeclaredValue().multiply(INSURANCE_RATE).setScale(2, RoundingMode.HALF_UP);
        }

        // COD processing fee
        BigDecimal codFee = BigDecimal.ZERO;
        if (request.getCodAmount() != null && request.getCodAmount().compareTo(BigDecimal.ZERO) > 0) {
            codFee = new BigDecimal("5000"); // 5k flat COD fee
        }

        BigDecimal total = baseFee.add(distanceSurcharge).add(weightSurcharge).add(insuranceFee).add(codFee);
        if (request.isExpressDelivery()) {
            total = total.multiply(new BigDecimal("1.30")).setScale(2, RoundingMode.HALF_UP); // +30% for express
        }

        return OrderDTOs.PriceCalculationResponse.builder()
                .baseFee(baseFee)
                .distanceSurcharge(distanceSurcharge)
                .weightSurcharge(weightSurcharge)
                .insuranceFee(insuranceFee)
                .codFee(codFee)
                .totalShippingFee(total)
                .currency("VND")
                .estimatedDeliveryHours(request.isExpressDelivery() ? "4 - 8 hours" : "24 - 48 hours")
                .build();
    }
}
