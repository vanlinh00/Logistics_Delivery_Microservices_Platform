package com.logistics.order.controller;

import com.logistics.order.dto.OrderDTOs;
import com.logistics.order.model.Order;
import com.logistics.order.service.OrderService;
import com.logistics.order.service.PricingCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "Endpoints for order lifecycle, price estimation, and status dispatch")
public class OrderController {

    private final OrderService orderService;
    private final PricingCalculationService pricingService;

    @PostMapping
    @Operation(summary = "Create a new shipment order", description = "Validates address, calculates shipping fees, persists order and writes to Transactional Outbox")
    public ResponseEntity<Order> createOrder(@Valid @RequestBody OrderDTOs.CreateOrderRequest request) {
        Order created = orderService.createOrder(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/track/{trackingNumber}")
    @Operation(summary = "Get order details by tracking number")
    public ResponseEntity<Order> getOrderByTracking(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(orderService.getOrderByTrackingNumber(trackingNumber));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List customer shipment orders with pagination")
    public ResponseEntity<Page<Order>> getCustomerOrders(@PathVariable UUID customerId, Pageable pageable) {
        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId, pageable));
    }

    @PutMapping("/{orderId}/status")
    @Operation(summary = "Update order status with distributed lock safety")
    public ResponseEntity<Order> updateStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderDTOs.UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, request));
    }

    @PostMapping("/calculate-price")
    @Operation(summary = "Estimate shipping cost dynamically based on weight, distance, COD and value")
    public ResponseEntity<OrderDTOs.PriceCalculationResponse> calculatePrice(
            @Valid @RequestBody OrderDTOs.PriceCalculationRequest request) {
        return ResponseEntity.ok(pricingService.calculatePrice(request));
    }
}
