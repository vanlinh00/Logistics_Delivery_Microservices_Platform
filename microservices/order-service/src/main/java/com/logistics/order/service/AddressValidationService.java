package com.logistics.order.service;

import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class AddressValidationService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0|\\+84)[3|5|7|8|9][0-9]{8}$");

    @Getter
    @Builder
    public static class ValidationResult {
        private boolean valid;
        private String message;
        private Double latitude;
        private Double longitude;
        private String formattedAddress;
    }

    public ValidationResult validateAddress(String address, String phone) {
        if (address == null || address.trim().length() < 5) {
            return ValidationResult.builder()
                    .valid(false)
                    .message("Địa chỉ quá ngắn hoặc không hợp lệ (tối thiểu 5 ký tự)")
                    .build();
        }

        if (phone != null && !PHONE_PATTERN.matcher(phone.replaceAll("\\s+", "")).matches()) {
            return ValidationResult.builder()
                    .valid(false)
                    .message("Số điện thoại không đúng định dạng chuẩn (10 chữ số VN)")
                    .build();
        }

        // Mock Geo-coordinates resolve based on common locations
        double lat = 10.7769;
        double lon = 106.7009;
        if (address.toLowerCase().contains("hà nội") || address.toLowerCase().contains("ha noi")) {
            lat = 21.0285;
            lon = 105.8542;
        } else if (address.toLowerCase().contains("đà nẵng") || address.toLowerCase().contains("da nang")) {
            lat = 16.0544;
            lon = 108.2022;
        }

        return ValidationResult.builder()
                .valid(true)
                .message("Địa chỉ và số điện thoại hợp lệ")
                .latitude(lat)
                .longitude(lon)
                .formattedAddress(address.trim())
                .build();
    }
}
