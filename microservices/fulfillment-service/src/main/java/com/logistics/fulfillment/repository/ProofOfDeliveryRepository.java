package com.logistics.fulfillment.repository;

import com.logistics.fulfillment.model.ProofOfDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProofOfDeliveryRepository extends JpaRepository<ProofOfDelivery, UUID> {
    Optional<ProofOfDelivery> findByTrackingNumber(String trackingNumber);
}
