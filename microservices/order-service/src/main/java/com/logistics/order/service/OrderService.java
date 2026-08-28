package com.logistics.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.order.constant.MessageCode;
import com.logistics.order.dto.OrderDTOs;
import com.logistics.order.model.Order;
import com.logistics.order.model.OrderItem;
import com.logistics.order.model.OrderOutbox;
import com.logistics.order.model.OrderStatus;
import com.logistics.order.repository.OrderOutboxRepository;
import com.logistics.order.repository.OrderRepository;
import com.logistics.order.exception.ResourceNotFoundException;
import com.logistics.order.exception.InvalidStatusTransitionException;
import com.logistics.order.exception.BusinessRuleViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderOutboxRepository outboxRepository;
    private final PricingCalculationService pricingService;
    private final AddressValidationService addressService;
    private final MessageService messageService;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final com.logistics.order.saga.OrderSagaOrchestrator sagaOrchestrator;
    private final com.logistics.order.client.UserAuthServiceClient userAuthServiceClient;

    private static final String TRACKING_PREFIX = "VNX";
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public Order createOrder(OrderDTOs.CreateOrderRequest request) {
        // Optional inter-service customer validation via Eureka & LoadBalancer
        if (request.getCustomerId() != null) {
            try {
                var customerProfile = userAuthServiceClient.fetchUserProfile(request.getCustomerId());
                log.info("Validated customer via Eureka inter-service call: {} ({})", customerProfile.getUsername(), customerProfile.getEmail());
            } catch (Exception ex) {
                log.warn("Customer validation inter-service call warning for {}: {}", request.getCustomerId(), ex.getMessage());
            }
        }

        // Validate sender and recipient addresses
        var senderValidation = addressService.validateAddress(request.getSenderAddress(), request.getSenderPhone());
        if (!senderValidation.isValid()) {
            throw new IllegalArgumentException(messageService.getMessage(MessageCode.SENDER_ADDRESS_INVALID, senderValidation.getMessage()));
        }

        var recipientValidation = addressService.validateAddress(request.getRecipientAddress(), request.getRecipientPhone());
        if (!recipientValidation.isValid()) {
            throw new IllegalArgumentException(messageService.getMessage(MessageCode.RECIPIENT_ADDRESS_INVALID, recipientValidation.getMessage()));
        }

        // Calculate dynamic pricing
        var priceQuote = pricingService.calculatePrice(OrderDTOs.PriceCalculationRequest.builder()
                .weightKg(request.getTotalWeightKg())
                .declaredValue(request.getDeclaredValue())
                .codAmount(request.getCodAmount())
                .distanceKm(8.5)
                .build());

        String trackingNumber = TRACKING_PREFIX + System.currentTimeMillis() % 100000000 + String.format("%04d", random.nextInt(10000));

        Order order = Order.builder()
                .trackingNumber(trackingNumber)
                .customerId(request.getCustomerId())
                .status(OrderStatus.CREATED)
                .senderName(request.getSenderName())
                .senderPhone(request.getSenderPhone())
                .senderAddress(senderValidation.getFormattedAddress())
                .senderLatitude(senderValidation.getLatitude())
                .senderLongitude(senderValidation.getLongitude())
                .recipientName(request.getRecipientName())
                .recipientPhone(request.getRecipientPhone())
                .recipientAddress(recipientValidation.getFormattedAddress())
                .recipientLatitude(recipientValidation.getLatitude())
                .recipientLongitude(recipientValidation.getLongitude())
                .totalWeightKg(request.getTotalWeightKg())
                .totalVolumeM3(request.getTotalVolumeM3())
                .baseShippingFee(priceQuote.getBaseFee())
                .weightSurcharge(priceQuote.getWeightSurcharge())
                .insuranceFee(priceQuote.getInsuranceFee())
                .codAmount(request.getCodAmount() != null ? request.getCodAmount() : BigDecimal.ZERO)
                .totalAmount(priceQuote.getTotalShippingFee())
                .specialInstructions(request.getSpecialInstructions())
                .build();

        if (request.getItems() != null) {
            for (var itemReq : request.getItems()) {
                order.addItem(OrderItem.builder()
                        .itemName(itemReq.getItemName())
                        .quantity(itemReq.getQuantity())
                        .weightKg(itemReq.getWeightKg())
                        .declaredValue(itemReq.getDeclaredValue())
                        .category(itemReq.getCategory())
                        .build());
            }
        }

        Order savedOrder = orderRepository.save(order);

        // Transactional Outbox write
        try {
            OrderOutbox outbox = OrderOutbox.builder()
                    .aggregateType("ORDER")
                    .aggregateId(savedOrder.getId() != null ? savedOrder.getId().toString() : savedOrder.getTrackingNumber())
                    .eventType("ORDER_CREATED")
                    .payload(objectMapper.writeValueAsString(savedOrder))
                    .processed(false)
                    .build();
            outboxRepository.save(outbox);
        } catch (Exception e) {
            log.error("Failed to serialize order outbox payload", e);
        }

        // Trigger Saga Orchestration workflow across fleet & payment services
        try {
            sagaOrchestrator.startSaga(savedOrder);
        } catch (Exception e) {
            log.warn("Failed to initiate Saga workflow asynchronously: {}", e.getMessage());
        }

        return savedOrder;
    }

    @Transactional
    public Order updateOrderStatus(UUID orderId, OrderDTOs.UpdateOrderStatusRequest request) {
        String lockKey = "lock:order:" + orderId;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // Acquire distributed lock for 5s to prevent concurrent state corruption
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(messageService.getMessage(MessageCode.ORDER_LOCK_FAILED));
            }

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage(MessageCode.ORDER_NOT_FOUND, orderId)));

            OrderStatus nextStatus;
            try {
                nextStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidStatusTransitionException(messageService.getMessage(MessageCode.INVALID_STATUS_TRANSITION, request.getStatus()));
            }
            order.setStatus(nextStatus);

            if (request.getAssignedDriverId() != null) {
                order.setAssignedDriverId(request.getAssignedDriverId());
            }
            if (request.getCurrentHubId() != null) {
                order.setCurrentHubId(request.getCurrentHubId());
            }

            Order updated = orderRepository.save(order);

            // Write status update to Outbox
            try {
                OrderOutbox outbox = OrderOutbox.builder()
                        .aggregateType("ORDER")
                        .aggregateId(updated.getId() != null ? updated.getId().toString() : orderId.toString())
                        .eventType("ORDER_STATUS_UPDATED")
                        .payload(objectMapper.writeValueAsString(updated))
                        .processed(false)
                        .build();
                outboxRepository.save(outbox);
            } catch (Exception e) {
                log.error("Failed to serialize order update outbox", e);
            }

            return updated;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(messageService.getMessage(MessageCode.ORDER_INTERRUPTED), e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public Order getOrderByTrackingNumber(String trackingNumber) {
        return orderRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage(MessageCode.ORDER_NOT_FOUND, trackingNumber)));
    }

    public Page<Order> getOrdersByCustomer(UUID customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable);
    }
}
