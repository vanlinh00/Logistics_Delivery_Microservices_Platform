package com.logistics.tracking.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.time.Instant;
import java.util.List;

/**
 * Elasticsearch Document representing a parcel / consignment in the Logistics Search Engine.
 * Supports full-text fuzzy search, edge n-gram autocomplete, geo-distance radius search,
 * multi-field boosting, and faceted aggregations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "logistics_parcels", createIndex = true)
@Setting(settingPath = "/elasticsearch/parcel-settings.json")
public class ParcelIndexDocument {

    @Id
    @Field(type = FieldType.Keyword)
    private String id; // Tracking Code (e.g. "ORD-984210")

    @MultiField(
        mainField = @Field(type = FieldType.Keyword),
        otherFields = {
            @InnerField(suffix = "suggest", type = FieldType.Text, analyzer = "autocomplete_index", searchAnalyzer = "autocomplete_search"),
            @InnerField(suffix = "wildcard", type = FieldType.Wildcard)
        }
    )
    private String trackingNumber;

    @Field(type = FieldType.Keyword)
    private String orderId;

    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "vietnamese_standard_analyzer"),
        otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword),
            @InnerField(suffix = "autocomplete", type = FieldType.Text, analyzer = "autocomplete_index", searchAnalyzer = "autocomplete_search")
        }
    )
    private String senderName;

    @Field(type = FieldType.Keyword)
    private String senderPhone;

    @Field(type = FieldType.Text, analyzer = "vietnamese_standard_analyzer")
    private String senderAddress;

    @Field(type = FieldType.Keyword)
    private String originCity;

    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "vietnamese_standard_analyzer"),
        otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword),
            @InnerField(suffix = "autocomplete", type = FieldType.Text, analyzer = "autocomplete_index", searchAnalyzer = "autocomplete_search")
        }
    )
    private String recipientName;

    @Field(type = FieldType.Keyword)
    private String recipientPhone;

    @Field(type = FieldType.Text, analyzer = "vietnamese_standard_analyzer")
    private String recipientAddress;

    @Field(type = FieldType.Keyword)
    private String destinationCity;

    @Field(type = FieldType.Keyword)
    private String destinationDistrict;

    @Field(type = FieldType.Keyword)
    private String currentStatus; // PICKUP_SCHEDULED, PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, RETURNED

    @Field(type = FieldType.Text, analyzer = "vietnamese_standard_analyzer")
    private String statusDescription;

    @Field(type = FieldType.Text, analyzer = "vietnamese_standard_analyzer")
    private String currentHub;

    @GeoPointField
    private GeoPoint currentLocation; // GPS Coordinates (lat, lon) for spatial search

    @Field(type = FieldType.Double)
    private Double weightKg;

    @Field(type = FieldType.Double)
    private Double shippingFee;

    @Field(type = FieldType.Double)
    private Double codAmount;

    @Field(type = FieldType.Keyword)
    private String shippingServiceType; // STANDARD, EXPRESS, HEAVY_FREIGHT, COLD_CHAIN

    @Field(type = FieldType.Keyword)
    private String assignedCarrierName;

    @Field(type = FieldType.Keyword)
    private String assignedCarrierPhone;

    @Field(type = FieldType.Text, analyzer = "vietnamese_standard_analyzer")
    private String itemDescription; // e.g. "Laptop Dell XPS 15, Chuột Logitech, Sạc 130W"

    @Field(type = FieldType.Keyword)
    private List<String> tags; // ["FRAGILE", "HIGH_VALUE", "OVERSIZED", "ELECTRONICS"]

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant createdAt;

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant lastUpdatedAt;

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant estimatedDeliveryTime;

    @Field(type = FieldType.Nested)
    private List<TrackingEventSummary> eventTimeline;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrackingEventSummary {
        @Field(type = FieldType.Keyword)
        private String eventId;

        @Field(type = FieldType.Keyword)
        private String status;

        @Field(type = FieldType.Text, analyzer = "vietnamese_standard_analyzer")
        private String location;

        @Field(type = FieldType.Text)
        private String notes;

        @GeoPointField
        private GeoPoint coordinates;

        @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
        private Instant timestamp;
    }
}
