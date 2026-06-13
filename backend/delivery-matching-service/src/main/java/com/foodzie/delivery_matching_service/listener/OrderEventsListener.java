package com.foodzie.delivery_matching_service.listener;

import com.foodzie.delivery_matching_service.event.OrderStatusUpdatedEvent;
import com.foodzie.delivery_matching_service.service.DriverMatchingService;
import com.foodzie.delivery_matching_service.service.RestaurantLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes the "order-events" topic and triggers driver matching when
 * a restaurant accepts an order (newStatus = ACCEPTED).
 *
 * This is the only trigger for the matching algorithm — it reacts to the
 * world rather than waiting for a user request.
 *
 * Flow:
 *   1. Filter: only process events where newStatus = "ACCEPTED"
 *   2. Fetch restaurant lat/lon from restaurant-service (Eureka)
 *   3. Derive city zone from the restaurantId (simplified: use "default" zone
 *      in dev; in production, the restaurant document would carry a cityZone field)
 *   4. Delegate to DriverMatchingService for the full matching algorithm
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
    public void onOrderEvent(OrderStatusUpdatedEvent event) {
        if (event == null || event.getPayload() == null) {
            log.warn("Received null or malformed OrderStatusUpdatedEvent — skipping");
            return;
        }

        // Only act when the restaurant has accepted the order
        if (!"ACCEPTED".equals(event.getPayload().getNewStatus())) {
            return;
        }

        String orderId      = event.getPayload().getOrderId();
        String restaurantId = event.getPayload().getRestaurantId();

        log.info("Order ACCEPTED — starting driver matching: orderId={} restaurantId={}",
                orderId, restaurantId);

        // Fetch restaurant coordinates from restaurant-service
        double[] location;
        try {
            location = restaurantLocationService.getLocation(restaurantId);
        } catch (IllegalStateException e) {
            log.error("Cannot start matching for orderId={} — restaurant location unavailable: {}",
                    orderId, e.getMessage());
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

        matchingService.findAndDispatch(
                orderId, restaurantId, restaurantLat, restaurantLon, cityZone, itemCount);
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
