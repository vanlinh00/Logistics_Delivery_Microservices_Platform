package com.logistics.auth.controller;

import com.logistics.auth.constant.ApiPath;
import com.logistics.auth.dto.ApiResponse;
import com.logistics.auth.dto.RolePermissionDTOs.*;
import com.logistics.auth.service.KeycloakRolePermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiPath.ROLES_BASE)
@RequiredArgsConstructor
@Tag(name = "Keycloak Roles & Permissions Governance", description = "Enterprise RBAC, Client/Realm Roles, Composites, and User-Role Mappings")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class RolePermissionController {

    private final KeycloakRolePermissionService rolePermissionService;

    // =========================================================================
    // REALM ROLES
    // =========================================================================

    @PostMapping
    @Operation(summary = "Create new Realm-level role with custom attributes")
    public ResponseEntity<ApiResponse<RoleRepresentationDTO>> createRealmRole(
            @Valid @RequestBody CreateRoleRequest request) {
        RoleRepresentationDTO role = rolePermissionService.createRealmRole(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(role, "Realm role created successfully"));
    }

    @GetMapping
    @Operation(summary = "Fetch all Realm roles with optional search and pagination")
    public ResponseEntity<ApiResponse<List<RoleRepresentationDTO>>> getAllRealmRoles(
            @RequestParam(name = "first", defaultValue = "0") int first,
            @RequestParam(name = "max", defaultValue = "100") int max,
            @RequestParam(name = "search", required = false) String search) {
        List<RoleRepresentationDTO> roles = rolePermissionService.getAllRealmRoles(first, max, search);
        return ResponseEntity.ok(ApiResponse.ok(roles, "Roles retrieved successfully"));
    }

    @GetMapping(ApiPath.ROLE_BY_NAME)
    @Operation(summary = "Get specific Realm role details by name")
    public ResponseEntity<ApiResponse<RoleRepresentationDTO>> getRealmRole(
            @PathVariable("roleName") String roleName) {
        RoleRepresentationDTO role = rolePermissionService.getRealmRole(roleName);
        return ResponseEntity.ok(ApiResponse.ok(role, "Role details retrieved"));
    }

    @PutMapping(ApiPath.ROLE_BY_NAME)
    @Operation(summary = "Update Realm role description and attributes")
    public ResponseEntity<ApiResponse<RoleRepresentationDTO>> updateRealmRole(
            @PathVariable("roleName") String roleName,
            @Valid @RequestBody UpdateRoleRequest request) {
        RoleRepresentationDTO updated = rolePermissionService.updateRealmRole(roleName, request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Role updated successfully"));
    }

    @DeleteMapping(ApiPath.ROLE_BY_NAME)
    @Operation(summary = "Delete Realm role safely")
    public ResponseEntity<ApiResponse<Void>> deleteRealmRole(
            @PathVariable("roleName") String roleName) {
        rolePermissionService.deleteRealmRole(roleName);
        return ResponseEntity.ok(ApiResponse.ok(null, "Role deleted successfully"));
    }

    // =========================================================================
    // COMPOSITE ROLES
    // =========================================================================

    @PostMapping(ApiPath.ROLE_COMPOSITES)
    @Operation(summary = "Add child roles to a composite parent role")
    public ResponseEntity<ApiResponse<List<RoleRepresentationDTO>>> addCompositeRoles(
            @PathVariable("roleName") String roleName,
            @RequestBody List<String> childRoleNames) {
        rolePermissionService.addCompositeRoles(roleName, childRoleNames);
        List<RoleRepresentationDTO> composites = rolePermissionService.getRoleComposites(roleName);
        return ResponseEntity.ok(ApiResponse.ok(composites, "Composite child roles attached"));
    }

    @GetMapping(ApiPath.ROLE_COMPOSITES)
    @Operation(summary = "List all child composite roles of a parent role")
    public ResponseEntity<ApiResponse<List<RoleRepresentationDTO>>> getRoleComposites(
            @PathVariable("roleName") String roleName) {
        List<RoleRepresentationDTO> composites = rolePermissionService.getRoleComposites(roleName);
        return ResponseEntity.ok(ApiResponse.ok(composites, "Composite child roles retrieved"));
    }

    @DeleteMapping(ApiPath.ROLE_COMPOSITES)
    @Operation(summary = "Remove child roles from a composite parent role")
    public ResponseEntity<ApiResponse<Void>> removeCompositeRoles(
            @PathVariable("roleName") String roleName,
            @RequestBody List<String> childRoleNames) {
        rolePermissionService.removeCompositeRoles(roleName, childRoleNames);
        return ResponseEntity.ok(ApiResponse.ok(null, "Composite child roles removed"));
    }

    // =========================================================================
    // CLIENT ROLES
    // =========================================================================

    @PostMapping(ApiPath.CLIENT_ROLES)
    @Operation(summary = "Create a Client-level role")
    public ResponseEntity<ApiResponse<RoleRepresentationDTO>> createClientRole(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody CreateRoleRequest request) {
        RoleRepresentationDTO role = rolePermissionService.createClientRole(clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(role, "Client role created successfully"));
    }

    @GetMapping(ApiPath.CLIENT_ROLES)
    @Operation(summary = "List all Client roles for a specific client")
    public ResponseEntity<ApiResponse<List<RoleRepresentationDTO>>> getClientRoles(
            @PathVariable("clientId") String clientId) {
        List<RoleRepresentationDTO> roles = rolePermissionService.getClientRoles(clientId);
        return ResponseEntity.ok(ApiResponse.ok(roles, "Client roles retrieved"));
    }

    @GetMapping(ApiPath.CLIENT_ROLE_BY_NAME)
    @Operation(summary = "Get specific Client role by name")
    public ResponseEntity<ApiResponse<RoleRepresentationDTO>> getClientRole(
            @PathVariable("clientId") String clientId,
            @PathVariable("roleName") String roleName) {
        RoleRepresentationDTO role = rolePermissionService.getClientRole(clientId, roleName);
        return ResponseEntity.ok(ApiResponse.ok(role, "Client role retrieved"));
    }

    @DeleteMapping(ApiPath.CLIENT_ROLE_BY_NAME)
    @Operation(summary = "Delete Client role")
    public ResponseEntity<ApiResponse<Void>> deleteClientRole(
            @PathVariable("clientId") String clientId,
            @PathVariable("roleName") String roleName) {
        rolePermissionService.deleteClientRole(clientId, roleName);
        return ResponseEntity.ok(ApiResponse.ok(null, "Client role deleted successfully"));
    }

    // =========================================================================
    // USER ROLE MAPPINGS
    // =========================================================================

    @PostMapping(ApiPath.ROLE_USER_ASSIGN)
    @Operation(summary = "Assign a Realm role to a user")
    public ResponseEntity<ApiResponse<Void>> assignRoleToUser(
            @PathVariable("userId") String userId,
            @RequestParam("roleName") String roleName) {
        rolePermissionService.assignRoleToUser(userId, roleName);
        return ResponseEntity.ok(ApiResponse.ok(null, "Role assigned to user successfully"));
    }

    @DeleteMapping(ApiPath.ROLE_USER_REMOVE)
    @Operation(summary = "Remove a Realm role from a user")
    public ResponseEntity<ApiResponse<Void>> removeRoleFromUser(
            @PathVariable("userId") String userId,
            @RequestParam("roleName") String roleName) {
        rolePermissionService.removeRoleFromUser(userId, roleName);
        return ResponseEntity.ok(ApiResponse.ok(null, "Role removed from user successfully"));
    }

    @GetMapping(ApiPath.ROLE_USER_MAPPINGS)
    @Operation(summary = "Get all Realm roles assigned to a user")
    public ResponseEntity<ApiResponse<List<RoleRepresentationDTO>>> getUserRoles(
            @PathVariable("userId") String userId) {
        List<RoleRepresentationDTO> roles = rolePermissionService.getUserRoles(userId);
        return ResponseEntity.ok(ApiResponse.ok(roles, "User role mappings retrieved"));
    }

    @GetMapping(ApiPath.ROLE_USERS)
    @Operation(summary = "Get all users assigned to a specific Realm role")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRoleUsers(
            @PathVariable("roleName") String roleName) {
        List<Map<String, Object>> users = rolePermissionService.getRoleUsers(roleName);
        return ResponseEntity.ok(ApiResponse.ok(users, "Role member users retrieved"));
    }
}
