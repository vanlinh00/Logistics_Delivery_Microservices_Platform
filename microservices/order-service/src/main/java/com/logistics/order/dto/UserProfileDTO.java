package com.logistics.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private UUID id;
    private String keycloakId;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String role;
    private Boolean active;
}
