package com.foodzie.order_service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    @Value("${app.kafka.topics.order-events}")
    private String orderEventsTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes an {@link OrderCreatedEvent} to the order-events topic.
     * The orderId is used as the Kafka message key to guarantee that all events
     * for the same order land on the same partition (preserving order).
     */
    public void publishOrderCreated(OrderCreatedEvent event) {
        String key = event.getPayload().getOrderId();
        kafkaTemplate.send(orderEventsTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OrderCreatedEvent for orderId={}: {}",
                                key, ex.getMessage());
                    } else {
                        log.info("Published OrderCreatedEvent for orderId={}", key);
                    }
                });
    }

    /**
     * Publishes an {@link OrderStatusUpdatedEvent} to the order-events topic.
     * The orderId is used as the Kafka message key for partition ordering.
     */
    public void publishOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        String key = event.getPayload().getOrderId();
        kafkaTemplate.send(orderEventsTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OrderStatusUpdatedEvent for orderId={}: {}",
                                key, ex.getMessage());
                    } else {
                        log.info("Published OrderStatusUpdatedEvent for orderId={} newStatus={}",
                                key, event.getPayload().getNewStatus());
                    }
                });
    }
}
