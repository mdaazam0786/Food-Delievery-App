package com.foodzie.delivery_service.repository;

import com.foodzie.delivery_service.data.DriverEarnings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DriverEarningsRepository extends JpaRepository<DriverEarnings, Long> {

    Optional<DriverEarnings> findByDriverId(String driverId);
}
