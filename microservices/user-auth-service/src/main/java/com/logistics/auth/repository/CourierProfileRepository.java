package com.logistics.auth.repository;

import com.logistics.auth.model.CourierProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourierProfileRepository extends JpaRepository<CourierProfile, UUID> {

    Optional<CourierProfile> findByUserId(UUID userId);

    Optional<CourierProfile> findByUser_Username(String username);

    List<CourierProfile> findByAssignedHubId(String hubId);

    List<CourierProfile> findByAssignedHubIdAndIsOnlineTrue(String hubId);

    List<CourierProfile> findByKycStatus(CourierProfile.KycStatus kycStatus);

    Page<CourierProfile> findByKycStatus(CourierProfile.KycStatus kycStatus, Pageable pageable);
}
