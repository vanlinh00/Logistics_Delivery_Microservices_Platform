package com.logistics.auth.service;

import com.logistics.auth.dto.AuthDTOs.*;
import com.logistics.auth.exception.InvalidCredentialsException;
import com.logistics.auth.exception.ResourceNotFoundException;
import com.logistics.auth.model.CourierProfile;
import com.logistics.auth.model.MerchantProfile;
import com.logistics.auth.model.User;
import com.logistics.auth.repository.CourierProfileRepository;
import com.logistics.auth.repository.MerchantProfileRepository;
import com.logistics.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final CourierProfileRepository courierProfileRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    @Transactional(readOnly = true)
    public UserSummaryDTO getUserSummary(String username) {
        User user = getUserByUsername(username);
        CourierProfile courier = courierProfileRepository.findByUserId(user.getId()).orElse(null);
        MerchantProfile merchant = merchantProfileRepository.findByUserId(user.getId()).orElse(null);

        return UserSummaryDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .active(user.getActive())
                .mfaEnabled(user.getMfaEnabled())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .courierProfile(courier)
                .merchantProfile(merchant)
                .build();
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = getUserByUsername(username);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())
                && !request.getOldPassword().equals(user.getPasswordHash())) {
            throw new InvalidCredentialsException("Mật khẩu hiện tại không chính xác");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password updated successfully for user: {}", username);
    }

    @Transactional
    public User toggleUserStatus(UUID userId, boolean active) {
        User user = getUserById(userId);
        user.setActive(active);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public AuthStatsResponse getStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countActiveUsers();
        long couriers = userRepository.findByRole(User.UserRole.ROLE_COURIER).size();
        long merchants = userRepository.findByRole(User.UserRole.ROLE_MERCHANT).size();
        long customers = userRepository.findByRole(User.UserRole.ROLE_CUSTOMER).size();
        long activeCouriers = courierProfileRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsOnline()))
                .count();

        return AuthStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalCouriers(couriers)
                .totalMerchants(merchants)
                .totalCustomers(customers)
                .activeCouriersOnline(activeCouriers)
                .build();
    }
}
