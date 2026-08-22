package com.logistics.fulfillment.repository;

import com.logistics.fulfillment.model.HubTransitRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HubTransitRepository extends JpaRepository<HubTransitRecord, UUID> {
    List<HubTransitRecord> findByTrackingNumberOrderByScannedAtDesc(String trackingNumber);
}
