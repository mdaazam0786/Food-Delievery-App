package com.foodzie.delivery_service.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "delivery_history")
@CompoundIndexes({
    @CompoundIndex(name = "driver_completed_idx", def = "{'driverId': 1, 'completedAt': -1}")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryHistory {

    @Id
    private String id;

    @Indexed
    private String driverId;

    @Indexed(unique = true)
    private String orderId;

    private String restaurantId;

    private String restaurantName;

    private String pickupAddress;

    private String deliveryAddress;

    private BigDecimal payoutAmount;

    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.COMPLETED;

    private Integer customerRating;

    private String customerFeedback;

    private LocalDateTime createdAt;

    @Indexed
    private LocalDateTime completedAt;

    private LocalDateTime updatedAt;
}
