package com.foodzie.delivery_service.service;

import com.foodzie.delivery_service.data.DeliveryPartner;
import com.foodzie.delivery_service.data.DriverStatus;
import com.foodzie.delivery_service.dto.DriverResponse;
import com.foodzie.delivery_service.dto.RegisterDriverRequest;
import com.foodzie.delivery_service.dto.UpdateDriverStatusRequest;
import com.foodzie.delivery_service.exception.DriverNotFoundException;
import com.foodzie.delivery_service.repository.DeliveryPartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles slow-moving driver lifecycle operations against MySQL.
 * Location updates are handled separately by DriverLocationService (Redis only).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverManagementService {

    private final DeliveryPartnerRepository driverRepository;
    private final DriverLocationService locationService;
    private final DriverEarningsService earningsService;

    @Transactional
    public DriverResponse registerDriver(RegisterDriverRequest request) {
        String id = generateDriverId();

        DeliveryPartner driver = DeliveryPartner.builder()
                .id(id)
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .vehicleType(request.getVehicleType())
                .cityZone(request.getCityZone() != null ? request.getCityZone() : "default")
                .build();

        DeliveryPartner saved = driverRepository.save(driver);
        
        // Initialize earnings record for the new driver
        earningsService.initializeEarningsForDriver(saved.getId());
        
        log.info("Driver registered: id={} name={} email={}", saved.getId(), saved.getFullName(), saved.getEmail());
        return toResponse(saved);
    }

    public DriverResponse getDriver(String driverId) {
        return toResponse(findDriver(driverId));
    }

    /**
     * Find driver by email. Used by frontend auth to look up driver profile using JWT email.
     */
    public DriverResponse getDriverByEmail(String email) {
        DeliveryPartner driver = driverRepository.findByEmail(email)
                .orElseThrow(() -> new DriverNotFoundException("Driver not found for email: " + email));
        return toResponse(driver);
    }

    @Transactional
    public DriverResponse updateStatus(String driverId, UpdateDriverStatusRequest request) {
        DeliveryPartner driver = findDriver(driverId);
        DriverStatus previous = driver.getCurrentStatus();
        driver.setCurrentStatus(request.getStatus());
        DeliveryPartner saved = driverRepository.save(driver);

        log.info("Driver status updated: id={} {} → {}", driverId, previous, request.getStatus());

        // When driver goes offline, remove them from the Redis geo set immediately
        if (request.getStatus() == DriverStatus.OFFLINE) {
            locationService.removeFromGeoSet(driverId, driver.getCityZone());
        }

        return toResponse(saved);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DeliveryPartner findDriver(String driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> new DriverNotFoundException("Driver not found: " + driverId));
    }

    private String generateDriverId() {
        int suffix = (int) (Math.random() * 9000) + 1000;
        String candidate = "DRV-" + suffix;
        // Retry on collision (extremely rare)
        while (driverRepository.existsById(candidate)) {
            suffix = (int) (Math.random() * 9000) + 1000;
            candidate = "DRV-" + suffix;
        }
        return candidate;
    }

    private DriverResponse toResponse(DeliveryPartner d) {
        return DriverResponse.builder()
                .id(d.getId())
                .fullName(d.getFullName())
                .phoneNumber(d.getPhoneNumber())
                .email(d.getEmail())
                .vehicleType(d.getVehicleType())
                .kycStatus(d.getKycStatus())
                .currentStatus(d.getCurrentStatus())
                .cityZone(d.getCityZone())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
