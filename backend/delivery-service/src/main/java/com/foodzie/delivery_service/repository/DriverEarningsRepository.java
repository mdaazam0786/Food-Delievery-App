package com.foodzie.delivery_service.repository;

import com.foodzie.delivery_service.data.DriverEarnings;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DriverEarningsRepository extends MongoRepository<DriverEarnings, String> {

    Optional<DriverEarnings> findByDriverId(String driverId);
}
