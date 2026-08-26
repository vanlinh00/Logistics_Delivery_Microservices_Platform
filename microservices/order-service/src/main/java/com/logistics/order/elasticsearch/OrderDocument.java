package com.logistics.order.elasticsearch;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 📦 OrderDocument: Read-optimized Elasticsearch Index Model for Order Service.
 * 
 * ✅ Index: "logistics_orders"
 * ✅ Optimized for:
 *   - Instant full-text fuzzy lookup (Customer name, item names, delivery address)
 *   - Fast filtering (Status, City, Service Tier, Date range)
 *   - Aggregations (Order volume by status/city, average order value)
 *   - Proximity search (GeoPoint)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "logistics_orders", createIndex = false)
public class OrderDocument {

    @Id
    private String id; // Matches PostgreSQL Primary Key / Order Number (e.g. "ORD-984210")

    @Field(type = FieldType.Keyword)
    private String orderNumber;

    @Field(type = FieldType.Keyword)
    private String customerId;

    // Full-text search with Vietnamese analyzer + keyword for sorting
    @MultiField(
        mainField = @Field(type = FieldType.Text),
        otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
        }
    )
    private String customerName;

    @Field(type = FieldType.Keyword)
    private String customerPhone;

    @Field(type = FieldType.Text)
    private String deliveryAddress;

    @Field(type = FieldType.Keyword)
    private String destinationCity;

    @Field(type = FieldType.Keyword)
    private String destinationDistrict;

    @Field(type = FieldType.Keyword)
    private String status; // PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED

    @Field(type = FieldType.Double)
    private BigDecimal totalAmount;

    @Field(type = FieldType.Double)
    private BigDecimal codAmount;

    @Field(type = FieldType.Keyword)
    private String paymentStatus; // UNPAID, PAID, REFUNDED

    @Field(type = FieldType.Keyword)
    private String shippingServiceType; // STANDARD, EXPRESS, HEAVY_FREIGHT, COLD_CHAIN

    // Multi-item descriptions indexed for rich full-text product search (e.g., "iPhone", "Dell XPS")
    @Field(type = FieldType.Text)
    private String itemSummary;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Date)
    private Instant createdAt;

    @Field(type = FieldType.Date)
    private Instant updatedAt;

    // Spatial coordinate of destination for radius querying
    private GeoPoint deliveryGeoPoint;
}
