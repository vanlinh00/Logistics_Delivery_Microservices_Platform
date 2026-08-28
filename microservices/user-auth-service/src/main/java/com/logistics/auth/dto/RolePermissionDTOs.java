package com.logistics.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public final class RolePermissionDTOs {

    private RolePermissionDTOs() {}

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRoleRequest {
        @NotBlank(message = "Role name is required")
        @Pattern(regexp = "^[A-Za-z0-9_\\-\\.:]+$", message = "Role name contains invalid characters")
        private String name;

        private String description;
        private Boolean composite;
        private Map<String, List<String>> attributes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRoleRequest {
        private String description;
        private Map<String, List<String>> attributes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleRepresentationDTO {
        private String id;
        private String name;
        private String description;
        private Boolean composite;
        private Boolean clientRole;
        private String containerId;
        private Map<String, List<String>> attributes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeRoleRequest {
        @NotBlank(message = "Parent role name is required")
        private String parentRoleName;

        private List<String> childRoleNames;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRoleAssignmentRequest {
        @NotBlank(message = "User ID is required")
        private String userId;

        private List<String> roleNames;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionDTO {
        private String id;
        private String name;
        private String description;
        private String resource;
        private String scope;
    }
}
