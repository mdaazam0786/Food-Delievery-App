package com.foodzie.delivery_acceptance_service.repository;

import com.foodzie.delivery_acceptance_service.data.DeliveryPartner;
import com.foodzie.delivery_acceptance_service.data.DriverStatus;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Custom implementation of specialized MongoDB update operations.
 * 
 * This class implements DeliveryPartnerRepositoryCustom and is automatically
 * detected and composed into DeliveryPartnerRepository by Spring Data MongoDB.
 * The naming convention (Suffix with "Impl") is important for Spring Data to recognize it.
 */
public class DeliveryPartnerRepositoryImpl implements DeliveryPartnerRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public DeliveryPartnerRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Targeted status update — avoids loading the full entity just to flip one field.
     */
    @Override
    public long updateStatusIfExpected(String driverId, DriverStatus newStatus) {
        Query query = new Query(Criteria.where("_id").is(driverId));
        Update update = new Update().set("currentStatus", newStatus);
        
        var result = mongoTemplate.updateFirst(query, update, DeliveryPartner.class);
        return result.getModifiedCount();
    }
}
