package com.foodzie.delivery_matching_service.config;

import com.foodzie.delivery_matching_service.data.DeliveryPartner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.stereotype.Component;

/**
 * Initializes MongoDB indexes for the delivery-matching-service.
 * 
 * Indexes are critical for query performance:
 * - Query: findAllByIdInAndCurrentStatusAndKycStatus(ids, IDLE, APPROVED)
 * 
 * These indexes are created at application startup.
 * If indexes already exist, MongoDB silently ignores the creation request.
 * 
 * Production monitoring:
 * - Monitor index size growth with: db.delivery_partner.stats()
 * - Rebuild indexes periodically with: db.delivery_partner.reIndex()
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;

    /**
     * Runs after Spring application context is fully initialized.
     * Creates all necessary MongoDB indexes for optimal query performance.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void createIndexes() {
        try {
            log.info("Starting MongoDB index initialization for DeliveryPartner collection");

            // Composite index for the main driver matching query
            // Query: findAllByIdInAndCurrentStatusAndKycStatus(ids, IDLE, APPROVED)
            // This index covers: _id IN (...) AND currentStatus = IDLE AND kycStatus = APPROVED
            mongoTemplate.indexOps(DeliveryPartner.class)
                    .ensureIndex(new Index()
                            .on("_id", org.springframework.data.domain.Sort.Direction.ASC)
                            .on("currentStatus", org.springframework.data.domain.Sort.Direction.ASC)
                            .on("kycStatus", org.springframework.data.domain.Sort.Direction.ASC));
            log.info("✓ Created composite index on _id, currentStatus, kycStatus");

            // Index for filtering by status and KYC only (useful for admin queries)
            mongoTemplate.indexOps(DeliveryPartner.class)
                    .ensureIndex(new Index()
                            .on("currentStatus", org.springframework.data.domain.Sort.Direction.ASC)
                            .on("kycStatus", org.springframework.data.domain.Sort.Direction.ASC));
            log.info("✓ Created index on currentStatus, kycStatus");

            // Index for driver status queries (driver status changes frequently)
            mongoTemplate.indexOps(DeliveryPartner.class)
                    .ensureIndex(new Index()
                            .on("currentStatus", org.springframework.data.domain.Sort.Direction.ASC));
            log.info("✓ Created index on currentStatus");

            // Index for KYC status queries (useful for compliance/audit)
            mongoTemplate.indexOps(DeliveryPartner.class)
                    .ensureIndex(new Index()
                            .on("kycStatus", org.springframework.data.domain.Sort.Direction.ASC));
            log.info("✓ Created index on kycStatus");

            log.info("MongoDB index initialization completed successfully");

        } catch (Exception e) {
            log.error("Failed to create MongoDB indexes: {}", e.getMessage(), e);
            // Don't throw exception - allow application to continue with degraded performance
            // Indexes will be created on next restart or by admin intervention
        }
    }
}
