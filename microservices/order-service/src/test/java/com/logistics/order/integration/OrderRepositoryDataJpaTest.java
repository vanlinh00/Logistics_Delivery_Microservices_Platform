package com.logistics.order.integration;

import com.logistics.order.model.Order;
import com.logistics.order.model.OrderStatus;
import com.logistics.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 🧪 OrderRepositoryDataJpaTest:
 * Integration test verifying JPA entity mapping, custom finder queries,
 * and database constraints.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("OrderRepository JPA Integration Tests")
class OrderRepositoryDataJpaTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = Order.builder()
                .trackingNumber("VNX88990011")
                .customerId(UUID.randomUUID())
                .status(OrderStatus.CREATED)
                .senderName("Sender VN")
                .senderPhone("0912345678")
                .senderAddress("Hà Nội")
                .recipientName("Recipient VN")
                .recipientPhone("0987654321")
                .recipientAddress("TP. Hồ Chí Minh")
                .totalWeightKg(1.5)
                .baseShippingFee(BigDecimal.valueOf(30000))
                .totalAmount(BigDecimal.valueOf(35000))
                .build();
    }

    @Test
    @DisplayName("Should persist and retrieve order by tracking number")
    void findByTrackingNumber_WhenPersisted_ShouldReturnOrder() {
        // Arrange
        Order persisted = entityManager.persistAndFlush(sampleOrder);

        // Act
        Optional<Order> found = orderRepository.findByTrackingNumber("VNX88990011");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(persisted.getId());
        assertThat(found.get().getRecipientPhone()).isEqualTo("0987654321");
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.CREATED);
    }
}
