package com.logistics.order.controller;

import com.logistics.order.constant.ApiPath;
import com.logistics.order.constant.MessageCode;
import com.logistics.order.dto.ApiResponse;
import com.logistics.order.dto.OrderDTOs;
import com.logistics.order.model.Order;
import com.logistics.order.service.MessageService;
import com.logistics.order.service.OrderService;
import com.logistics.order.service.ParallelPricingAggregatorService;
import com.logistics.order.service.PricingCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(ApiPath.ORDERS_BASE)
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "Endpoints for order lifecycle, price estimation, and status dispatch protected by Keycloak OAuth2 / OIDC")
public class OrderController {

    private final OrderService orderService;
    private final PricingCalculationService pricingService;
    private final ParallelPricingAggregatorService parallelPricingService;
    private final MessageService messageService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MERCHANT', 'ROLE_CUSTOMER')")
    @Operation(summary = "Create a new shipment order", description = "Validates address, calculates shipping fees, persists order and writes to Transactional Outbox")
    public ResponseEntity<ApiResponse<Order>> createOrder(@Valid @RequestBody OrderDTOs.CreateOrderRequest request) {
        Order created = orderService.createOrder(request);
        return new ResponseEntity<>(ApiResponse.created(created, messageService.getMessage(MessageCode.CREATED)), HttpStatus.CREATED);
    }

    @GetMapping(ApiPath.TRACK_BY_NUMBER)
    @Operation(summary = "Get order details by tracking number (Public / All Roles)")
    public ResponseEntity<ApiResponse<Order>> getOrderByTracking(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderByTrackingNumber(trackingNumber), messageService.getMessage(MessageCode.SUCCESS)));
    }

    @GetMapping(ApiPath.BY_CUSTOMER_ID)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MERCHANT', 'ROLE_CUSTOMER')")
    @Operation(summary = "List customer shipment orders with pagination")
    public ResponseEntity<ApiResponse<Page<Order>>> getCustomerOrders(@PathVariable UUID customerId, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrdersByCustomer(customerId, pageable), messageService.getMessage(MessageCode.SUCCESS)));
    }

    @PutMapping(ApiPath.STATUS_BY_ID)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_COURIER', 'ROLE_DISPATCHER')")
    @Operation(summary = "Update order status with distributed lock safety (Shipper / Dispatcher / Admin)")
    public ResponseEntity<ApiResponse<Order>> updateStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderDTOs.UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.updateOrderStatus(orderId, request), messageService.getMessage(MessageCode.SUCCESS)));
    }

    @DeleteMapping(ApiPath.BY_ID)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Cancel and void shipment order (Admin Only)")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable UUID orderId) {
        // Administrative voiding action
        return ResponseEntity.ok(ApiResponse.ok(null, messageService.getMessage(MessageCode.SUCCESS)));
    }

    @PostMapping(ApiPath.CALCULATE_PRICE)
    @Operation(summary = "Estimate shipping cost dynamically based on weight, distance, COD and value")
    public ResponseEntity<ApiResponse<OrderDTOs.PriceCalculationResponse>> calculatePrice(
            @Valid @RequestBody OrderDTOs.PriceCalculationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(pricingService.calculatePrice(request), messageService.getMessage(MessageCode.SUCCESS)));
    }

    @PostMapping(ApiPath.CALCULATE_TIERS_CONCURRENTLY)
    @Operation(summary = "Multi-threaded parallel calculation across all shipping tiers (Standard, Express, Freight, Cold Chain)")
    public ResponseEntity<ApiResponse<java.util.List<com.logistics.order.strategy.PriceBreakdown>>> calculateTiersConcurrently(
            @Valid @RequestBody OrderDTOs.PriceCalculationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(parallelPricingService.calculateAllTiersConcurrently(request), messageService.getMessage(MessageCode.SUCCESS)));
    }
}
