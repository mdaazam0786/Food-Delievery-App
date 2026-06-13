package com.foodzie.delivery_acceptance_service.repository;

import com.foodzie.delivery_acceptance_service.data.DeliveryPartner;
import com.foodzie.delivery_acceptance_service.data.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, String> {

    /**
     * Targeted status update — avoids loading the full entity just to flip one field.
     * Called after a driver wins the distributed lock.
     */
    @Modifying
    @Query("UPDATE DeliveryPartner d SET d.currentStatus = :newStatus WHERE d.id = :driverId AND d.currentStatus = :expectedStatus")
    int updateStatusIfExpected(
            @Param("driverId") String driverId,
            @Param("expectedStatus") DriverStatus expectedStatus,
            @Param("newStatus") DriverStatus newStatus
    );
}
