package com.logistics.order.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.order.model.Order;
import com.logistics.order.model.OrderStatus;
import com.logistics.order.repository.OrderRepository;
import com.logistics.order.saga.OrderSagaEvents.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Saga Orchestrator for Order Processing & Logistics Dispatching.
 *
 * Pattern: Orchestration-based Saga with Compensating Transactions.
 *
 * Workflow Steps:
 * 1. Step 1: Order Created -> Send FleetPickupCommand to 'logistics.fleet.assign-pickup'
 * 2. Step 2: Receive FleetPickupResultEvent:
 *    - If SUCCESS -> Send PaymentReserveCommand to 'logistics.payment.reserve'
 *    - If FAILURE -> Compensate & Cancel Order (OrderStatus.CANCELLED)
 * 3. Step 3: Receive PaymentResultEvent:
 *    - If SUCCESS -> Confirm Order (OrderStatus.CONFIRMED) & complete Saga
 *    - If FAILURE -> Compensate: Cancel Fleet Pickup + Cancel Order with Refund
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSagaOrchestrator {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC_FLEET_COMMAND = "logistics.fleet.commands";
    private static final String TOPIC_PAYMENT_COMMAND = "logistics.payment.commands";
    private static final String TOPIC_NOTIFICATIONS = "logistics.notifications";

    /**
     * Entry point of Saga: triggered after Order is created in OrderService
     */
    public void startSaga(Order order) {
        log.info("🚀 [SAGA START] Initiating Order Creation Saga for Tracking #{}", order.getTrackingNumber());

        FleetPickupCommand command = new FleetPickupCommand(
                order.getId(),
                order.getTrackingNumber(),
                order.getSenderAddress(),
                order.getSenderLatitude(),
                order.getSenderLongitude()
        );

        sendCommand(TOPIC_FLEET_COMMAND, order.getId().toString(), command);
    }

    /**
     * Step 2 Callback: Fleet Service response handler
     */
    @KafkaListener(topics = "logistics.fleet.pickup-results", groupId = "order-saga-orchestrator-group")
    @Transactional
    public void handleFleetPickupResult(String message) {
        try {
            FleetPickupResultEvent event = objectMapper.readValue(message, FleetPickupResultEvent.class);
            log.info("📥 [SAGA STEP 2] Received FleetPickupResult for Order {}: success={}", event.orderId(), event.success());

            Order order = orderRepository.findById(event.orderId()).orElse(null);
            if (order == null) {
                log.warn("Order {} not found in saga orchestrator", event.orderId());
                return;
            }

            if (event.success()) {
                // Step 2 Succeeded: Update assigned driver and trigger Payment Reservation
                order.setAssignedDriverId(event.assignedDriverId());
                order.setStatus(OrderStatus.SCHEDULED_FOR_PICKUP);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);

                // Move forward to Step 3 (Payment)
                PaymentReserveCommand paymentCommand = new PaymentReserveCommand(
                        order.getId(),
                        order.getCustomerId(),
                        order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0
                );
                sendCommand(TOPIC_PAYMENT_COMMAND, order.getId().toString(), paymentCommand);
            } else {
                // Step 2 Failed: Execute Compensation for Order
                log.warn("❌ [SAGA COMPENSATION] Fleet pickup failed for Order {}: {}", event.orderId(), event.failureReason());
                compensateOrder(order, "Fleet Pickup Dispatch Failed: " + event.failureReason());
            }
        } catch (Exception e) {
            log.error("Error processing FleetPickupResultEvent in Saga: {}", e.getMessage());
        }
    }

    /**
     * Step 3 Callback: Payment Service response handler
     */
    @KafkaListener(topics = "logistics.payment.results", groupId = "order-saga-orchestrator-group")
    @Transactional
    public void handlePaymentResult(String message) {
        try {
            PaymentResultEvent event = objectMapper.readValue(message, PaymentResultEvent.class);
            log.info("📥 [SAGA STEP 3] Received PaymentResult for Order {}: success={}", event.orderId(), event.success());

            Order order = orderRepository.findById(event.orderId()).orElse(null);
            if (order == null) return;

            if (event.success()) {
                // All steps succeeded -> SAGA COMPLETED
                order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                log.info("🎉 [SAGA SUCCESS] Order {} fully confirmed and dispatched to courier network!", order.getTrackingNumber());
            } else {
                // Payment Failed -> Compensate Fleet (Cancel Driver) + Cancel Order
                log.warn("❌ [SAGA COMPENSATION] Payment failed for Order {}. Rolling back driver allocation...", event.orderId());
                compensateFleet(order, "Payment failed: " + event.failureReason());
                compensateOrder(order, "Payment failed: " + event.failureReason());
            }
        } catch (Exception e) {
            log.error("Error processing PaymentResultEvent in Saga: {}", e.getMessage());
        }
    }

    private void compensateOrder(Order order, String reason) {
        order.setStatus(OrderStatus.CANCELLED);
        order.setSpecialInstructions((order.getSpecialInstructions() != null ? order.getSpecialInstructions() + " | " : "") + "[CANCELLED: " + reason + "]");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Notify user about cancellation
        sendCommand(TOPIC_NOTIFICATIONS, order.getId().toString(), new CompensateOrderCommand(order.getId(), order.getTrackingNumber(), reason));
    }

    private void compensateFleet(Order order, String reason) {
        CompensateFleetCommand cancelFleetCmd = new CompensateFleetCommand(
                order.getId(),
                order.getTrackingNumber(),
                reason
        );
        sendCommand(TOPIC_FLEET_COMMAND, order.getId().toString(), cancelFleetCmd);
    }

    private void sendCommand(String topic, String key, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, json);
            log.info("📤 [SAGA DISPATCH] Sent command to topic [{}] key [{}]", topic, key);
        } catch (Exception e) {
            log.error("Failed to serialize and send Saga command to [{}]: {}", topic, e.getMessage());
        }
    }
}
