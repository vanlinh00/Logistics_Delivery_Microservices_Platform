package com.logistics.auth.service;

import com.logistics.auth.dto.AuthDTOs.CourierKycRequest;
import com.logistics.auth.dto.AuthDTOs.CourierShiftRequest;
import com.logistics.auth.exception.ResourceNotFoundException;
import com.logistics.auth.model.CourierProfile;
import com.logistics.auth.model.User;
import com.logistics.auth.repository.CourierProfileRepository;
import com.logistics.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourierService {

    private final CourierProfileRepository courierProfileRepository;
    private final UserRepository userRepository;

    @Transactional
    public CourierProfile submitKyc(String username, CourierKycRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        CourierProfile profile = courierProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> CourierProfile.builder().user(user).build());

        profile.setCitizenId(request.getCitizenId());
        if (request.getVehicleType() != null) {
            profile.setVehicleType(request.getVehicleType());
        }
        profile.setLicensePlate(request.getLicensePlate());
        profile.setAssignedHubId(request.getAssignedHubId());
        if (request.getMaxCapacityKg() != null) {
            profile.setMaxCapacityKg(request.getMaxCapacityKg());
        }
        profile.setKycStatus(CourierProfile.KycStatus.APPROVED); // Auto-approve or queue for review

        log.info("Courier KYC profile updated for: {}", username);
        return courierProfileRepository.save(profile);
    }

    @Transactional
    public CourierProfile toggleShift(String username, CourierShiftRequest request) {
        CourierProfile profile = courierProfileRepository.findByUser_Username(username)
                .orElseThrow(() -> new ResourceNotFoundException("Courier profile not found for user: " + username));

        profile.setIsOnline(request.getIsOnline());
        return courierProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public CourierProfile getCourierProfile(UUID userId) {
        return courierProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier profile not found for userId: " + userId));
    }

    @Transactional(readOnly = true)
    public List<CourierProfile> getActiveCouriersByHub(String hubId) {
        return courierProfileRepository.findByAssignedHubIdAndIsOnlineTrue(hubId);
    }
}
