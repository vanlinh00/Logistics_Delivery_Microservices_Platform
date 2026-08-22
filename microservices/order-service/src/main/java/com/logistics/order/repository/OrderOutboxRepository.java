package com.logistics.order.repository;

import com.logistics.order.model.OrderOutbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, UUID> {
    @Query("SELECT o FROM OrderOutbox o WHERE o.processed = false ORDER BY o.createdAt ASC")
    List<OrderOutbox> findUnprocessedEvents(Pageable pageable);
}
