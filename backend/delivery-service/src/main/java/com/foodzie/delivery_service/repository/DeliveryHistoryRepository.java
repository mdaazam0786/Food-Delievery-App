package com.foodzie.delivery_service.repository;

import com.foodzie.delivery_service.data.DeliveryHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryHistoryRepository extends JpaRepository<DeliveryHistory, Long> {

    /**
     * Find all deliveries for a driver, paginated and sorted by completed_at DESC
     */
    Page<DeliveryHistory> findByDriverIdOrderByCompletedAtDesc(String driverId, Pageable pageable);
}
