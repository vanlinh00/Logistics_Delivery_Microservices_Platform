package com.logistics.order.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_tracking_number", columnList = "trackingNumber", unique = true),
    @Index(name = "idx_order_customer_id", columnList = "customerId"),
    @Index(name = "idx_order_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 32)
    private String trackingNumber;

    @Column(nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    // Sender Address Details
    @Column(nullable = false)
    private String senderName;
    @Column(nullable = false)
    private String senderPhone;
    @Column(nullable = false)
    private String senderAddress;
    private Double senderLatitude;
    private Double senderLongitude;

    // Recipient Address Details
    @Column(nullable = false)
    private String recipientName;
    @Column(nullable = false)
    private String recipientPhone;
    @Column(nullable = false)
    private String recipientAddress;
    private Double recipientLatitude;
    private Double recipientLongitude;

    // Dimensions & Weight
    @Column(name = "total_weight_kg", nullable = false)
    private Double totalWeightKg;

    @Column(name = "total_volume_m3")
    private Double totalVolumeM3;

    // Pricing & Fees
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal baseShippingFee;
    @Column(precision = 12, scale = 2)
    private BigDecimal weightSurcharge;
    @Column(precision = 12, scale = 2)
    private BigDecimal insuranceFee;
    @Column(precision = 12, scale = 2)
    private BigDecimal codAmount; // Cash On Delivery
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 500)
    private String specialInstructions;

    private String assignedDriverId;
    private String currentHubId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Version
    private Long version;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}
