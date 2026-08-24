package com.logistics.tracking.repository;

import com.logistics.tracking.document.ParcelIndexDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Highlight;
import org.springframework.data.elasticsearch.annotations.HighlightField;
import org.springframework.data.elasticsearch.annotations.HighlightParameters;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data Elasticsearch Repository for high-throughput logistics parcel search.
 */
@Repository
public interface ParcelElasticsearchRepository extends ElasticsearchRepository<ParcelIndexDocument, String> {

    List<ParcelIndexDocument> findByCurrentStatus(String status);

    List<ParcelIndexDocument> findByDestinationCity(String destinationCity);

    List<ParcelIndexDocument> findByRecipientPhone(String recipientPhone);

    List<ParcelIndexDocument> findBySenderPhone(String senderPhone);

    /**
     * Autocomplete query matching prefix in tracking number, recipient, sender, or items
     */
    @Query("{\"bool\": {\"should\": [" +
           "{\"match\": {\"trackingNumber.suggest\": {\"query\": \"?0\", \"boost\": 5.0}}}," +
           "{\"match\": {\"recipientName.autocomplete\": {\"query\": \"?0\", \"boost\": 3.0}}}," +
           "{\"match\": {\"senderName.autocomplete\": {\"query\": \"?0\", \"boost\": 2.0}}}," +
           "{\"prefix\": {\"recipientPhone\": \"?0\"}}" +
           "]}}")
    List<ParcelIndexDocument> suggestAutocomplete(String prefix, Pageable pageable);

    /**
     * Multi-field fuzzy search across all parcel attributes with typo tolerance
     */
    @Query("{\"bool\": {\"must\": [" +
           "{\"multi_match\": {" +
           "  \"query\": \"?0\"," +
           "  \"fields\": [\"trackingNumber^5\", \"recipientName^3\", \"recipientPhone^3\", \"itemDescription^2\", \"recipientAddress\", \"senderName\", \"currentHub\"]," +
           "  \"fuzziness\": \"AUTO\"," +
           "  \"prefix_length\": 1" +
           "}}" +
           "]}}")
    @Highlight(
        fields = {
            @HighlightField(name = "recipientName"),
            @HighlightField(name = "itemDescription"),
            @HighlightField(name = "recipientAddress")
        },
        parameters = @HighlightParameters(preTags = "<mark class='bg-amber-500/20 text-amber-300 font-semibold px-1 rounded'>", postTags = "</mark>")
    )
    SearchHits<ParcelIndexDocument> searchFuzzy(String keyword, Pageable pageable);
}
