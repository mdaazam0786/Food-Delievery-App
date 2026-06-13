package com.foodzie.delivery_acceptance_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to "delivery-events" when a driver successfully claims the
 * distributed lock and is officially assigned to an order.
 *
 * Consumers:
 *   order-service        — updates master order record with driverId
 *   notification-service — alerts the customer: "Your delivery partner has been assigned!"
 *
 * Payload matches the spec:
 *   { "orderId": 104592, "driverId": "DRV-8832", "status": "CONFIRMED" }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPartnerAssignedEvent {

    @Builder.Default
    private String eventId = "evt_" + UUID.randomUUID();

    @Builder.Default
    private String eventType = "DELIVERY_PARTNER_ASSIGNED";

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private String source = "delivery-acceptance-service";

    private Payload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private String orderId;
        private String driverId;
        private String restaurantId;
        private String cityZone;

        /** Always "CONFIRMED" on a successful assignment. */
        @Builder.Default
        private String status = "CONFIRMED";
    }
}
