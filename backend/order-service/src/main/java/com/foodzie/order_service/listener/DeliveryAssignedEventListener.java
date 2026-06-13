package com.foodzie.order_service.listener;

import com.foodzie.order_service.data.Order;
import com.foodzie.order_service.event.DeliveryPartnerAssignedEvent;
import com.foodzie.order_service.exception.OrderNotFoundException;
import com.foodzie.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens to the "delivery-events" topic for DeliveryPartnerAssignedEvent messages
 * published by delivery-acceptance-service after a driver wins the distributed lock.
 *
 * On receipt, writes the driverId onto the Order document so the
 * order-fulfillment-service can look it up without a cross-service call.
 *
 * No status transition happens here — the fulfillment-service owns status changes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryAssignedEventListener {

    private final OrderRepository orderRepository;

    @KafkaListener(
            topics = "${app.kafka.topics.delivery-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "deliveryAssignedListenerContainerFactory"
    )
    public void handleDeliveryPartnerAssigned(DeliveryPartnerAssignedEvent event) {
        if (event == null || event.getPayload() == null) {
            log.warn("Received null or malformed DeliveryPartnerAssignedEvent — skipping");
            return;
        }

        if (!"DELIVERY_PARTNER_ASSIGNED".equals(event.getEventType())) {
            // This consumer also receives OfferExpiredEvent on the same topic — ignore those
            return;
        }

        String orderId  = event.getPayload().getOrderId();
        String driverId = event.getPayload().getDriverId();

        log.info("DeliveryPartnerAssignedEvent received: orderId={} driverId={}", orderId, driverId);

        Order order;
        try {
            order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        } catch (OrderNotFoundException e) {
            log.error("Cannot process DeliveryPartnerAssignedEvent — {}", e.getMessage());
            return;
        }

        // Idempotency guard: skip if driverId is already set (duplicate event delivery)
        if (driverId.equals(order.getDriverId())) {
            log.warn("Order id={} already has driverId={} — ignoring duplicate event", orderId, driverId);
            return;
        }

        order.setDriverId(driverId);
        orderRepository.save(order);

        log.info("Order id={} updated with driverId={}", orderId, driverId);
    }
}
