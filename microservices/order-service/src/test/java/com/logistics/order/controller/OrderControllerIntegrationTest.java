package com.logistics.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.order.dto.OrderDTOs;
import com.logistics.order.model.Order;
import com.logistics.order.model.OrderStatus;
import com.logistics.order.service.OrderService;
import com.logistics.order.service.ParallelPricingAggregatorService;
import com.logistics.order.service.PricingCalculationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 🧪 OrderControllerIntegrationTest:
 * Web layer integration test utilizing MockMvc to validate HTTP contracts,
 * request validation constraints, and JSON serialization.
 */
@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filter chain for isolated web testing
@DisplayName("OrderController WebMvc Integration Tests")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private PricingCalculationService pricingService;

    @MockBean
    private ParallelPricingAggregatorService parallelPricingService;

    @Nested
    @DisplayName("POST /api/v1/orders - Order Creation Endpoint")
    class CreateOrderEndpointTests {

        @Test
        @DisplayName("Should return HTTP 201 Created when order payload is valid")
        void createOrder_WhenPayloadValid_ShouldReturnCreated() throws Exception {
            OrderDTOs.CreateOrderRequest request = OrderDTOs.CreateOrderRequest.builder()
                    .customerId(UUID.randomUUID())
                    .senderName("TechStore HN")
                    .senderPhone("0903112233")
                    .senderAddress("18 Duy Tân, Cầu Giấy, Hà Nội")
                    .recipientName("Nguyễn Văn Linh")
                    .recipientPhone("0984210001")
                    .recipientAddress("Tòa nhà Bitexco, Q1, TP.HCM")
                    .totalWeightKg(2.5)
                    .totalVolumeM3(0.01)
                    .declaredValue(BigDecimal.valueOf(15000000))
                    .codAmount(BigDecimal.valueOf(15000000))
                    .items(List.of(
                            OrderDTOs.OrderItemRequest.builder()
                                    .itemName("Dell XPS 15")
                                    .quantity(1)
                                    .weightKg(2.0)
                                    .category("ELECTRONICS")
                                    .build()
                    ))
                    .build();

            Order createdOrder = Order.builder()
                    .id(UUID.randomUUID())
                    .trackingNumber("VNX984210001")
                    .status(OrderStatus.CREATED)
                    .recipientName("Nguyễn Văn Linh")
                    .recipientPhone("0984210001")
                    .totalAmount(BigDecimal.valueOf(45000))
                    .build();

            when(orderService.createOrder(any(OrderDTOs.CreateOrderRequest.class))).thenReturn(createdOrder);

            mockMvc.perform(post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.trackingNumber", is("VNX984210001")))
                    .andExpect(jsonPath("$.data.recipientName", is("Nguyễn Văn Linh")))
                    .andExpect(jsonPath("$.data.status", is("CREATED")));
        }

        @Test
        @DisplayName("Should return HTTP 400 Bad Request when required fields are missing")
        void createOrder_WhenPayloadInvalid_ShouldReturnBadRequest() throws Exception {
            // Missing senderName, senderAddress, recipientPhone
            OrderDTOs.CreateOrderRequest invalidRequest = OrderDTOs.CreateOrderRequest.builder()
                    .totalWeightKg(-5.0) // Invalid negative weight
                    .build();

            mockMvc.perform(post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orders/track/{trackingNumber} - Tracking Endpoint")
    class TrackOrderEndpointTests {

        @Test
        @DisplayName("Should return HTTP 200 OK and Order details when tracking number exists")
        void getOrderByTracking_WhenFound_ShouldReturnOk() throws Exception {
            String trackingNumber = "VNX984210001";
            Order order = Order.builder()
                    .id(UUID.randomUUID())
                    .trackingNumber(trackingNumber)
                    .status(OrderStatus.IN_TRANSIT)
                    .senderName("TechStore HN")
                    .recipientName("Nguyễn Văn Linh")
                    .build();

            when(orderService.getOrderByTrackingNumber(eq(trackingNumber))).thenReturn(order);

            mockMvc.perform(get("/api/v1/orders/track/{trackingNumber}", trackingNumber)
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.trackingNumber", is(trackingNumber)))
                    .andExpect(jsonPath("$.data.status", is("IN_TRANSIT")));
        }
    }
}
