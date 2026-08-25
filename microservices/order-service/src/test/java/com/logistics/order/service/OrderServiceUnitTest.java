package com.logistics.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.order.dto.OrderDTOs;
import com.logistics.order.exception.BusinessRuleViolationException;
import com.logistics.order.exception.InvalidStatusTransitionException;
import com.logistics.order.exception.ResourceNotFoundException;
import com.logistics.order.model.Order;
import com.logistics.order.model.OrderOutbox;
import com.logistics.order.model.OrderStatus;
import com.logistics.order.repository.OrderOutboxRepository;
import com.logistics.order.repository.OrderRepository;
import com.logistics.order.service.AddressValidationService.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 🧪 OrderServiceUnitTest:
 * Comprehensive unit test suite covering order lifecycle, price coordination,
 * address validation rules, status state transitions, and distributed locking.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceUnitTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderOutboxRepository outboxRepository;

    @Mock
    private PricingCalculationService pricingService;

    @Mock
    private AddressValidationService addressService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private com.logistics.order.saga.OrderSagaOrchestrator sagaOrchestrator;

    @Mock
    private RLock distributedLock;

    @InjectMocks
    private OrderService orderService;

    private OrderDTOs.CreateOrderRequest sampleRequest;
    private OrderDTOs.PriceCalculationResponse samplePriceResponse;

    @BeforeEach
    void setUp() {
        sampleRequest = OrderDTOs.CreateOrderRequest.builder()
                .customerId(UUID.randomUUID())
                .senderName("TechStore HN")
                .senderPhone("0903112233")
                .senderAddress("Số 18 Duy Tân, Cầu Giấy, Hà Nội")
                .recipientName("Nguyễn Văn Linh")
                .recipientPhone("0984210001")
                .recipientAddress("Tòa Bitexco, Q1, TP. Hồ Chí Minh")
                .totalWeightKg(2.5)
                .totalVolumeM3(0.015)
                .declaredValue(BigDecimal.valueOf(15000000))
                .codAmount(BigDecimal.valueOf(15000000))
                .items(List.of(
                        OrderDTOs.CreateOrderItemRequest.builder()
                                .itemName("Dell XPS 15")
                                .quantity(1)
                                .weightKg(2.0)
                                .declaredValue(BigDecimal.valueOf(15000000))
                                .category("ELECTRONICS")
                                .build()
                ))
                .build();

        samplePriceResponse = OrderDTOs.PriceCalculationResponse.builder()
                .baseFee(BigDecimal.valueOf(30000))
                .weightSurcharge(BigDecimal.valueOf(15000))
                .insuranceFee(BigDecimal.valueOf(10000))
                .totalShippingFee(BigDecimal.valueOf(55000))
                .build();
    }

    @Nested
    @DisplayName("Create Order Workflow Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create order successfully and emit Outbox event when addresses and prices are valid")
        void createOrder_WhenValidRequest_ShouldSucceed() throws Exception {
            // Arrange
            when(addressService.validateAddress(sampleRequest.getSenderAddress(), sampleRequest.getSenderPhone()))
                    .thenReturn(ValidationResult.builder()
                            .valid(true)
                            .formattedAddress("Số 18 Duy Tân, Cầu Giấy, Hà Nội")
                            .latitude(21.0285)
                            .longitude(105.8542)
                            .build());

            when(addressService.validateAddress(sampleRequest.getRecipientAddress(), sampleRequest.getRecipientPhone()))
                    .thenReturn(ValidationResult.builder()
                            .valid(true)
                            .formattedAddress("Tòa Bitexco, Q1, TP. Hồ Chí Minh")
                            .latitude(10.8015)
                            .longitude(106.6644)
                            .build());

            when(pricingService.calculatePrice(any())).thenReturn(samplePriceResponse);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order ord = invocation.getArgument(0);
                if (ord.getId() == null) {
                    ord.setId(UUID.randomUUID());
                }
                return ord;
            });
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"orderId\":\"test\"}");

            // Act
            Order result = orderService.createOrder(sampleRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);
            assertThat(result.getTrackingNumber()).startsWith("VNX");
            assertThat(result.getRecipientName()).isEqualTo("Nguyễn Văn Linh");
            assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(55000));
            assertThat(result.getItems()).hasSize(1);

            // Verify Outbox Event created for Kafka propagation
            ArgumentCaptor<OrderOutbox> outboxCaptor = ArgumentCaptor.forClass(OrderOutbox.class);
            verify(outboxRepository, times(1)).save(outboxCaptor.capture());
            OrderOutbox outbox = outboxCaptor.getValue();
            assertThat(outbox.getEventType()).isEqualTo("ORDER_CREATED");
            assertThat(outbox.getProcessed()).isFalse();
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when sender address is invalid")
        void createOrder_WhenSenderAddressInvalid_ShouldThrowException() {
            // Arrange
            when(addressService.validateAddress(any(), any()))
                    .thenReturn(ValidationResult.builder()
                            .valid(false)
                            .message("Số điện thoại người gửi không đúng định dạng")
                            .build());

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(sampleRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Địa chỉ người gửi không hợp lệ");

            verifyNoInteractions(pricingService);
            verifyNoInteractions(orderRepository);
            verifyNoInteractions(outboxRepository);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when recipient address is invalid")
        void createOrder_WhenRecipientAddressInvalid_ShouldThrowException() {
            // Arrange
            when(addressService.validateAddress(sampleRequest.getSenderAddress(), sampleRequest.getSenderPhone()))
                    .thenReturn(ValidationResult.builder().valid(true).formattedAddress("Valid Address").build());

            when(addressService.validateAddress(sampleRequest.getRecipientAddress(), sampleRequest.getRecipientPhone()))
                    .thenReturn(ValidationResult.builder().valid(false).message("Tòa nhà không tồn tại").build());

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(sampleRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Địa chỉ người nhận không hợp lệ");

            verify(orderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Status Transition & Distributed Lock Tests")
    class StatusTransitionTests {

        @Test
        @DisplayName("Should transition status safely with Redisson distributed lock")
        void updateOrderStatus_WhenValidTransition_ShouldSucceed() throws Exception {
            UUID orderId = UUID.randomUUID();
            Order existingOrder = Order.builder()
                    .id(orderId)
                    .trackingNumber("VNX123456789")
                    .status(OrderStatus.CREATED)
                    .build();

            when(redissonClient.getLock("lock:order:" + orderId)).thenReturn(distributedLock);
            when(distributedLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(distributedLock.isHeldByCurrentThread()).thenReturn(true);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

            OrderDTOs.UpdateOrderStatusRequest updateReq = OrderDTOs.UpdateOrderStatusRequest.builder()
                    .status("PAYMENT_CONFIRMED")
                    .note("Xác nhận điều phối xe bưu tá")
                    .build();

            // Act
            Order updated = orderService.updateOrderStatus(orderId, updateReq);

            // Assert
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.PAYMENT_CONFIRMED);
            verify(distributedLock, times(1)).unlock();
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when order does not exist")
        void updateOrderStatus_WhenNotFound_ShouldThrowException() throws Exception {
            UUID orderId = UUID.randomUUID();
            when(redissonClient.getLock(anyString())).thenReturn(distributedLock);
            when(distributedLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(distributedLock.isHeldByCurrentThread()).thenReturn(true);
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            OrderDTOs.UpdateOrderStatusRequest updateReq = OrderDTOs.UpdateOrderStatusRequest.builder()
                    .status("PICKED_UP")
                    .build();

            assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, updateReq))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(distributedLock).unlock();
        }
    }
}
