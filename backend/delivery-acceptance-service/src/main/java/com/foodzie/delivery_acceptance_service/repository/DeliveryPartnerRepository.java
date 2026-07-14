package com.foodzie.delivery_acceptance_service.repository;

import com.foodzie.delivery_acceptance_service.data.DeliveryPartner;
import com.foodzie.delivery_acceptance_service.data.DriverStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryPartnerRepository extends MongoRepository<DeliveryPartner, String> {

    /**
     * Targeted status update — avoids loading the full entity just to flip one field.
     * Called after a driver wins the distributed lock.
     */
    @Update("{ '$set': { 'currentStatus': ?1 } }")
    void updateStatusIfExpected(
            String driverId,
            DriverStatus newStatus
    );
}
