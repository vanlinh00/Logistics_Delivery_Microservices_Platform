package com.logistics.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchant_profiles", indexes = {
    @Index(name = "idx_merchant_user_id", columnList = "user_id", unique = true),
    @Index(name = "idx_merchant_tax_code", columnList = "taxCode")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 128)
    private String shopName;

    @Column(length = 32)
    private String taxCode;

    @Column(length = 256)
    private String warehouseAddress;

    @Column(length = 64)
    private String bankAccount;

    @Column(length = 64)
    private String bankName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private CodTier codTier = CodTier.STANDARD;

    @Builder.Default
    private Double discountRate = 0.05; // 5% base discount for high-volume merchants

    @Builder.Default
    private Long monthlyShipmentVolume = 0L;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum CodTier {
        STANDARD,
        VIP_FAST_PAYOUT,
        ENTERPRISE
    }
}
