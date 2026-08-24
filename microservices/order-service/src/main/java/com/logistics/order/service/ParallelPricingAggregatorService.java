package com.logistics.order.service;

import com.logistics.order.config.AsyncThreadPoolConfig;
import com.logistics.order.dto.OrderDTOs;
import com.logistics.order.factory.PricingStrategyFactory;
import com.logistics.order.strategy.PriceBreakdown;
import com.logistics.order.strategy.PricingContext;
import com.logistics.order.strategy.ShippingPricingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Multi-Threaded Pricing Aggregator Service.
 * 
 * Uses CompletableFuture with a dedicated thread pool to calculate multi-tier
 * shipping quotes (Standard, Express, Freight, Cold Chain) concurrently in parallel.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParallelPricingAggregatorService {

    private final PricingStrategyFactory pricingStrategyFactory;

    @Qualifier(AsyncThreadPoolConfig.PRICING_EXECUTOR)
    private final Executor pricingTaskExecutor;

    /**
     * Executes parallel price calculation across multiple delivery tiers simultaneously.
     *
     * @param request the price calculation request
     * @return Aggregated list of quotes calculated in parallel threads
     */
    public List<PriceBreakdown> calculateAllTiersConcurrently(OrderDTOs.PriceCalculationRequest request) {
        log.info("Starting multi-threaded pricing calculation across all delivery tiers");

        PricingContext standardCtx = PricingContext.builder()
                .weightKg(request.getWeightKg())
                .distanceKm(request.getDistanceKm())
                .declaredValue(request.getDeclaredValue())
                .codAmount(request.getCodAmount())
                .deliveryType(PricingContext.DeliveryType.STANDARD)
                .build();

        PricingContext expressCtx = PricingContext.builder()
                .weightKg(request.getWeightKg())
                .distanceKm(request.getDistanceKm())
                .declaredValue(request.getDeclaredValue())
                .codAmount(request.getCodAmount())
                .deliveryType(PricingContext.DeliveryType.EXPRESS)
                .build();

        PricingContext freightCtx = PricingContext.builder()
                .weightKg(request.getWeightKg())
                .distanceKm(request.getDistanceKm())
                .declaredValue(request.getDeclaredValue())
                .codAmount(request.getCodAmount())
                .deliveryType(PricingContext.DeliveryType.HEAVY_FREIGHT)
                .build();

        PricingContext coldCtx = PricingContext.builder()
                .weightKg(request.getWeightKg())
                .distanceKm(request.getDistanceKm())
                .declaredValue(request.getDeclaredValue())
                .codAmount(request.getCodAmount())
                .deliveryType(PricingContext.DeliveryType.COLD_CHAIN)
                .build();

        // Spawn parallel worker threads using CompletableFuture
        CompletableFuture<PriceBreakdown> standardFuture = CompletableFuture.supplyAsync(() -> {
            log.debug("[Thread: {}] Calculating STANDARD tier", Thread.currentThread().getName());
            ShippingPricingStrategy strategy = pricingStrategyFactory.getStrategy(PricingContext.DeliveryType.STANDARD);
            return strategy.calculatePrice(standardCtx);
        }, pricingTaskExecutor);

        CompletableFuture<PriceBreakdown> expressFuture = CompletableFuture.supplyAsync(() -> {
            log.debug("[Thread: {}] Calculating EXPRESS tier", Thread.currentThread().getName());
            ShippingPricingStrategy strategy = pricingStrategyFactory.getStrategy(PricingContext.DeliveryType.EXPRESS);
            return strategy.calculatePrice(expressCtx);
        }, pricingTaskExecutor);

        CompletableFuture<PriceBreakdown> freightFuture = CompletableFuture.supplyAsync(() -> {
            log.debug("[Thread: {}] Calculating HEAVY_FREIGHT tier", Thread.currentThread().getName());
            ShippingPricingStrategy strategy = pricingStrategyFactory.getStrategy(PricingContext.DeliveryType.HEAVY_FREIGHT);
            return strategy.calculatePrice(freightCtx);
        }, pricingTaskExecutor);

        CompletableFuture<PriceBreakdown> coldFuture = CompletableFuture.supplyAsync(() -> {
            log.debug("[Thread: {}] Calculating COLD_CHAIN tier", Thread.currentThread().getName());
            ShippingPricingStrategy strategy = pricingStrategyFactory.getStrategy(PricingContext.DeliveryType.COLD_CHAIN);
            return strategy.calculatePrice(coldCtx);
        }, pricingTaskExecutor);

        // Wait for all parallel worker threads to complete (Barrier synchronization)
        CompletableFuture.allOf(standardFuture, expressFuture, freightFuture, coldFuture).join();

        List<PriceBreakdown> results = new ArrayList<>();
        try {
            results.add(standardFuture.get());
            results.add(expressFuture.get());
            results.add(freightFuture.get());
            results.add(coldFuture.get());
        } catch (Exception e) {
            log.error("Multi-threading pricing calculation failed: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }

        log.info("Multi-threaded pricing calculation completed. Produced {} tier quotes.", results.size());
        return results;
    }
}
