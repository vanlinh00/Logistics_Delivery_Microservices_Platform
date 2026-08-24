package com.logistics.order.service;

import com.logistics.order.dto.OrderDTOs;
import com.logistics.order.factory.PricingStrategyFactory;
import com.logistics.order.strategy.PriceBreakdown;
import com.logistics.order.strategy.PricingContext;
import com.logistics.order.strategy.ShippingPricingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service acting as a High-Level Coordinator for price calculations.
 * 
 * SOLID Principles applied:
 * - Single Responsibility Principle (SRP): Coordinates requests and maps DTOs.
 * - Open/Closed Principle (OCP): Pricing algorithms are decoupled into individual Strategy implementations.
 * - Dependency Inversion Principle (DIP): Injects PricingStrategyFactory instead of hardcoding pricing math.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PricingCalculationService {

    private final PricingStrategyFactory pricingStrategyFactory;

    public OrderDTOs.PriceCalculationResponse calculatePrice(OrderDTOs.PriceCalculationRequest request) {
        // 1. Build decoupled context (SRP)
        PricingContext context = PricingContext.builder()
                .weightKg(request.getWeightKg())
                .distanceKm(request.getDistanceKm())
                .declaredValue(request.getDeclaredValue())
                .codAmount(request.getCodAmount())
                .deliveryType(request.isExpressDelivery() ? PricingContext.DeliveryType.EXPRESS : PricingContext.DeliveryType.STANDARD)
                .build();

        // 2. Resolve strategy dynamically using Factory Pattern
        ShippingPricingStrategy strategy = pricingStrategyFactory.resolveStrategy(
                request.getWeightKg(),
                request.isExpressDelivery(),
                false
        );

        log.info("Calculating shipping price using Strategy: [{}]", strategy.getClass().getSimpleName());

        // 3. Execute Strategy Pattern calculation
        PriceBreakdown breakdown = strategy.calculatePrice(context);

        // 4. Map to response DTO
        return OrderDTOs.PriceCalculationResponse.builder()
                .baseFee(breakdown.getBaseFee())
                .distanceSurcharge(breakdown.getDistanceSurcharge())
                .weightSurcharge(breakdown.getWeightSurcharge())
                .insuranceFee(breakdown.getInsuranceFee())
                .codFee(breakdown.getCodFee())
                .totalShippingFee(breakdown.getTotalShippingFee())
                .currency(breakdown.getCurrency())
                .estimatedDeliveryHours(breakdown.getEstimatedDeliveryHours())
                .build();
    }
}

