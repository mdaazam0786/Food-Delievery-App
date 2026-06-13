package com.foodzie.order_service.listener;

import com.foodzie.order_service.data.Order;
import com.foodzie.order_service.data.OrderStatus;
import com.foodzie.order_service.event.OrderEventPublisher;
import com.foodzie.order_service.event.OrderStatusUpdatedEvent;
import com.foodzie.order_service.event.PaymentCompletedEvent;
import com.foodzie.order_service.exception.OrderNotFoundException;
import com.foodzie.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens to the "payment-events" topic for {@link PaymentCompletedEvent} messages
 * published by payment-service after a successful Razorpay webhook.
 *
 * <p>On receipt:
 * <ol>
 *   <li>Loads the order from MongoDB.</li>
 *   <li>Transitions status: {@code PENDING} → {@code PREPARING}.</li>
 *   <li>Persists the updated order.</li>
 *   <li>Publishes an {@link OrderStatusUpdatedEvent} so downstream services react:
 *       <ul>
 *         <li>restaurant-acceptance-service pings the restaurant tablet.</li>
 *         <li>notification-service sends a push notification to the customer.</li>
 *       </ul>
 *   </li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    @KafkaListener(
            topics = "${app.kafka.topics.payment-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "paymentEventListenerContainerFactory"
    )
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        if (event == null || event.getPayload() == null) {
            log.warn("Received null or malformed PaymentCompletedEvent — skipping");
            return;
        }

        String orderId = event.getPayload().getOrderId();
        String transactionId = event.getPayload().getPaymentTransactionId();

        log.info("PaymentCompletedEvent received: orderId={} transactionId={}", orderId, transactionId);

        Order order;
        try {
            order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        } catch (OrderNotFoundException e) {
            // Log and discard — retrying won't help if the order genuinely doesn't exist.
            log.error("Cannot process PaymentCompletedEvent — {}", e.getMessage());
            return;
        }

        // Guard: only transition from PENDING. If the order is already past PENDING
        // (e.g. duplicate webhook delivery), skip silently.
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Order id={} is already in status={} — ignoring duplicate PaymentCompletedEvent",
                    orderId, order.getStatus());
            return;
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.PREPARING);
        Order saved = orderRepository.save(order);

        log.info("Order id={} transitioned {} → {} via payment transactionId={}",
                orderId, previousStatus, OrderStatus.PREPARING, transactionId);

        // Build and publish the enriched status-update event.
        OrderStatusUpdatedEvent statusEvent = OrderStatusUpdatedEvent.builder()
                .payload(OrderStatusUpdatedEvent.Payload.builder()
                        .orderId(saved.getId())
                        .userEmail(saved.getUserEmail())
                        .restaurantId(saved.getRestaurantId())
                        .previousStatus(previousStatus.name())
                        .newStatus(saved.getStatus().name())
                        .triggerReason("PAYMENT_SUCCESSFUL")
                        .metadata(OrderStatusUpdatedEvent.EventMetadata.builder()
                                .paymentTransactionId(transactionId)
                                .build())
                        .build())
                .build();

        eventPublisher.publishOrderStatusUpdated(statusEvent);
    }
}
