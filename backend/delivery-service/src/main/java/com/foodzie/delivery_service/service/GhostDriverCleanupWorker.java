package com.foodzie.delivery_service.service;

import com.foodzie.delivery_service.data.DeliveryPartner;
import com.foodzie.delivery_service.data.DriverStatus;
import com.foodzie.delivery_service.data.KycStatus;
import com.foodzie.delivery_service.repository.DeliveryPartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Background worker that proactively evicts ghost drivers from Redis geo sets.
 *
 * The Problem:
 *   Redis GEO sets don't support per-member TTLs. A driver whose phone dies or
 *   who deletes the app without clocking out stays in the geo set indefinitely.
 *   If the delivery-matching service queries for nearby drivers, it might assign
 *   an order to a ghost — a driver who will never pick it up.
 *
 * The Solution (Heartbeat Pattern):
 *   Every GPS ping refreshes a separate key: driver:heartbeat:{id} EX 15
 *   This worker runs every 10 seconds and scans all IDLE/DELIVERING drivers.
 *   For each driver in the geo set, it checks if their heartbeat key exists.
 *   If missing → ghost → ZREM from geo set.
 *
 * This runs on a fixed delay (not a fixed rate) so if a scan takes longer than
 * the interval, the next scan waits rather than overlapping.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GhostDriverCleanupWorker {

    private final DriverLocationService locationService;
    private final DeliveryPartnerRepository driverRepository;

    /**
     * Runs every 10 seconds (configurable via app.driver.cleanup-interval-ms).
     * Scans all active drivers across all city zones and evicts ghosts.
     */
    @Scheduled(fixedDelayString = "${app.driver.cleanup-interval-ms}")
    public void evictGhostDrivers() {
        // Fetch all drivers who should be active (IDLE or DELIVERING, KYC approved)
        List<DeliveryPartner> idleDrivers = driverRepository
                .findAllByCurrentStatusAndKycStatus(DriverStatus.IDLE, KycStatus.APPROVED);
        List<DeliveryPartner> deliveringDrivers = driverRepository
                .findAllByCurrentStatusAndKycStatus(DriverStatus.DELIVERING, KycStatus.APPROVED);

        int evicted = 0;

        for (DeliveryPartner driver : idleDrivers) {
            evicted += checkAndEvict(driver);
        }
        for (DeliveryPartner driver : deliveringDrivers) {
            evicted += checkAndEvict(driver);
        }

        if (evicted > 0) {
            log.info("Ghost driver cleanup: evicted {} drivers", evicted);
        } else {
            log.debug("Ghost driver cleanup: no ghosts found");
        }
    }

    private int checkAndEvict(DeliveryPartner driver) {
        if (!locationService.isAlive(driver.getId())) {
            locationService.evictGhostDriver(driver.getId(), driver.getCityZone());
            log.warn("Ghost evicted by cleanup worker: driverId={} zone={}",
                    driver.getId(), driver.getCityZone());
            return 1;
        }
        return 0;
    }
}
