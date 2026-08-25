package com.logistics.order.service;

import com.logistics.order.dto.OrderDTOs;
import com.logistics.order.factory.PricingStrategyFactory;
import com.logistics.order.strategy.PriceBreakdown;
import com.logistics.order.strategy.PricingContext;
import com.logistics.order.strategy.ShippingPricingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 🧪 PricingCalculationServiceUnitTest:
 * Tests dynamic strategy resolution and pricing calculation aggregation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PricingCalculationService Unit Tests")
class PricingCalculationServiceUnitTest {

    @Mock
    private PricingStrategyFactory pricingStrategyFactory;

    @Mock
    private ShippingPricingStrategy mockStrategy;

    @InjectMocks
    private PricingCalculationService pricingCalculationService;

    private OrderDTOs.PriceCalculationRequest standardRequest;

    @BeforeEach
    void setUp() {
        standardRequest = OrderDTOs.PriceCalculationRequest.builder()
                .weightKg(1.5)
                .distanceKm(12.0)
                .declaredValue(BigDecimal.valueOf(1000000))
                .codAmount(BigDecimal.valueOf(500000))
                .expressDelivery(false)
                .build();
    }

    @Test
    @DisplayName("Should resolve correct pricing strategy and compute fee breakdown")
    void calculatePrice_WhenStandardShipment_ShouldReturnComputedPrice() {
        // Arrange
        PriceBreakdown expectedBreakdown = PriceBreakdown.builder()
                .baseFee(BigDecimal.valueOf(22000))
                .distanceSurcharge(BigDecimal.valueOf(5000))
                .weightSurcharge(BigDecimal.valueOf(3000))
                .insuranceFee(BigDecimal.valueOf(1000))
                .codFee(BigDecimal.valueOf(0))
                .totalShippingFee(BigDecimal.valueOf(31000))
                .currency("VND")
                .estimatedDeliveryHours("48")
                .build();

        when(pricingStrategyFactory.resolveStrategy(eq(1.5), eq(false), eq(false))).thenReturn(mockStrategy);
        when(mockStrategy.calculatePrice(any(PricingContext.class))).thenReturn(expectedBreakdown);

        // Act
        OrderDTOs.PriceCalculationResponse response = pricingCalculationService.calculatePrice(standardRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getBaseFee()).isEqualByComparingTo(BigDecimal.valueOf(22000));
        assertThat(response.getTotalShippingFee()).isEqualByComparingTo(BigDecimal.valueOf(31000));
        assertThat(response.getEstimatedDeliveryHours()).isEqualTo("48");

        verify(pricingStrategyFactory, times(1)).resolveStrategy(1.5, false, false);
        verify(mockStrategy, times(1)).calculatePrice(any(PricingContext.class));
    }
}
