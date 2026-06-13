package com.foodzie.order_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Inbound Kafka event consumed from the "delivery-events" topic.
 * Published by delivery-acceptance-service when a driver wins the distributed lock.
 *
 * The order-service listens for this to write driverId onto the Order document
 * so the fulfillment-service can look it up without a cross-service call.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPartnerAssignedEvent {

    private String eventId;
    private String eventType;   // "DELIVERY_PARTNER_ASSIGNED"
    private Instant timestamp;
    private String source;      // "delivery-acceptance-service"
    private Payload payload;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private String orderId;
        private String driverId;
        private String restaurantId;
        private String cityZone;
        private String status;  // "CONFIRMED"
    }
}
