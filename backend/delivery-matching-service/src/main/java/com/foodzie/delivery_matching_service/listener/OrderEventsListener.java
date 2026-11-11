package com.foodzie.delivery_matching_service.listener;

import com.foodzie.delivery_matching_service.event.OrderStatusUpdatedEvent;
import com.foodzie.delivery_matching_service.service.DriverMatchingService;
import com.foodzie.delivery_matching_service.service.RestaurantLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumes the "order-events" topic and triggers driver matching when
 * a restaurant accepts an order (newStatus = ACCEPTED).
 *
 * This is the only trigger for the matching algorithm — it reacts to the
 * world rather than waiting for a user request.
 *
 * Acknowledgment strategy:
 * - Manual acknowledgment mode: explicitly acknowledge only successful processing
 * - On error: negative acknowledge (nack) with delay for retry
 * - Prevents message loss and duplicate processing on failures
 *
 * Flow:
 *   1. Filter: only process events where newStatus = "ACCEPTED"
 *   2. Fetch restaurant lat/lon from restaurant-service (Eureka)
 *   3. Derive city zone from the restaurantId (simplified: use "default" zone
 *      in dev; in production, the restaurant document would carry a cityZone field)
 *   4. Delegate to DriverMatchingService for the full matching algorithm
 *   5. Acknowledge message on success or nack on failure
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventsListener {

    private final DriverMatchingService matchingService;
    private final RestaurantLocationService restaurantLocationService;

    @KafkaListener(
            topics = "${app.kafka.topics.order-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "orderStatusListenerContainerFactory"
    )
    public void onOrderEvent(
            @Payload OrderStatusUpdatedEvent event,
            @Header(name = KafkaHeaders.RECEIVED_PARTITION_ID, required = false) Integer partition,
            @Header(name = KafkaHeaders.OFFSET, required = false) Long offset,
            Acknowledgment ack) {
        
        try {
            if (event == null || event.getPayload() == null) {
                log.warn("Received null or malformed OrderStatusUpdatedEvent at partition={} offset={} — skipping",
                        partition != null ? partition : "unknown", 
                        offset != null ? offset : "unknown");
                if (ack != null) ack.acknowledge();  // Acknowledge to avoid reprocessing malformed messages
                return;
            }

            // Only act when the restaurant has accepted the order
            if (!"ACCEPTED".equals(event.getPayload().getNewStatus())) {
                log.debug("Order status not ACCEPTED, skipping: status={}", 
                        event.getPayload().getNewStatus());
                if (ack != null) ack.acknowledge();  // Acknowledge non-target events
                return;
            }

            String orderId      = event.getPayload().getOrderId();
            String restaurantId = event.getPayload().getRestaurantId();

            log.info("Order ACCEPTED — starting driver matching: orderId={} restaurantId={} partition={} offset={}",
                    orderId, restaurantId, 
                    partition != null ? partition : "unknown", 
                    offset != null ? offset : "unknown");

            // Fetch restaurant coordinates from restaurant-service (with retry)
            double[] location;
            try {
                location = restaurantLocationService.getLocation(restaurantId);
            } catch (IllegalStateException e) {
                log.error("Cannot start matching for orderId={} — restaurant location unavailable after retries: {}",
                        orderId, e.getMessage());
                // Negative acknowledge with 5s delay for retry
                if (ack != null) ack.nack(5000);
                return;
            }

            double restaurantLat = location[0];
            double restaurantLon = location[1];

            // In production, the restaurant document would carry a cityZone field.
            // For now we derive it from the restaurantId prefix or default to "default".
            String cityZone = deriveCityZone(restaurantId);

            // itemCount = 1 as default (we don't have order items in this event).
            // In production, the OrderStatusUpdatedEvent.Payload would carry itemCount,
            // or this service would call order-service to fetch it.
            int itemCount = 1;

            // Execute matching algorithm
            matchingService.findAndDispatch(
                    orderId, restaurantId, restaurantLat, restaurantLon, cityZone, itemCount);
            
            // Explicit acknowledgment on successful processing
            if (ack != null) ack.acknowledge();
            log.debug("Order event processed and acknowledged: orderId={}", orderId);

        } catch (Exception e) {
            log.error("Unexpected error processing order event at partition={} offset={}: {}",
                    partition != null ? partition : "unknown", 
                    offset != null ? offset : "unknown", 
                    e.getMessage(), e);
            // Negative acknowledge with 5s delay for retry
            try {
                if (ack != null) ack.nack(5000);
            } catch (Exception nackException) {
                log.error("Failed to nack message: {}", nackException.getMessage());
            }
        }
    }

    /**
     * Derives the city zone key from the restaurant ID.
     * In production, the restaurant document carries an explicit cityZone field
     * that would be included in the event payload.
     */
    private String deriveCityZone(String restaurantId) {
        // Placeholder — replace with actual zone lookup in production
        return "default";
    }
}
