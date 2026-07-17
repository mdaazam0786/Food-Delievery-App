package com.foodzie.delivery_service.service;

import com.foodzie.delivery_service.data.DeliveryHistory;
import com.foodzie.delivery_service.data.DriverEarnings;
import com.foodzie.delivery_service.dto.DeliveryHistoryResponse;
import com.foodzie.delivery_service.dto.DriverEarningsResponse;
import com.foodzie.delivery_service.dto.PaginatedDeliveryResponse;
import com.foodzie.delivery_service.exception.DriverNotFoundException;
import com.foodzie.delivery_service.repository.DeliveryHistoryRepository;
import com.foodzie.delivery_service.repository.DriverEarningsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Handles driver earnings and delivery history queries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverEarningsService {

    private final DriverEarningsRepository earningsRepository;
    private final DeliveryHistoryRepository deliveryHistoryRepository;

    /**
     * Get earnings summary for a driver (for dashboard)
     */
    public DriverEarningsResponse getEarningsSummary(String driverId) {
        DriverEarnings earnings = earningsRepository.findByDriverId(driverId)
                .orElseThrow(() -> new DriverNotFoundException("Earnings not found for driver: " + driverId));

        Long totalDeliveries = earnings.getTotalDeliveries() != null ? earnings.getTotalDeliveries() : 0L;
        Long activeDeliveries = earnings.getActiveDeliveries() != null ? earnings.getActiveDeliveries() : 0L;

        return DriverEarningsResponse.builder()
                .driverId(earnings.getDriverId())
                .totalEarnings(earnings.getTotalEarnings())
                .totalDeliveries(totalDeliveries)
                .activeDeliveries(activeDeliveries)
                .todaysEarnings(earnings.getTodaysEarnings())
                .build();
    }

    /**
     * Get paginated delivery history for a driver
     */
    public PaginatedDeliveryResponse getDeliveryHistory(String driverId, Pageable pageable) {
        Page<DeliveryHistory> page = deliveryHistoryRepository
                .findByDriverIdOrderByCompletedAtDesc(driverId, pageable);

        return PaginatedDeliveryResponse.builder()
                .content(page.map(this::toDeliveryResponse).getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }

    /**
     * Get paginated earnings history for a driver
     * (Same as delivery history but with earnings focus)
     */
    public PaginatedDeliveryResponse getEarningsHistory(String driverId, Pageable pageable) {
        return getDeliveryHistory(driverId, pageable);
    }

    /**
     * Create or update earnings record when driver registers
     */
    @Transactional
    public void initializeEarningsForDriver(String driverId) {
        // Only create if doesn't exist
        if (earningsRepository.findByDriverId(driverId).isEmpty()) {
            DriverEarnings earnings = DriverEarnings.builder()
                    .driverId(driverId)
                    .totalEarnings(java.math.BigDecimal.ZERO)
                    .totalDeliveries(0L)
                    .activeDeliveries(0L)
                    .todaysEarnings(java.math.BigDecimal.ZERO)
                    .lastEarningsDate(LocalDate.now())
                    .build();

            earningsRepository.save(earnings);
            log.info("Earnings record initialized for driver: {}", driverId);
        }
    }

    // ── Helper methods ─────────────────────────────────────────────────────────

    private DeliveryHistoryResponse toDeliveryResponse(DeliveryHistory history) {
        return DeliveryHistoryResponse.builder()
                .id(history.getId())
                .orderId(history.getOrderId())
                .restaurantName(history.getRestaurantName())
                .pickupAddress(history.getPickupAddress())
                .deliveryAddress(history.getDeliveryAddress())
                .payoutAmount(history.getPayoutAmount())
                .deliveryStatus(history.getDeliveryStatus())
                .customerRating(history.getCustomerRating())
                .customerFeedback(history.getCustomerFeedback())
                .completedAt(history.getCompletedAt())
                .build();
    }
}
