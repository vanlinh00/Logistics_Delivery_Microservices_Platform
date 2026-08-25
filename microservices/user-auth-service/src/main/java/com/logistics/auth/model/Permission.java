package com.logistics.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "permissions", indexes = {
    @Index(name = "idx_permission_code", columnList = "code", unique = true),
    @Index(name = "idx_permission_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String code; // e.g. "orders:read", "orders:write", "fleet:dispatch"

    @Column(nullable = false, length = 128)
    private String name; // e.g. "View Orders", "Create & Edit Orders"

    @Column(length = 64)
    private String category; // e.g. "ORDERS", "FLEET", "USERS", "SYSTEM"

    @Column(length = 255)
    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
