package com.foodzie.order_fulfillment_service.service;

import com.foodzie.order_fulfillment_service.data.Order;
import com.foodzie.order_fulfillment_service.data.OrderStatus;
import com.foodzie.order_fulfillment_service.dto.FulfillmentResponse;
import com.foodzie.order_fulfillment_service.event.OrderStatusUpdatedEvent;
import com.foodzie.order_fulfillment_service.exception.InvalidStateTransitionException;
import com.foodzie.order_fulfillment_service.exception.OrderNotFoundException;
import com.foodzie.order_fulfillment_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

/**
 * State machine for the physical delivery lifecycle.
 *
 * Valid transitions enforced here:
 *
 *   ACCEPTED  ──► READY_FOR_PICKUP   (chef marks food ready)
 *   READY_FOR_PICKUP ──► OUT_FOR_DELIVERY  (driver picks up)
 *   OUT_FOR_DELIVERY ──► DELIVERED         (driver confirms delivery)
 *
 * Each transition:
 *   1. Validates the current status is the expected predecessor.
 *   2. Updates the Order document in MongoDB.
 *   3. Publishes an OrderStatusUpdatedEvent to Kafka with a specific triggerReason.
 *
 * Downstream consumers of the Kafka event:
 *   notification-service — sends push notifications to the relevant actor
 *   payment-service      — on DELIVERED, settles funds with the restaurant
 *   websocket-manager    — on DELIVERED, closes the live tracking connection
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FulfillmentService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.order-events}")
    private String orderEventsTopic;

    // ── Transition 1: ACCEPTED → READY_FOR_PICKUP ─────────────────────────────

    /**
     * Called by the restaurant chef when the food is ready for pickup.
     * Actor: ROLE_RESTAURANT
     *
     * Precondition: order.status == ACCEPTED
     * Postcondition: order.status == READY_FOR_PICKUP
     *
     * Downstream: notification-service alerts the assigned driver:
     *   "The food is ready! Head inside the restaurant."
     */
    public FulfillmentResponse markReady(String orderId) {
        Order order = loadOrder(orderId);

        validateTransition(order, Set.of(OrderStatus.ACCEPTED),
                OrderStatus.READY_FOR_PICKUP,
                "Order must be in ACCEPTED state before marking as ready. Current: " + order.getStatus());

        return transition(order, OrderStatus.READY_FOR_PICKUP, "CHEF_MARKED_READY");
    }

    // ── Transition 2: READY_FOR_PICKUP → OUT_FOR_DELIVERY ────────────────────

    /**
     * Called by the driver when they physically pick up the order from the restaurant.
     * Actor: ROLE_DRIVER
     *
     * Precondition: order.status == READY_FOR_PICKUP
     * Postcondition: order.status == OUT_FOR_DELIVERY
     *
     * Downstream: notification-service alerts the customer:
     *   "Your delivery partner has picked up your order and is on the way!"
     */
    public FulfillmentResponse markPickedUp(String orderId) {
        Order order = loadOrder(orderId);

        validateTransition(order, Set.of(OrderStatus.READY_FOR_PICKUP),
                OrderStatus.OUT_FOR_DELIVERY,
                "Order must be in READY_FOR_PICKUP state before marking as picked up. Current: " + order.getStatus());

        return transition(order, OrderStatus.OUT_FOR_DELIVERY, "DRIVER_PICKED_UP");
    }

    // ── Transition 3: OUT_FOR_DELIVERY → DELIVERED ────────────────────────────

    /**
     * Called by the driver when they hand the order to the customer.
     * Actor: ROLE_DRIVER
     *
     * Precondition: order.status == OUT_FOR_DELIVERY
     * Postcondition: order.status == DELIVERED
     *
     * Downstream:
     *   payment-service      — settles funds with the restaurant (uses orderTotal in metadata)
     *   notification-service — confirms delivery to the customer
     *   websocket-manager    — closes the live tracking connection
     */
    public FulfillmentResponse markDelivered(String orderId) {
        Order order = loadOrder(orderId);

        validateTransition(order, Set.of(OrderStatus.OUT_FOR_DELIVERY),
                OrderStatus.DELIVERED,
                "Order must be OUT_FOR_DELIVERY before marking as delivered. Current: " + order.getStatus());

        return transition(order, OrderStatus.DELIVERED, "DRIVER_CONFIRMED_DELIVERY");
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private Order loadOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }

    private void validateTransition(Order order, Set<OrderStatus> allowedPredecessors,
                                    OrderStatus target, String errorMessage) {
        if (!allowedPredecessors.contains(order.getStatus())) {
            log.warn("Invalid state transition attempt: orderId={} currentStatus={} targetStatus={}",
                    order.getId(), order.getStatus(), target);
            throw new InvalidStateTransitionException(errorMessage);
        }
    }

    private FulfillmentResponse transition(Order order, OrderStatus newStatus, String triggerReason) {
        String orderId        = order.getId();
        String previousStatus = order.getStatus().name();

        order.setStatus(newStatus);
        orderRepository.save(order);

        log.info("Order fulfillment transition: orderId={} {} → {} reason={}",
                orderId, previousStatus, newStatus, triggerReason);

        // Build the Kafka event — include orderTotal in metadata only on DELIVERED
        // so payment-service can settle without a DB lookup.
        OrderStatusUpdatedEvent.EventMetadata metadata = null;
        if (newStatus == OrderStatus.DELIVERED) {
            metadata = OrderStatusUpdatedEvent.EventMetadata.builder()
                    .orderTotal(order.getTotalAmount())
                    .currency(order.getCurrency())
                    .build();
        }

        OrderStatusUpdatedEvent event = OrderStatusUpdatedEvent.builder()
                .payload(OrderStatusUpdatedEvent.Payload.builder()
                        .orderId(orderId)
                        .userEmail(order.getUserEmail())
                        .restaurantId(order.getRestaurantId())
                        .driverId(order.getDriverId())
                        .previousStatus(previousStatus)
                        .newStatus(newStatus.name())
                        .triggerReason(triggerReason)
                        .metadata(metadata)
                        .build())
                .build();

        kafkaTemplate.send(orderEventsTopic, orderId, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OrderStatusUpdatedEvent: orderId={} newStatus={} error={}",
                                orderId, newStatus, ex.getMessage());
                    } else {
                        log.info("OrderStatusUpdatedEvent published: orderId={} newStatus={} reason={}",
                                orderId, newStatus, triggerReason);
                    }
                });

        return FulfillmentResponse.builder()
                .orderId(orderId)
                .previousStatus(previousStatus)
                .newStatus(newStatus.name())
                .driverId(order.getDriverId())
                .transitionedAt(Instant.now())
                .build();
    }
}
