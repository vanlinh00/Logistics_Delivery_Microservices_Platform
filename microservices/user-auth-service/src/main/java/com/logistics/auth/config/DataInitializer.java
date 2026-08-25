package com.logistics.auth.config;

import com.logistics.auth.model.CourierProfile;
import com.logistics.auth.model.MerchantProfile;
import com.logistics.auth.model.User;
import com.logistics.auth.repository.CourierProfileRepository;
import com.logistics.auth.repository.MerchantProfileRepository;
import com.logistics.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CourierProfileRepository courierProfileRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("UserAuthService: Users already seeded, skipping initial data load.");
            return;
        }

        log.info("UserAuthService: Seeding initial IAM users and profiles...");

        // 1. Admin User
        User admin = User.builder()
                .username("admin")
                .email("admin@logistics.vn")
                .passwordHash(passwordEncoder.encode("admin123"))
                .fullName("System Administrator")
                .phone("0901234567")
                .role(User.UserRole.ROLE_ADMIN)
                .active(true)
                .mfaEnabled(false)
                .build();
        userRepository.save(admin);

        // 2. Courier User
        User courier = User.builder()
                .username("courier_hung")
                .email("hung.nguyen@logistics.vn")
                .passwordHash(passwordEncoder.encode("courier123"))
                .fullName("Nguyễn Văn Hùng")
                .phone("0988776655")
                .role(User.UserRole.ROLE_COURIER)
                .active(true)
                .mfaEnabled(false)
                .build();
        User savedCourier = userRepository.save(courier);

        CourierProfile courierProfile = CourierProfile.builder()
                .user(savedCourier)
                .citizenId("079095012345")
                .vehicleType(CourierProfile.VehicleType.MOTORBIKE)
                .licensePlate("59-X1 888.99")
                .assignedHubId("HUB-TAN-BINH-01")
                .kycStatus(CourierProfile.KycStatus.APPROVED)
                .isOnline(true)
                .rating(4.9)
                .totalDeliveries(1280)
                .maxCapacityKg(35.0)
                .build();
        courierProfileRepository.save(courierProfile);

        // 3. Merchant User
        User merchant = User.builder()
                .username("merchant_long")
                .email("long.tran@fashionstore.vn")
                .passwordHash(passwordEncoder.encode("merchant123"))
                .fullName("Trần Hoàng Long")
                .phone("0912348899")
                .role(User.UserRole.ROLE_MERCHANT)
                .active(true)
                .mfaEnabled(false)
                .build();
        User savedMerchant = userRepository.save(merchant);

        MerchantProfile merchantProfile = MerchantProfile.builder()
                .user(savedMerchant)
                .shopName("Long Fashion & Accessories")
                .taxCode("0315998877")
                .warehouseAddress("123 Lê Lợi, Phường Bến Thành, Quận 1, TP.HCM")
                .bankAccount("19034567890123")
                .bankName("Techcombank")
                .codTier(MerchantProfile.CodTier.VIP_FAST_PAYOUT)
                .discountRate(0.08)
                .monthlyShipmentVolume(4500L)
                .build();
        merchantProfileRepository.save(merchantProfile);

        // 4. Dispatcher User
        User dispatcher = User.builder()
                .username("dispatcher_minh")
                .email("minh.dispatcher@logistics.vn")
                .passwordHash(passwordEncoder.encode("dispatch123"))
                .fullName("Lê Quang Minh")
                .phone("0933445566")
                .role(User.UserRole.ROLE_DISPATCHER)
                .active(true)
                .mfaEnabled(false)
                .build();
        userRepository.save(dispatcher);

        // 5. Customer User
        User customer = User.builder()
                .username("customer_linh")
                .email("linh.nguyen@gmail.com")
                .passwordHash(passwordEncoder.encode("cust123"))
                .fullName("Nguyễn Thùy Linh")
                .phone("0944556677")
                .role(User.UserRole.ROLE_CUSTOMER)
                .active(true)
                .mfaEnabled(false)
                .build();
        userRepository.save(customer);

        log.info("UserAuthService: Successfully seeded 5 users with roles, profiles, and encrypted credentials.");
    }
}
