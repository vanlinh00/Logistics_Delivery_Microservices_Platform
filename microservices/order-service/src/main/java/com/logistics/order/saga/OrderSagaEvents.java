package com.logistics.order.saga;

import java.util.UUID;

public class OrderSagaEvents {

    public enum SagaStep {
        ORDER_CREATED,
        FLEET_PICKUP_ASSIGNED,
        FLEET_PICKUP_FAILED,
        PAYMENT_RESERVED,
        PAYMENT_FAILED,
        FULFILLMENT_ACCEPTED,
        FULFILLMENT_REJECTED,
        SAGA_COMPLETED,
        SAGA_COMPENSATED_FAILED
    }

    public record OrderCreatedEvent(
            UUID orderId,
            String trackingNumber,
            UUID customerId,
            Double totalAmount,
            String senderAddress,
            Double senderLat,
            Double senderLng,
            String recipientAddress,
            Double recipientLat,
            Double recipientLng,
            Double weightKg
    ) {}

    public record FleetPickupCommand(
            UUID orderId,
            String trackingNumber,
            String senderAddress,
            Double lat,
            Double lng
    ) {}

    public record FleetPickupResultEvent(
            UUID orderId,
            String trackingNumber,
            boolean success,
            String assignedDriverId,
            String failureReason
    ) {}

    public record PaymentReserveCommand(
            UUID orderId,
            UUID customerId,
            Double amount
    ) {}

    public record PaymentResultEvent(
            UUID orderId,
            boolean success,
            String transactionId,
            String failureReason
    ) {}

    public record CompensateOrderCommand(
            UUID orderId,
            String trackingNumber,
            String reason
    ) {}

    public record CompensateFleetCommand(
            UUID orderId,
            String trackingNumber,
            String reason
    ) {}
}
