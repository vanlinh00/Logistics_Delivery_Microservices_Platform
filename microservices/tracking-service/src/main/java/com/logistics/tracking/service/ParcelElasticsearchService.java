package com.logistics.tracking.service;

import com.logistics.tracking.document.ParcelIndexDocument;
import com.logistics.tracking.dto.SearchDTOs.*;
import com.logistics.tracking.model.TrackingEvent;
import com.logistics.tracking.repository.ParcelElasticsearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enterprise Service handling all Elasticsearch 8.x operations:
 * - High-speed multi-attribute fuzzy search with relevance ranking (_score)
 * - Autocomplete and prefix suggesters
 * - Spatial Geo-Distance perimeter searches
 * - Aggregation analytics (Faceting by status, destination, service type, revenue)
 * - Real-time Change Data Capture (CDC) syncing from Kafka tracking events
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParcelElasticsearchService {

    private final ParcelElasticsearchRepository repository;
    private final ElasticsearchOperations elasticsearchOperations;
    private static final String INDEX_NAME = "logistics_parcels";

    /**
     * Executes advanced logistics search with full-text fuzzy matching, boolean filtering,
     * highlight snippet generation, and metric aggregations.
     */
    public SearchResultResponse searchParcels(ParcelSearchRequest request) {
        long startTime = System.currentTimeMillis();

        int page = request.getPage() != null ? Math.max(0, request.getPage()) : 0;
        int size = request.getSize() != null ? Math.min(100, Math.max(1, request.getSize())) : 20;

        Sort.Direction direction = "ASC".equalsIgnoreCase(request.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortField = (request.getSortBy() != null && !request.getSortBy().isBlank()) ? request.getSortBy() : "_score";
        
        Pageable pageable = "_score".equals(sortField) ? PageRequest.of(page, size) : PageRequest.of(page, size, Sort.by(direction, sortField));

        NativeQueryBuilder queryBuilder = NativeQuery.builder()
                .withPageable(pageable);

        // Highlight configuration
        HighlightParameters highlightParams = HighlightParameters.builder()
                .withPreTags("<mark class='bg-amber-400/25 text-amber-300 font-semibold px-1 rounded'>")
                .withPostTags("</mark>")
                .withNumberOfFragments(2)
                .withFragmentSize(150)
                .build();

        Highlight highlight = new Highlight(highlightParams, List.of(
                new HighlightField("recipientName"),
                new HighlightField("recipientAddress"),
                new HighlightField("itemDescription"),
                new HighlightField("trackingNumber")
        ));
        queryBuilder.withHighlightQuery(new HighlightQuery(highlight, ParcelIndexDocument.class));

        // Build Elasticsearch Query with BoolQueryBuilder
        queryBuilder.withQuery(q -> q.bool(b -> {
            // Full-Text Search with Fuzzy and Boosting
            if (request.getQuery() != null && !request.getQuery().trim().isEmpty()) {
                String term = request.getQuery().trim();
                b.must(m -> m.bool(subBool -> subBool
                    .should(s -> s.term(t -> t.field("trackingNumber").value(term).boost(10.0f)))
                    .should(s -> s.term(t -> t.field("recipientPhone").value(term).boost(8.0f)))
                    .should(s -> s.multiMatch(mm -> mm
                        .query(term)
                        .fields("trackingNumber^8", "recipientName^4", "recipientPhone^5", "itemDescription^3", "recipientAddress^2", "senderName", "currentHub")
                        .fuzziness("AUTO")
                        .prefixLength(1)
                    ))
                    .should(s -> s.wildcard(w -> w.field("trackingNumber.wildcard").value("*" + term.toUpperCase() + "*").boost(5.0f)))
                ));
            } else {
                b.must(m -> m.matchAll(ma -> ma));
            }

            // Status Filter
            if (request.getStatus() != null && !request.getStatus().isBlank() && !"ALL".equalsIgnoreCase(request.getStatus())) {
                b.filter(f -> f.term(t -> t.field("currentStatus").value(request.getStatus())));
            }

            // Destination City Filter
            if (request.getDestinationCity() != null && !request.getDestinationCity().isBlank()) {
                b.filter(f -> f.term(t -> t.field("destinationCity").value(request.getDestinationCity())));
            }

            // Shipping Service Type Filter
            if (request.getShippingServiceType() != null && !request.getShippingServiceType().isBlank()) {
                b.filter(f -> f.term(t -> t.field("shippingServiceType").value(request.getShippingServiceType())));
            }

            // Weight Range Filter
            if (request.getMinWeight() != null || request.getMaxWeight() != null) {
                b.filter(f -> f.range(r -> {
                    r.field("weightKg");
                    if (request.getMinWeight() != null) r.gte(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.numberNode(request.getMinWeight()).asText());
                    if (request.getMaxWeight() != null) r.lte(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.numberNode(request.getMaxWeight()).asText());
                    return r;
                }));
            }

            // Price / COD Range Filter
            if (request.getMinPrice() != null || request.getMaxPrice() != null) {
                b.filter(f -> f.range(r -> {
                    r.field("shippingFee");
                    if (request.getMinPrice() != null) r.gte(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.numberNode(request.getMinPrice()).asText());
                    if (request.getMaxPrice() != null) r.lte(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.numberNode(request.getMaxPrice()).asText());
                    return r;
                }));
            }

            return b;
        }));

        SearchHits<ParcelIndexDocument> searchHits;
        try {
            searchHits = elasticsearchOperations.search(queryBuilder.build(), ParcelIndexDocument.class, IndexCoordinates.of(INDEX_NAME));
        } catch (Exception ex) {
            log.warn("Elasticsearch live query fallback: {}", ex.getMessage());
            // Fallback to local Spring repository
            searchHits = repository.searchFuzzy(request.getQuery() != null ? request.getQuery() : "", pageable);
        }

        long took = System.currentTimeMillis() - startTime;

        List<ParcelHitDTO> parcelHits = searchHits.getSearchHits().stream().map(hit -> {
            return ParcelHitDTO.builder()
                    .document(hit.getContent())
                    .score(hit.getScore())
                    .highlights(hit.getHighlightFields())
                    .build();
        }).collect(Collectors.toList());

        // Compute Facets dynamically
        Map<String, Long> statusFacets = computeStatusFacets(parcelHits);
        Map<String, Long> cityFacets = computeCityFacets(parcelHits);
        Map<String, Long> serviceTypeFacets = computeServiceTypeFacets(parcelHits);
        PriceStatsDTO priceStats = computePriceStats(parcelHits);

        long totalHits = searchHits.getTotalHits();
        int totalPages = (int) Math.ceil((double) totalHits / size);

        return SearchResultResponse.builder()
                .totalHits(totalHits)
                .tookMillis(took)
                .parcels(parcelHits)
                .statusFacets(statusFacets)
                .cityFacets(cityFacets)
                .serviceTypeFacets(serviceTypeFacets)
                .priceStats(priceStats)
                .currentPage(page)
                .totalPages(Math.max(1, totalPages))
                .build();
    }

    /**
     * Fast Autocomplete & Prefix Suggestion query for Search bar
     */
    public List<AutocompleteSuggestion> getAutocompleteSuggestions(String prefix) {
        if (prefix == null || prefix.trim().length() < 2) {
            return Collections.emptyList();
        }

        String cleanPrefix = prefix.trim();
        List<ParcelIndexDocument> matched = repository.suggestAutocomplete(cleanPrefix, PageRequest.of(0, 8));

        List<AutocompleteSuggestion> suggestions = new ArrayList<>();

        for (ParcelIndexDocument doc : matched) {
            if (doc.getTrackingNumber() != null && doc.getTrackingNumber().toLowerCase().contains(cleanPrefix.toLowerCase())) {
                suggestions.add(AutocompleteSuggestion.builder()
                        .text(doc.getTrackingNumber())
                        .type("TRACKING_CODE")
                        .trackingNumber(doc.getTrackingNumber())
                        .subtitle(doc.getDestinationCity() + " • " + doc.getCurrentStatus())
                        .build());
            }
            if (doc.getRecipientName() != null && doc.getRecipientName().toLowerCase().contains(cleanPrefix.toLowerCase())) {
                suggestions.add(AutocompleteSuggestion.builder()
                        .text(doc.getRecipientName())
                        .type("RECIPIENT_NAME")
                        .trackingNumber(doc.getTrackingNumber())
                        .subtitle("Người nhận: " + doc.getRecipientAddress())
                        .build());
            }
            if (doc.getItemDescription() != null && doc.getItemDescription().toLowerCase().contains(cleanPrefix.toLowerCase())) {
                suggestions.add(AutocompleteSuggestion.builder()
                        .text(doc.getItemDescription())
                        .type("ITEM")
                        .trackingNumber(doc.getTrackingNumber())
                        .subtitle("Kiện hàng: " + doc.getTrackingNumber())
                        .build());
            }
        }

        return suggestions.stream().limit(6).collect(Collectors.toList());
    }

    /**
     * Spatial Geo-Distance Search: Finds parcels within radiusKm of GPS coordinates
     */
    public List<ParcelHitDTO> searchNearbyParcels(GeoSearchRequest geoRequest) {
        Double lat = geoRequest.getLatitude();
        Double lon = geoRequest.getLongitude();
        Double radius = geoRequest.getRadiusKm() != null ? geoRequest.getRadiusKm() : 15.0;
        int limit = geoRequest.getLimit() != null ? geoRequest.getLimit() : 20;

        if (lat == null || lon == null) {
            return Collections.emptyList();
        }

        NativeQueryBuilder queryBuilder = NativeQuery.builder()
                .withPageable(PageRequest.of(0, limit))
                .withQuery(q -> q.bool(b -> {
                    b.filter(f -> f.geoDistance(gd -> gd
                            .field("currentLocation")
                            .distance(radius + "km")
                            .location(l -> l.latlon(ll -> ll.lat(lat).lon(lon)))
                    ));
                    if (geoRequest.getStatus() != null && !geoRequest.getStatus().isBlank()) {
                        b.filter(f -> f.term(t -> t.field("currentStatus").value(geoRequest.getStatus())));
                    }
                    return b;
                }));

        SearchHits<ParcelIndexDocument> hits = elasticsearchOperations.search(queryBuilder.build(), ParcelIndexDocument.class);

        return hits.getSearchHits().stream().map(h -> ParcelHitDTO.builder()
                .document(h.getContent())
                .score(h.getScore())
                .highlights(Collections.emptyMap())
                .build()
        ).collect(Collectors.toList());
    }

    /**
     * Indexes or updates a parcel document into Elasticsearch in real-time
     */
    public ParcelIndexDocument indexParcel(ParcelIndexDocument document) {
        if (document.getLastUpdatedAt() == null) {
            document.setLastUpdatedAt(Instant.now());
        }
        return repository.save(document);
    }

    /**
     * CDC / Event-Driven Sync: Ingests a new TrackingEvent and updates Elasticsearch document
     */
    public void syncTrackingEventToElasticsearch(TrackingEvent event) {
        try {
            Optional<ParcelIndexDocument> optionalDoc = repository.findById(event.getTrackingNumber());
            ParcelIndexDocument doc = optionalDoc.orElseGet(() -> ParcelIndexDocument.builder()
                    .id(event.getTrackingNumber())
                    .trackingNumber(event.getTrackingNumber())
                    .createdAt(Instant.now())
                    .eventTimeline(new ArrayList<>())
                    .build());

            doc.setCurrentStatus(event.getStatus() != null ? event.getStatus().name() : "IN_TRANSIT");
            doc.setStatusDescription(event.getStatusDescription());
            doc.setCurrentHub(event.getLocation());
            doc.setLastUpdatedAt(Instant.now());

            if (event.getLatitude() != null && event.getLongitude() != null) {
                doc.setCurrentLocation(new GeoPoint(event.getLatitude(), event.getLongitude()));
            }

            if (doc.getEventTimeline() == null) {
                doc.setEventTimeline(new ArrayList<>());
            }

            doc.getEventTimeline().add(ParcelIndexDocument.TrackingEventSummary.builder()
                    .eventId(event.getId() != null ? event.getId().toString() : UUID.randomUUID().toString())
                    .status(event.getStatus() != null ? event.getStatus().name() : "UPDATE")
                    .location(event.getLocation())
                    .notes(event.getStatusDescription())
                    .coordinates(event.getLatitude() != null ? new GeoPoint(event.getLatitude(), event.getLongitude()) : null)
                    .timestamp(Instant.now())
                    .build());

            repository.save(doc);
            log.info("Successfully synchronized TrackingEvent for [{}] into Elasticsearch", event.getTrackingNumber());
        } catch (Exception e) {
            log.error("Failed to sync tracking event to Elasticsearch: {}", e.getMessage(), e);
        }
    }

    /**
     * Seeds realistic logistics parcels for testing & demonstration
     */
    public List<ParcelIndexDocument> seedSampleParcels() {
        List<ParcelIndexDocument> samples = List.of(
            ParcelIndexDocument.builder()
                .id("ORD-984210")
                .trackingNumber("ORD-984210")
                .orderId("ORD-984210")
                .senderName("Phạm Hoàng Long - TechStore")
                .senderPhone("0903112233")
                .senderAddress("Số 18 Duy Tân, Dịch Vọng Hậu, Cầu Giấy")
                .originCity("Hà Nội")
                .recipientName("Nguyễn Văn Linh")
                .recipientPhone("0984210001")
                .recipientAddress("Tòa nhà Bitexco, Số 2 Hải Triều, Bến Nghé, Quận 1")
                .destinationCity("TP. Hồ Chí Minh")
                .destinationDistrict("Quận 1")
                .currentStatus("IN_TRANSIT")
                .statusDescription("Đang trung chuyển qua Hub Tân Bình, chuẩn bị giao cho Shipper")
                .currentHub("Hub Tân Bình Express Hub")
                .currentLocation(new GeoPoint(10.8015, 106.6644))
                .weightKg(1.85)
                .shippingFee(45000.0)
                .codAmount(1450000.0)
                .shippingServiceType("EXPRESS")
                .assignedCarrierName("Nguyễn Văn Hùng (Courier-01)")
                .assignedCarrierPhone("0912345678")
                .itemDescription("Laptop Dell XPS 15 9530, Chuột Logitech MX Master 3S, Túi chống sốc")
                .tags(List.of("HIGH_VALUE", "FRAGILE", "ELECTRONICS"))
                .createdAt(Instant.now().minusSeconds(86400 * 2))
                .lastUpdatedAt(Instant.now().minusSeconds(1800))
                .build(),

            ParcelIndexDocument.builder()
                .id("ORD-984211")
                .trackingNumber("ORD-984211")
                .orderId("ORD-984211")
                .senderName("Công ty TNHH Thời Trang May Mặc An Phú")
                .senderPhone("0933445566")
                .senderAddress("Khu Công Nghiệp Tân Bình, Tây Thạnh")
                .originCity("TP. Hồ Chí Minh")
                .recipientName("Trần Thị Mai Phương")
                .recipientPhone("0977889900")
                .recipientAddress("Số 45 Lê Duẩn, Phường Thạch Thang, Quận Hải Châu")
                .destinationCity("Đà Nẵng")
                .destinationDistrict("Hải Châu")
                .currentStatus("OUT_FOR_DELIVERY")
                .statusDescription("Shipper đang mang hàng đi giao, liên hệ trước khi đến")
                .currentHub("Bưu cục Hải Châu - Đà Nẵng")
                .currentLocation(new GeoPoint(16.0680, 108.2120))
                .weightKg(0.75)
                .shippingFee(32000.0)
                .codAmount(520000.0)
                .shippingServiceType("STANDARD")
                .assignedCarrierName("Lê Văn Tùng (Courier-04)")
                .assignedCarrierPhone("0945678901")
                .itemDescription("Váy lụa cao cấp dự tiệc, Áo Blazer màu be dáng rộng")
                .tags(List.of("FASHION", "COD"))
                .createdAt(Instant.now().minusSeconds(86400 * 3))
                .lastUpdatedAt(Instant.now().minusSeconds(900))
                .build(),

            ParcelIndexDocument.builder()
                .id("ORD-984212")
                .trackingNumber("ORD-984212")
                .orderId("ORD-984212")
                .senderName("Tổng Kho Cảng Hải Phòng Logistics Depot")
                .senderPhone("0918223344")
                .senderAddress("Đường Chùa Vẽ, Phường Đông Hải 1, Quận Hải An")
                .originCity("Hải Phòng")
                .recipientName("Công ty Công Nghệ Cao Samsung Electronics")
                .recipientPhone("0966554433")
                .recipientAddress("KCN Tiên Sơn, Phường Đồng Nguyên, TP. Từ Sơn")
                .destinationCity("Bắc Ninh")
                .destinationDistrict("Từ Sơn")
                .currentStatus("PICKUP_SCHEDULED")
                .statusDescription("Đã điều phối xe tải 5 tấn nhận hàng tại kho cảng")
                .currentHub("Kho Tiên Sơn Logistics Hub")
                .currentLocation(new GeoPoint(21.1215, 105.9750))
                .weightKg(45.0)
                .shippingFee(380000.0)
                .codAmount(0.0)
                .shippingServiceType("HEAVY_FREIGHT")
                .assignedCarrierName("Trần Đình Trọng (Fleet-Truck-09)")
                .assignedCarrierPhone("0981122334")
                .itemDescription("Linh kiện bán dẫn vi xử lý công nghiệp, Bộ cảm biến nhiệt độ quang học")
                .tags(List.of("HEAVY_FREIGHT", "B2B", "INDUSTRIAL"))
                .createdAt(Instant.now().minusSeconds(7200))
                .lastUpdatedAt(Instant.now().minusSeconds(1200))
                .build(),

            ParcelIndexDocument.builder()
                .id("ORD-984213")
                .trackingNumber("ORD-984213")
                .orderId("ORD-984213")
                .senderName("Dược Phẩm Sinh Học MedPharma VN")
                .senderPhone("0908889999")
                .senderAddress("Khu Công Nghệ Cao, Long Thạnh Mỹ, TP. Thủ Đức")
                .originCity("TP. Hồ Chí Minh")
                .recipientName("Bệnh viện Đa Khoa Trung Ương Cần Thơ")
                .recipientPhone("0907776655")
                .recipientAddress("Số 315 Nguyễn Văn Linh, Phường An Khánh, Quận Ninh Kiều")
                .destinationCity("Cần Thơ")
                .destinationDistrict("Ninh Kiều")
                .currentStatus("IN_TRANSIT")
                .statusDescription("Kiện hàng kiểm soát nhiệt độ 2°C - 8°C trên xe lạnh chuyên dụng")
                .currentHub("Trạm trung chuyển Tiền Giang")
                .currentLocation(new GeoPoint(10.3600, 106.3600))
                .weightKg(8.2)
                .shippingFee(250000.0)
                .codAmount(0.0)
                .shippingServiceType("COLD_CHAIN")
                .assignedCarrierName("Võ Minh Trí (ColdFleet-02)")
                .assignedCarrierPhone("0934567812")
                .itemDescription("Vắc xin sinh học bảo quản lạnh, Thuốc điều trị đặc trị chuyên khoa")
                .tags(List.of("COLD_CHAIN", "MEDICAL", "URGENT"))
                .createdAt(Instant.now().minusSeconds(86400))
                .lastUpdatedAt(Instant.now().minusSeconds(3600))
                .build(),

            ParcelIndexDocument.builder()
                .id("ORD-984214")
                .trackingNumber("ORD-984214")
                .orderId("ORD-984214")
                .senderName("Văn Phòng Phẩm & Sách Nhã Nam")
                .senderPhone("0912334455")
                .senderAddress("59 Đỗ Quang, Trung Hòa, Cầu Giấy")
                .originCity("Hà Nội")
                .recipientName("Hoàng Nhật Minh")
                .recipientPhone("0988776655")
                .recipientAddress("Số 12 Ngõ 198 Kim Mã, Giảng Võ, Ba Đình")
                .destinationCity("Hà Nội")
                .destinationDistrict("Ba Đình")
                .currentStatus("DELIVERED")
                .statusDescription("Giao hàng thành công cho người nhận lúc 14:30. Ký nhận bởi Hoàng Nhật Minh.")
                .currentHub("Bưu cục Ba Đình Express")
                .currentLocation(new GeoPoint(21.0333, 105.8190))
                .weightKg(1.2)
                .shippingFee(22000.0)
                .codAmount(180000.0)
                .shippingServiceType("STANDARD")
                .assignedCarrierName("Đỗ Anh Dũng (Courier-07)")
                .assignedCarrierPhone("0923456789")
                .itemDescription("Bộ sách Lịch Sử Văn Minh Thế Giới (5 tập), Sổ tay bìa da cao cấp")
                .tags(List.of("BOOKS", "DELIVERED"))
                .createdAt(Instant.now().minusSeconds(86400 * 4))
                .lastUpdatedAt(Instant.now().minusSeconds(14400))
                .build()
        );

        repository.saveAll(samples);
        log.info("Seeded {} sample parcels into Elasticsearch index '{}'", samples.size(), INDEX_NAME);
        return samples;
    }

    // Helper aggregation calculators
    private Map<String, Long> computeStatusFacets(List<ParcelHitDTO> hits) {
        Map<String, Long> map = new HashMap<>();
        for (ParcelHitDTO hit : hits) {
            String st = hit.getDocument().getCurrentStatus();
            if (st != null) map.put(st, map.getOrDefault(st, 0L) + 1);
        }
        return map;
    }

    private Map<String, Long> computeCityFacets(List<ParcelHitDTO> hits) {
        Map<String, Long> map = new HashMap<>();
        for (ParcelHitDTO hit : hits) {
            String city = hit.getDocument().getDestinationCity();
            if (city != null) map.put(city, map.getOrDefault(city, 0L) + 1);
        }
        return map;
    }

    private Map<String, Long> computeServiceTypeFacets(List<ParcelHitDTO> hits) {
        Map<String, Long> map = new HashMap<>();
        for (ParcelHitDTO hit : hits) {
            String type = hit.getDocument().getShippingServiceType();
            if (type != null) map.put(type, map.getOrDefault(type, 0L) + 1);
        }
        return map;
    }

    private PriceStatsDTO computePriceStats(List<ParcelHitDTO> hits) {
        if (hits.isEmpty()) {
            return PriceStatsDTO.builder().count(0.0).min(0.0).max(0.0).avg(0.0).sum(0.0).build();
        }
        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        int count = 0;
        for (ParcelHitDTO hit : hits) {
            Double fee = hit.getDocument().getShippingFee();
            if (fee != null) {
                sum += fee;
                if (fee < min) min = fee;
                if (fee > max) max = fee;
                count++;
            }
        }
        if (count == 0) min = max = 0;
        return PriceStatsDTO.builder()
                .count((double) count)
                .min(min)
                .max(max)
                .avg(count > 0 ? sum / count : 0.0)
                .sum(sum)
                .build();
    }
}
