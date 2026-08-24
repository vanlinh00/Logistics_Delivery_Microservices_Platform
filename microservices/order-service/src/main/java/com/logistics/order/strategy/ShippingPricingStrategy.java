package com.logistics.order.strategy;

/**
 * Strategy Pattern Interface for shipping pricing calculation.
 * 
 * SOLID Principles applied:
 * - Open-Closed Principle (OCP): New shipping tiers (e.g. Drone, Overnight, Same-Day) can be added
 *   without modifying existing calculation code.
 * - Liskov Substitution Principle (LSP): Any concrete strategy can be used interchangeably by callers.
 * - Interface Segregation Principle (ISP): Focused solely on pricing calculation.
 */
public interface ShippingPricingStrategy {

    /**
     * Identifies which delivery type this strategy handles.
     */
    PricingContext.DeliveryType getSupportedType();

    /**
     * Calculates the full price breakdown for the given pricing context.
     *
     * @param context the calculation parameters (weight, distance, COD, value, etc.)
     * @return PriceBreakdown containing itemized fees and total
     */
    PriceBreakdown calculatePrice(PricingContext context);
}
