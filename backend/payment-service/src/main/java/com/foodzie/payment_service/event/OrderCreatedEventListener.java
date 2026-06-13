package com.foodzie.payment_service.event;

import com.foodzie.payment_service.data.Payment;
import com.foodzie.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens to the "order-events" topic for ORDER_CREATED events.
 *
 * <p>Pre-creates a {@link Payment} record in MySQL as soon as an order is placed,
 * so the record is ready when the React Native app calls {@code POST /api/payments/initiate}.
 * This decouples order creation from payment initiation and avoids a synchronous
 * cross-service call at checkout time.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventListener {

    private final PaymentRepository paymentRepository;

    @KafkaListener(
            topics = "${app.kafka.topics.order-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "orderCreatedListenerContainerFactory"
    )
    public void onOrderCreated(OrderCreatedEvent event) {
        if (event == null || event.getPayload() == null) {
            log.warn("Received null or malformed OrderCreatedEvent — skipping");
            return;
        }
        if (!"ORDER_CREATED".equals(event.getEventType())) {
            // The order-events topic carries multiple event types; ignore the rest.
            return;
        }

        OrderCreatedEvent.Payload p = event.getPayload();
        log.info("OrderCreatedEvent received: orderId={} amount={} {}",
                p.getOrderId(), p.getFinancials().getTotalAmount(), p.getFinancials().getCurrency());

        // Idempotency guard — duplicate Kafka delivery should not create a second record.
        if (paymentRepository.findByOrderId(p.getOrderId()).isPresent()) {
            log.warn("Payment record already exists for orderId={} — skipping duplicate", p.getOrderId());
            return;
        }

        Payment payment = Payment.builder()
                .orderId(p.getOrderId())
                .userEmail(p.getUserEmail())
                .amount(p.getFinancials().getTotalAmount())
                .currency(p.getFinancials().getCurrency())
                .build();

        paymentRepository.save(payment);
        log.info("Payment record pre-created for orderId={} amount={} {}",
                p.getOrderId(), p.getFinancials().getTotalAmount(), p.getFinancials().getCurrency());
    }
}
