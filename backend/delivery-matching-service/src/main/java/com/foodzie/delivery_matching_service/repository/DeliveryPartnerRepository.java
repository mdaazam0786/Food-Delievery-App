package com.foodzie.delivery_matching_service.repository;

import com.foodzie.delivery_matching_service.data.DeliveryPartner;
import com.foodzie.delivery_matching_service.data.DriverStatus;
import com.foodzie.delivery_matching_service.data.KycStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryPartnerRepository extends MongoRepository<DeliveryPartner, String> {

    /**
     * Fetches profiles for a specific set of driver IDs.
     * Used to bulk-load profiles for all Redis geo candidates in one query
     * instead of N individual lookups.
     */
    List<DeliveryPartner> findAllByIdInAndCurrentStatusAndKycStatus(
            List<String> ids, DriverStatus status, KycStatus kycStatus);
}
