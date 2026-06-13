package com.foodzie.payment_service.repository;

import com.foodzie.payment_service.data.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Lookup by internal order-service ID — used by the Kafka listener and /initiate. */
    Optional<Payment> findByOrderId(String orderId);

    /** Lookup by Razorpay order ID — used by the webhook handler. */
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
}
