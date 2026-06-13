package com.foodzie.order_service.listener;

import com.foodzie.order_service.data.Order;
import com.foodzie.order_service.data.OrderStatus;
import com.foodzie.order_service.event.OrderEventPublisher;
import com.foodzie.order_service.event.OrderStatusUpdatedEvent;
import com.foodzie.order_service.event.RestaurantDecisionEvent;
import com.foodzie.order_service.exception.OrderNotFoundException;
import com.foodzie.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens to the "order-events" topic for {@link RestaurantDecisionEvent} messages
 * published by restaurant-acceptance-service when a chef accepts or declines an order,
 * or when the 5-minute TTL expires with no action.
 *
 * <p>On receipt:
 * <ol>
 *   <li>Loads the order from MongoDB.</li>
 *   <li>Transitions status: {@code PREPARING} → {@code ACCEPTED} or {@code DECLINED}.</li>
 *   <li>Persists the updated order.</li>
 *   <li>Publishes an {@link OrderStatusUpdatedEvent} so downstream services react:
 *       <ul>
 *         <li>notification-service sends a push notification to the customer.</li>
 *         <li>delivery-matching-service wakes up to find a driver (when accepted).</li>
 *       </ul>
 *   </li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestaurantDecisionEventListener {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    @KafkaListener(
            topics = "${app.kafka.topics.order-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "restaurantDecisionEventListenerContainerFactory"
    )
    public void handleRestaurantDecision(RestaurantDecisionEvent event) {
        if (event == null || event.getPayload() == null) {
            log.warn("🚫 Received null or malformed RestaurantDecisionEvent — skipping");
            return;
        }

        String orderId = event.getPayload().getOrderId();
        String newStatusStr = event.getPayload().getNewStatus();
        String triggerReason = event.getPayload().getTriggerReason();

        log.info("🔔 RestaurantDecisionEvent received: orderId={} newStatus={} reason={}",
                orderId, newStatusStr, triggerReason);

        Order order;
        try {
            order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        } catch (OrderNotFoundException e) {
            log.error("❌ Cannot process RestaurantDecisionEvent — {}", e.getMessage());
            return;
        }

        // Guard: only transition from PREPARING. If the order is already past PREPARING
        // (e.g. duplicate event), skip silently.
        if (order.getStatus() != OrderStatus.PREPARING) {
            log.warn("⚠️  Order id={} is already in status={} — ignoring duplicate RestaurantDecisionEvent with reason={}",
                    orderId, order.getStatus(), triggerReason);
            return;
        }

        // Map string status to OrderStatus enum
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(newStatusStr);
        } catch (IllegalArgumentException e) {
            log.error("❌ Unknown status '{}' in RestaurantDecisionEvent for orderId={}", newStatusStr, orderId);
            return;
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);

        log.info("✅ Order id={} transitioned {} → {} via {}",
                orderId, previousStatus, newStatus, triggerReason);

        // Build and publish the enriched status-update event.
        OrderStatusUpdatedEvent statusEvent = OrderStatusUpdatedEvent.builder()
                .payload(OrderStatusUpdatedEvent.Payload.builder()
                        .orderId(saved.getId())
                        .userEmail(saved.getUserEmail())
                        .restaurantId(saved.getRestaurantId())
                        .previousStatus(previousStatus.name())
                        .newStatus(saved.getStatus().name())
                        .triggerReason(triggerReason)
                        .metadata(OrderStatusUpdatedEvent.EventMetadata.builder()
                                .estimatedPrepTimeMinutes(
                                        event.getPayload().getMetadata() != null
                                                ? event.getPayload().getMetadata().getEstimatedPrepTimeMinutes()
                                                : null)
                                .declineReason(
                                        event.getPayload().getMetadata() != null
                                                ? event.getPayload().getMetadata().getDeclineReason()
                                                : null)
                                .build())
                        .build())
                .build();

        eventPublisher.publishOrderStatusUpdated(statusEvent);
    }
}
