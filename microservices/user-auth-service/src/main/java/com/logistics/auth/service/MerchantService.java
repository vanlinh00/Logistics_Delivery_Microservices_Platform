package com.logistics.auth.service;

import com.logistics.auth.dto.AuthDTOs.MerchantProfileRequest;
import com.logistics.auth.exception.ResourceNotFoundException;
import com.logistics.auth.model.MerchantProfile;
import com.logistics.auth.model.User;
import com.logistics.auth.repository.MerchantProfileRepository;
import com.logistics.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantService {

    private final MerchantProfileRepository merchantProfileRepository;
    private final UserRepository userRepository;

    @Transactional
    public MerchantProfile saveMerchantProfile(String username, MerchantProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        MerchantProfile profile = merchantProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> MerchantProfile.builder().user(user).build());

        profile.setShopName(request.getShopName());
        profile.setTaxCode(request.getTaxCode());
        profile.setWarehouseAddress(request.getWarehouseAddress());
        profile.setBankAccount(request.getBankAccount());
        profile.setBankName(request.getBankName());
        if (request.getCodTier() != null) {
            profile.setCodTier(request.getCodTier());
        }

        log.info("Merchant profile saved for user: {}", username);
        return merchantProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public MerchantProfile getMerchantProfile(UUID userId) {
        return merchantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant profile not found for userId: " + userId));
    }
}
