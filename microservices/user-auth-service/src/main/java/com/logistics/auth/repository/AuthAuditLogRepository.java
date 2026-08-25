package com.logistics.auth.repository;

import com.logistics.auth.model.AuthAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, UUID> {

    List<AuthAuditLog> findTop20ByUsernameOrderByTimestampDesc(String username);

    Page<AuthAuditLog> findByUsernameOrderByTimestampDesc(String username, Pageable pageable);

    Page<AuthAuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
}
