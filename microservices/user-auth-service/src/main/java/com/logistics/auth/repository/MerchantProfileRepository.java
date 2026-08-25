package com.logistics.auth.repository;

import com.logistics.auth.model.MerchantProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantProfileRepository extends JpaRepository<MerchantProfile, UUID> {

    Optional<MerchantProfile> findByUserId(UUID userId);

    Optional<MerchantProfile> findByUser_Username(String username);

    Optional<MerchantProfile> findByTaxCode(String taxCode);

    Page<MerchantProfile> findByCodTier(MerchantProfile.CodTier codTier, Pageable pageable);
}
