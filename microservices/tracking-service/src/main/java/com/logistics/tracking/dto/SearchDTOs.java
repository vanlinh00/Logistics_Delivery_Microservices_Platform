package com.logistics.tracking.dto;

import com.logistics.tracking.document.ParcelIndexDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public class SearchDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParcelSearchRequest {
        private String query; // Full-text keyword or tracking number
        private String status; // Filter: e.g. DELIVERED, IN_TRANSIT
        private String destinationCity; // Filter: e.g. Hà Nội, TP. Hồ Chí Minh
        private String shippingServiceType; // Filter: STANDARD, EXPRESS, HEAVY_FREIGHT
        private Double minWeight;
        private Double maxWeight;
        private Double minPrice;
        private Double maxPrice;
        private String sortBy; // _score, createdAt, lastUpdatedAt, shippingFee, weightKg
        private String sortDirection; // ASC, DESC
        private Integer page;
        private Integer size;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoSearchRequest {
        private Double latitude;
        private Double longitude;
        private Double radiusKm; // Search radius in kilometers (e.g. 10.0km)
        private String status;
        private Integer limit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AutocompleteSuggestion {
        private String text;
        private String type; // TRACKING_CODE, RECIPIENT_NAME, SENDER_NAME, PHONE, ITEM
        private String trackingNumber;
        private String subtitle;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResultResponse {
        private long totalHits;
        private long tookMillis;
        private List<ParcelHitDTO> parcels;
        private Map<String, Long> statusFacets;
        private Map<String, Long> cityFacets;
        private Map<String, Long> serviceTypeFacets;
        private PriceStatsDTO priceStats;
        private int currentPage;
        private int totalPages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParcelHitDTO {
        private ParcelIndexDocument document;
        private float score;
        private Map<String, List<String>> highlights;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceStatsDTO {
        private Double count;
        private Double min;
        private Double max;
        private Double avg;
        private Double sum;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClusterHealthDTO {
        private String status; // GREEN, YELLOW, RED
        private int numberOfNodes;
        private int activePrimaryShards;
        private int activeShards;
        private long totalDocumentsIndexed;
        private String clusterName;
        private String jvmHeapUsage;
        private String diskUsage;
    }
}
