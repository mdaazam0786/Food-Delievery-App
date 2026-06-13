package com.foodzie.delivery_service.data;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tracks individual delivery records.
 * One row per completed delivery.
 *
 * Stored in MySQL (driverdb) — high volume table, indexed for pagination queries.
 */
@Entity
@Table(
    name = "delivery_history",
    indexes = {
        @Index(name = "idx_dh_driver_id", columnList = "driver_id"),
        @Index(name = "idx_dh_order_id", columnList = "order_id", unique = true),
        @Index(name = "idx_dh_completed_at", columnList = "completed_at"),
        @Index(name = "idx_dh_driver_completed", columnList = "driver_id,completed_at"),
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Driver who completed this delivery
     */
    @Column(name = "driver_id", nullable = false, length = 16)
    private String driverId;

    /**
     * Order ID this delivery belongs to
     */
    @Column(name = "order_id", nullable = false, unique = true, length = 32)
    private String orderId;

    /**
     * Restaurant ID for display
     */
    @Column(name = "restaurant_id", nullable = false, length = 32)
    private String restaurantId;

    /**
     * Restaurant name for display
     */
    @Column(name = "restaurant_name", nullable = false, length = 128)
    private String restaurantName;

    /**
     * Pickup address/location
     */
    @Column(name = "pickup_address", length = 256)
    private String pickupAddress;

    /**
     * Delivery address/location
     */
    @Column(name = "delivery_address", length = 256)
    private String deliveryAddress;

    /**
     * Estimated delivery payout
     */
    @Column(name = "payout_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal payoutAmount;

    /**
     * Delivery status at completion
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 16)
    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.COMPLETED;

    /**
     * Rating from customer (1-5 stars), optional
     */
    @Column(name = "customer_rating")
    private Integer customerRating;

    /**
     * Customer review/feedback, optional
     */
    @Column(name = "customer_feedback", length = 512)
    private String customerFeedback;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When the delivery was completed
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
