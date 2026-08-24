package com.logistics.order.factory;

import com.logistics.order.strategy.PricingContext.DeliveryType;
import com.logistics.order.strategy.ShippingPricingStrategy;
import com.logistics.order.strategy.StandardShippingPricingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory Pattern: Resolves the appropriate ShippingPricingStrategy based on delivery requirements.
 * 
 * SOLID Principles applied:
 * - Open-Closed Principle (OCP): New strategies are auto-discovered without modifying the factory.
 * - Dependency Inversion Principle (DIP): Injects list of abstractions (List<ShippingPricingStrategy>).
 * - Single Responsibility Principle (SRP): Solely responsible for strategy lookup and validation.
 */
@Component
@Slf4j
public class PricingStrategyFactory {

    private final Map<DeliveryType, ShippingPricingStrategy> strategies = new EnumMap<>(DeliveryType.class);

    /**
     * Spring auto-wires all beans implementing ShippingPricingStrategy into this list.
     */
    public PricingStrategyFactory(List<ShippingPricingStrategy> strategyList) {
        for (ShippingPricingStrategy strategy : strategyList) {
            strategies.put(strategy.getSupportedType(), strategy);
            log.info("Registered Pricing Strategy: [{}] for DeliveryType [{}]",
                    strategy.getClass().getSimpleName(), strategy.getSupportedType());
        }
    }

    /**
     * Factory Method to obtain the appropriate pricing strategy.
     *
     * @param type the requested DeliveryType
     * @return the matching ShippingPricingStrategy (or Standard fallback)
     */
    public ShippingPricingStrategy getStrategy(DeliveryType type) {
        if (type == null) {
            return strategies.get(DeliveryType.STANDARD);
        }

        return Optional.ofNullable(strategies.get(type))
                .orElseGet(() -> {
                    log.warn("No strategy found for type: {}, falling back to STANDARD", type);
                    return strategies.get(DeliveryType.STANDARD);
                });
    }

    /**
     * Intelligent Strategy Resolution based on weight and attributes.
     */
    public ShippingPricingStrategy resolveStrategy(Double weightKg, boolean isExpress, boolean isColdChain) {
        if (isColdChain) {
            return getStrategy(DeliveryType.COLD_CHAIN);
        }
        if (weightKg != null && weightKg > 20.0) {
            return getStrategy(DeliveryType.HEAVY_FREIGHT);
        }
        if (isExpress) {
            return getStrategy(DeliveryType.EXPRESS);
        }
        return getStrategy(DeliveryType.STANDARD);
    }
}
