package com.logistics.order.elasticsearch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 🔍 OrderElasticsearchRepository:
 * Spring Data Elasticsearch repository providing instant CRUD & custom query methods.
 */
@Repository
public interface OrderElasticsearchRepository extends ElasticsearchRepository<OrderDocument, String> {

    // Simple keyword queries
    List<OrderDocument> findByCustomerId(String customerId);

    List<OrderDocument> findByCustomerPhone(String customerPhone);

    List<OrderDocument> findByStatus(String status);

    Page<OrderDocument> findByDestinationCity(String destinationCity, Pageable pageable);
}
