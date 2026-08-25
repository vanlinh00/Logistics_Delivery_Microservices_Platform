package com.logistics.auth.repository;

import com.logistics.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByCode(String code);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.code = :code")
    Optional<Role> findByCodeWithPermissions(@Param("code") String code);
}
