package com.logistics.auth.config;

import com.logistics.auth.model.Permission;
import com.logistics.auth.model.Role;
import com.logistics.auth.repository.PermissionRepository;
import com.logistics.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class RbacDataInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            initPermissionsAndRoles();
        } catch (Exception e) {
            log.warn("RBAC Data Initializer encountered an error or data already present: {}", e.getMessage());
        }
    }

    private void initPermissionsAndRoles() {
        // 1. Define all standard permissions
        Map<String, String[]> permissionsDef = new LinkedHashMap<>();
        // code -> [name, category, description]
        permissionsDef.put("orders:read", new String[]{"View Orders", "ORDERS", "Permission to view orders"});
        permissionsDef.put("orders:write", new String[]{"Create & Edit Orders", "ORDERS", "Permission to create and modify orders"});
        permissionsDef.put("orders:delete", new String[]{"Delete Orders", "ORDERS", "Permission to delete orders"});
        permissionsDef.put("orders:create", new String[]{"Create Orders", "ORDERS", "Permission to place new delivery orders"});
        permissionsDef.put("orders:cancel", new String[]{"Cancel Orders", "ORDERS", "Permission to cancel orders"});
        permissionsDef.put("orders:read:self", new String[]{"View Own Orders", "ORDERS", "Permission to view customer's own orders"});
        permissionsDef.put("orders:status:update", new String[]{"Update Order Status", "ORDERS", "Permission to update order delivery status"});
        permissionsDef.put("orders:pod:upload", new String[]{"Upload Proof of Delivery", "ORDERS", "Permission to upload POD image / signature"});
        permissionsDef.put("orders:assign", new String[]{"Assign Orders", "ORDERS", "Permission to assign orders to couriers"});

        permissionsDef.put("fleet:read", new String[]{"View Fleet", "FLEET", "Permission to view fleet and courier status"});
        permissionsDef.put("fleet:dispatch", new String[]{"Dispatch Fleet", "FLEET", "Permission to execute dispatch operations"});
        permissionsDef.put("fleet:status:toggle", new String[]{"Toggle Driver Status", "FLEET", "Permission for courier to go online/offline"});
        permissionsDef.put("fleet:route:optimize", new String[]{"Optimize Routes", "FLEET", "Permission to run dispatch route optimization"});

        permissionsDef.put("tracking:read", new String[]{"Track Delivery", "TRACKING", "Permission to read parcel tracking milestones"});
        permissionsDef.put("tracking:push:location", new String[]{"Push GPS Location", "TRACKING", "Permission for driver to send live GPS coordinates"});

        permissionsDef.put("merchant:profile:manage", new String[]{"Manage Merchant Profile", "MERCHANT", "Permission to configure store and COD preferences"});
        permissionsDef.put("inventory:sync", new String[]{"Sync Inventory", "MERCHANT", "Permission to sync store inventory"});

        permissionsDef.put("users:read", new String[]{"View Users", "USERS", "Permission to list users"});
        permissionsDef.put("users:write", new String[]{"Create & Edit Users", "USERS", "Permission to edit users"});
        permissionsDef.put("users:admin", new String[]{"Administer Users", "USERS", "Full user administration"});

        permissionsDef.put("analytics:view", new String[]{"View Analytics", "SYSTEM", "Permission to view executive KPI dashboards"});
        permissionsDef.put("system:manage", new String[]{"Manage System", "SYSTEM", "Permission for full infrastructure management"});

        Map<String, Permission> permissionMap = new HashMap<>();

        for (Map.Entry<String, String[]> entry : permissionsDef.entrySet()) {
            String code = entry.getKey();
            String[] meta = entry.getValue();
            Permission perm = permissionRepository.findByCode(code).orElseGet(() -> {
                Permission p = Permission.builder()
                        .code(code)
                        .name(meta[0])
                        .category(meta[1])
                        .description(meta[2])
                        .build();
                return permissionRepository.save(p);
            });
            permissionMap.put(code, perm);
        }

        // 2. Define role mappings
        Map<String, List<String>> roleMappings = new LinkedHashMap<>();
        roleMappings.put("ROLE_ADMIN", List.of(
                "orders:read", "orders:write", "orders:delete",
                "fleet:read", "fleet:dispatch",
                "users:read", "users:write", "users:admin",
                "analytics:view", "system:manage"
        ));
        roleMappings.put("ROLE_COURIER", List.of(
                "orders:read", "orders:status:update", "orders:pod:upload",
                "fleet:status:toggle", "tracking:push:location"
        ));
        roleMappings.put("ROLE_MERCHANT", List.of(
                "orders:read", "orders:create", "orders:cancel",
                "merchant:profile:manage", "inventory:sync"
        ));
        roleMappings.put("ROLE_DISPATCHER", List.of(
                "orders:read", "orders:assign", "fleet:read", "fleet:route:optimize"
        ));
        roleMappings.put("ROLE_CUSTOMER", List.of(
                "orders:read:self", "orders:create", "tracking:read"
        ));

        Map<String, String> roleNames = Map.of(
                "ROLE_ADMIN", "System Administrator",
                "ROLE_COURIER", "Courier Driver",
                "ROLE_MERCHANT", "Merchant Partner",
                "ROLE_DISPATCHER", "Dispatch Operator",
                "ROLE_CUSTOMER", "End Customer"
        );

        for (Map.Entry<String, List<String>> roleEntry : roleMappings.entrySet()) {
            String roleCode = roleEntry.getKey();
            List<String> permCodes = roleEntry.getValue();

            Role role = roleRepository.findByCodeWithPermissions(roleCode).orElseGet(() ->
                    Role.builder()
                            .code(roleCode)
                            .name(roleNames.getOrDefault(roleCode, roleCode))
                            .description("Default system role for " + roleCode)
                            .permissions(new HashSet<>())
                            .build()
            );

            Set<Permission> targetPerms = new HashSet<>();
            for (String pCode : permCodes) {
                Permission p = permissionMap.get(pCode);
                if (p != null) {
                    targetPerms.add(p);
                }
            }

            role.setPermissions(targetPerms);
            roleRepository.save(role);
        }

        log.info("RBAC database tables (roles, permissions, role_permissions) initialized successfully.");
    }
}
