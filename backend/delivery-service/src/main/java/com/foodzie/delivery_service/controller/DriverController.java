package com.foodzie.delivery_service.controller;

import com.foodzie.common.dto.ApiResponse;
import com.foodzie.common.exceptions.*;
import com.foodzie.delivery_service.dto.*;
import com.foodzie.delivery_service.model.DriverLocation;
import com.foodzie.delivery_service.service.DriverEarningsService;
import com.foodzie.delivery_service.service.DriverLocationService;
import com.foodzie.delivery_service.service.DriverManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for the delivery-service.
 *
 * Management (admin/system):
 *   POST  /api/drivers                          — register a new driver
 *   GET   /api/drivers/{driverId}               — get driver profile
 *   PATCH /api/drivers/{driverId}/status        — update lifecycle status (clock in/out)
 *
 * Driver app (called by the driver's mobile app):
 *   POST  /api/drivers/location/ping            — GPS ping (Redis only, no MySQL)
 *
 * Internal / delivery-matching:
 *   GET   /api/drivers/nearby                   — find live drivers near a point
 */
@Slf4j
@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverManagementService managementService;
    private final DriverLocationService locationService;
    private final DriverEarningsService earningsService;

    @Value("${app.driver.default-search-radius-km}")
    private double defaultSearchRadiusKm;

    // ── Management ────────────────────────────────────────────────────────────

    /**
     * POST /api/drivers
     * Onboards a new delivery partner. KYC starts as PENDING.
     * Allows any authenticated user with ROLE_DRIVER to register.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_DRIVER')")
    public ResponseEntity<ApiResponse<DriverResponse>> registerDriver(
            @Valid @RequestBody RegisterDriverRequest request) {
        try {
            if (request == null) {
                throw new BadRequestException("Request body cannot be empty");
            }
            log.info("Registering new driver");
            DriverResponse response = managementService.registerDriver(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Driver registered", response));
        } catch (BadRequestException ex) {
            log.warn("Invalid driver registration request: {}", ex.getMessage());
            throw ex;
        } catch (ConflictException ex) {
            log.warn("Conflict during driver registration: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error registering driver: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * GET /api/drivers/{driverId}
     * Retrieve driver profile. Requires ROLE_DRIVER role.
     */
    @GetMapping("/{driverId}")
    @PreAuthorize("hasAnyRole('ROLE_DRIVER')")
    public ResponseEntity<ApiResponse<DriverResponse>> getDriver(
            @PathVariable String driverId) {
        try {
            if (driverId == null || driverId.isBlank()) {
                throw new BadRequestException("Driver ID cannot be empty");
            }
            log.debug("Fetching driver profile: {}", driverId);
            return ResponseEntity.ok(ApiResponse.ok("Driver profile",
                    managementService.getDriver(driverId)));
        } catch (BadRequestException ex) {
            log.warn("Invalid driver ID: {}", driverId);
            throw ex;
        } catch (Exception ex) {
            log.error("Error fetching driver {}: {}", driverId, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * GET /api/drivers/profile
     * Retrieve driver profile by email from X-User-Email header.
     * Used by frontend authentication to look up driver using JWT email.
     * Requires ROLE_DRIVER role.
     */
    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('ROLE_DRIVER')")
    public ResponseEntity<ApiResponse<DriverResponse>> getProfileByEmail(
            @RequestHeader(value = "X-User-Email", required = false) String email) {
        try {
            if (email == null || email.isBlank()) {
                throw new BadRequestException("X-User-Email header is required");
            }
            log.debug("Fetching driver profile by email");
            return ResponseEntity.ok(ApiResponse.ok("Driver profile",
                    managementService.getDriverByEmail(email)));
        } catch (BadRequestException ex) {
            log.warn("Invalid email header: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error fetching driver by email: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * PATCH /api/drivers/{driverId}/status
     * Major lifecycle transitions only: OFFLINE ↔ IDLE, IDLE → DELIVERING.
     * Going OFFLINE also removes the driver from the Redis geo set immediately.
     */
    @PatchMapping("/{driverId}/status")
    @PreAuthorize("hasAnyRole('ROLE_DRIVER')")
    public ResponseEntity<ApiResponse<DriverResponse>> updateStatus(
            @PathVariable String driverId,
            @Valid @RequestBody UpdateDriverStatusRequest request) {
        try {
            if (driverId == null || driverId.isBlank()) {
                throw new BadRequestException("Driver ID cannot be empty");
            }
            if (request == null) {
                throw new BadRequestException("Request body cannot be empty");
            }
            log.info("Updating driver status: {}", driverId);
            return ResponseEntity.ok(ApiResponse.ok("Status updated",
                    managementService.updateStatus(driverId, request)));
        } catch (BadRequestException ex) {
            log.warn("Invalid status update request: {}", ex.getMessage());
            throw ex;
        } catch (InvalidStateTransitionException ex) {
            log.warn("Invalid state transition for driver {}: {}", driverId, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error updating driver status {}: {}", driverId, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * GET /api/drivers/{driverId}/earnings
     * Get earnings summary for a driver (dashboard stats)
     * Shows: total earnings, total deliveries, active deliveries, today's earnings
     */
    @GetMapping("/{driverId}/earnings")
    @PreAuthorize("hasAnyRole('ROLE_DRIVER')")
    public ResponseEntity<ApiResponse<DriverEarningsResponse>> getEarningsSummary(
            @PathVariable String driverId) {
        try {
            if (driverId == null || driverId.isBlank()) {
                throw new BadRequestException("Driver ID cannot be empty");
            }
            log.debug("Fetching earnings summary for driver: {}", driverId);
            return ResponseEntity.ok(ApiResponse.ok("Earnings summary",
                    earningsService.getEarningsSummary(driverId)));
        } catch (BadRequestException ex) {
            log.warn("Invalid driver ID: {}", driverId);
            throw ex;
        } catch (Exception ex) {
            log.error("Error fetching earnings summary for driver {}: {}", driverId, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * GET /api/drivers/{driverId}/deliveries?page=0&size=10
     * Get paginated delivery history for a driver
     * Results sorted by completed_at DESC (most recent first)
     */
    @GetMapping("/{driverId}/deliveries")
    @PreAuthorize("hasAnyRole('ROLE_DRIVER')")
    public ResponseEntity<ApiResponse<PaginatedDeliveryResponse>> getDeliveryHistory(
            @PathVariable String driverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            if (driverId == null || driverId.isBlank()) {
                throw new BadRequestException("Driver ID cannot be empty");
            }
            if (page < 0) {
                throw new BadRequestException("Page number cannot be negative");
            }
            if (size < 1 || size > 100) {
                throw new BadRequestException("Page size must be between 1 and 100");
            }
            log.debug("Fetching delivery history for driver: {} page={} size={}", driverId, page, size);
            Pageable pageable = PageRequest.of(page, size);
            return ResponseEntity.ok(ApiResponse.ok("Delivery history",
                    earningsService.getDeliveryHistory(driverId, pageable)));
        } catch (BadRequestException ex) {
            log.warn("Invalid request for delivery history: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error fetching delivery history for driver {}: {}", driverId, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * GET /api/drivers/{driverId}/earnings-history?page=0&size=10
     * Get paginated earnings history for a driver
     * Same data as deliveries, but filtered/presented for earnings context
     */
    @GetMapping("/{driverId}/earnings-history")
    @PreAuthorize("hasAnyRole('ROLE_DRIVER')")
    public ResponseEntity<ApiResponse<PaginatedDeliveryResponse>> getEarningsHistory(
            @PathVariable String driverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            if (driverId == null || driverId.isBlank()) {
                throw new BadRequestException("Driver ID cannot be empty");
            }
            if (page < 0) {
                throw new BadRequestException("Page number cannot be negative");
            }
            if (size < 1 || size > 100) {
                throw new BadRequestException("Page size must be between 1 and 100");
            }
            log.debug("Fetching earnings history for driver: {} page={} size={}", driverId, page, size);
            Pageable pageable = PageRequest.of(page, size);
            return ResponseEntity.ok(ApiResponse.ok("Earnings history",
                    earningsService.getEarningsHistory(driverId, pageable)));
        } catch (BadRequestException ex) {
            log.warn("Invalid request for earnings history: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error fetching earnings history for driver {}: {}", driverId, ex.getMessage(), ex);
            throw ex;
        }
    }

    // ── Driver app ────────────────────────────────────────────────────────────

    /**
     * POST /api/drivers/location/ping
     * Called by the driver's app every ~5 seconds.
     * Executes GEOADD + SET heartbeat EX 15 in Redis.
     * Does NOT touch MySQL.
     */
    @PostMapping("/location/ping")
    @PreAuthorize("hasAnyRole('ROLE_DRIVER')")
    public ResponseEntity<ApiResponse<Void>> locationPing(
            @Valid @RequestBody LocationPingRequest request) {
        try {
            if (request == null) {
                throw new BadRequestException("Request body cannot be empty");
            }
            if (request.getDriverId() == null || request.getDriverId().isBlank()) {
                throw new BadRequestException("Driver ID cannot be empty");
            }
            log.debug("Updating location for driver: {}", request.getDriverId());
            locationService.updateLocation(
                    request.getDriverId(),
                    request.getLatitude(),
                    request.getLongitude(),
                    request.getCityZone()
            );
            return ResponseEntity.ok(ApiResponse.ok("Location updated", null));
        } catch (BadRequestException ex) {
            log.warn("Invalid location ping request: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error updating location: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    // ── Internal / delivery-matching ──────────────────────────────────────────

    /**
     * GET /api/drivers/nearby?lat=28.7041&lon=77.1025&cityZone=delhi&radius=5.0
     * Returns live (heartbeat-alive) drivers within the radius, sorted by distance.
     * Ghost drivers are evicted inline during this query.
     */
    @GetMapping("/nearby")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_DRIVER')")
    public ResponseEntity<ApiResponse<NearbyDriversResponse>> nearbyDrivers(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam String cityZone,
            @RequestParam(required = false) Double radius) {

        try {
            if (cityZone == null || cityZone.isBlank()) {
                throw new BadRequestException("City zone cannot be empty");
            }
            if (lat < -90 || lat > 90) {
                throw new BadRequestException("Latitude must be between -90 and 90");
            }
            if (lon < -180 || lon > 180) {
                throw new BadRequestException("Longitude must be between -180 and 180");
            }
            if (radius != null && radius <= 0) {
                throw new BadRequestException("Radius must be greater than 0");
            }

            log.debug("Searching for nearby drivers at lat={} lon={} cityZone={}", lat, lon, cityZone);
            double searchRadius = (radius != null && radius > 0) ? radius : defaultSearchRadiusKm;
            List<DriverLocation> drivers = locationService.findNearbyDrivers(
                    lat, lon, searchRadius, cityZone);

            NearbyDriversResponse response = NearbyDriversResponse.builder()
                    .drivers(drivers)
                    .totalActive(drivers.size())
                    .cityZone(cityZone)
                    .searchRadiusKm(searchRadius)
                    .build();

            return ResponseEntity.ok(ApiResponse.ok("Nearby drivers", response));
        } catch (BadRequestException ex) {
            log.warn("Invalid nearby drivers request: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error fetching nearby drivers: {}", ex.getMessage(), ex);
            throw ex;
        }
    }
}
