package com.foodzie.delivery_service.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "driver_earnings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverEarnings {

    @Id
    private String id;

    @Indexed(unique = true)
    private String driverId;

    @Builder.Default
    private BigDecimal totalEarnings = BigDecimal.ZERO;

    @Builder.Default
    private Long totalDeliveries = 0L;

    @Builder.Default
    private Long activeDeliveries = 0L;

    @Builder.Default
    private BigDecimal todaysEarnings = BigDecimal.ZERO;

    private LocalDate lastEarningsDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
