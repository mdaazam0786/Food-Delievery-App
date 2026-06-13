package com.foodzie.payment_service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    @Value("${app.kafka.topics.payment-events}")
    private String paymentEventsTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes a {@link PaymentCompletedEvent} to the payment-events topic.
     * The orderId is used as the Kafka message key so all events for the same
     * order land on the same partition (preserving processing order).
     */
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        String key = event.getPayload().getOrderId();
        kafkaTemplate.send(paymentEventsTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish PaymentCompletedEvent for orderId={}: {}",
                                key, ex.getMessage());
                    } else {
                        log.info("Published PaymentCompletedEvent orderId={} status={}",
                                key, event.getPayload().getStatus());
                    }
                });
    }
}
