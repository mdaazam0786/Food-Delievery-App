package com.foodzie.delivery_acceptance_service.repository;

import com.foodzie.delivery_acceptance_service.data.DriverStatus;

/**
 * Custom repository interface for specialized update operations.
 */
public interface DeliveryPartnerRepositoryCustom {

    /**
     * Targeted status update — avoids loading the full entity just to flip one field.
     * Called after a driver wins the distributed lock.
     * 
     * @param driverId the driver's ID
     * @param newStatus the new status to set
     * @return the number of documents updated
     */
    long updateStatusIfExpected(String driverId, DriverStatus newStatus);
}
