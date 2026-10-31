package com.foodzie.delivery_acceptance_service.repository;

import com.foodzie.delivery_acceptance_service.data.DeliveryPartner;
import com.foodzie.delivery_acceptance_service.data.DriverStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryPartnerRepository extends MongoRepository<DeliveryPartner, String>, DeliveryPartnerRepositoryCustom {

}
