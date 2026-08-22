package com.logistics.order.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class OrderDTOs {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateOrderRequest {
        @NotNull(message = "Customer ID is required")
        private UUID customerId;

        @NotBlank(message = "Sender name is required")
        private String senderName;
        @NotBlank(message = "Sender phone is required")
        private String senderPhone;
        @NotBlank(message = "Sender address is required")
        private String senderAddress;
        private Double senderLatitude;
        private Double senderLongitude;

        @NotBlank(message = "Recipient name is required")
        private String recipientName;
        @NotBlank(message = "Recipient phone is required")
        private String recipientPhone;
        @NotBlank(message = "Recipient address is required")
        private String recipientAddress;
        private Double recipientLatitude;
        private Double recipientLongitude;

        @NotNull(message = "Total weight is required")
        @Positive(message = "Weight must be positive")
        private Double totalWeightKg;
        private Double totalVolumeM3;

        private BigDecimal declaredValue;
        private BigDecimal codAmount;
        private String specialInstructions;

        @NotEmpty(message = "At least one item is required")
        private List<CreateOrderItemRequest> items;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateOrderItemRequest {
        @NotBlank(message = "Item name is required")
        private String itemName;
        @NotNull @Min(1)
        private Integer quantity;
        @NotNull @Positive
        private Double weightKg;
        private BigDecimal declaredValue;
        private String category;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateOrderStatusRequest {
        @NotNull
        private String status;
        private String reason;
        private String assignedDriverId;
        private String currentHubId;
        private String note;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriceCalculationRequest {
        @NotNull @Positive
        private Double weightKg;
        private Double distanceKm;
        private BigDecimal declaredValue;
        private BigDecimal codAmount;
        private boolean expressDelivery;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriceCalculationResponse {
        private BigDecimal baseFee;
        private BigDecimal distanceSurcharge;
        private BigDecimal weightSurcharge;
        private BigDecimal insuranceFee;
        private BigDecimal codFee;
        private BigDecimal totalShippingFee;
        private String currency;
        private String estimatedDeliveryHours;
    }
}
