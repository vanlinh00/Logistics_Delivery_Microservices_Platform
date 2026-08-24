package com.logistics.order.elasticsearch;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.GeoDistanceQuery;
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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 💡 OrderElasticsearchService:
 * Demonstrates WHEN to query Elasticsearch vs PostgreSQL.
 * 
 * 🟢 USE ELASTICSEARCH FOR:
 *   1. Full-text search with typo tolerance (Fuzzy matching across order number, phone, customer name, items).
 *   2. Multi-facet filtering on large order volumes (millions of orders in < 5ms).
 *   3. Customer search autocomplete & prefix matching.
 *   4. Geo-Spatial queries (Find orders delivering within 15km of a warehouse).
 * 
 * 🔴 USE POSTGRESQL (RDBMS) FOR:
 *   1. Atomic order creation & ACID Transactions.
 *   2. Financial calculations, balance deduction, payments.
 *   3. Exact-key row locking (SELECT ... FOR UPDATE).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderElasticsearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final OrderElasticsearchRepository orderRepository;

    /**
     * 1️⃣ Save or Update Document in Elasticsearch
     * Called AFTER PostgreSQL transaction commits (usually via Outbox CDC or Kafka event).
     */
    public void syncOrderToElasticsearch(OrderDocument orderDocument) {
        log.info("📥 [Elasticsearch Sync] Indexing order document: id={}, status={}", 
                orderDocument.getId(), orderDocument.getStatus());
        orderRepository.save(orderDocument);
    }

    /**
     * 2️⃣ Complex Multi-Field Fuzzy Search with Typo Tolerance
     * Example: User searches "laptap dell xps 0984" or "ORD-984210"
     */
    public List<OrderDocument> searchOrders(String keyword, String status, String city, int page, int size) {
        NativeQueryBuilder queryBuilder = NativeQuery.builder();

        // Bool Query (must match keywords, filter by exact status & city)
        queryBuilder.withQuery(q -> q.bool(b -> {
            if (keyword != null && !keyword.trim().isEmpty()) {
                b.must(m -> m.multiMatch(mm -> mm
                    .query(keyword.trim())
                    .fields(
                        "orderNumber^8",       // Highest priority boost
                        "customerPhone^5",     // High priority
                        "customerName^4",
                        "itemSummary^3",
                        "deliveryAddress^2"
                    )
                    .fuzziness("AUTO")        // Handles typos like "laptap" -> "laptop"
                    .prefixLength(1)
                ));
            } else {
                b.must(m -> m.matchAll(ma -> ma));
            }

            // High performance filters (cached by Elasticsearch)
            if (status != null && !status.equalsIgnoreCase("ALL")) {
                b.filter(f -> f.term(t -> t.field("status").value(status)));
            }

            if (city != null && !city.equalsIgnoreCase("ALL")) {
                b.filter(f -> f.term(t -> t.field("destinationCity").value(city)));
            }

            return b;
        }));

        // Pagination & Score Sorting
        queryBuilder.withPageable(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "_score", "createdAt")));

        SearchHits<OrderDocument> searchHits = elasticsearchOperations.search(queryBuilder.build(), OrderDocument.class);

        log.info("🔎 Found {} orders in Elasticsearch (took {} ms)", 
                searchHits.getTotalHits(), searchHits.getExecutionDuration() != null ? searchHits.getExecutionDuration().toMillis() : "<1");

        return searchHits.getSearchHits()
                .stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    /**
     * 3️⃣ Geo-Spatial Nearby Orders Query
     * Finds all orders delivering within `radiusKm` from given GPS coordinates.
     */
    public List<OrderDocument> findOrdersNearby(double lat, double lon, double radiusKm) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .filter(f -> f.geoDistance(gd -> gd
                                .field("deliveryGeoPoint")
                                .distance(radiusKm + "km")
                                .location(l -> l.latlon(ll -> ll.lat(lat).lon(lon)))
                        ))
                ))
                .withPageable(PageRequest.of(0, 50))
                .build();

        SearchHits<OrderDocument> searchHits = elasticsearchOperations.search(query, OrderDocument.class);
        return searchHits.getSearchHits().stream().map(SearchHit::getContent).collect(Collectors.toList());
    }
}
