package com.foodzie.payment_service.repository;

import com.foodzie.payment_service.data.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    /** Lookup by internal order-service ID — used by the Kafka listener and /initiate. */
    Optional<Payment> findByOrderId(String orderId);

    /** Lookup by Razorpay order ID — used by the webhook handler. */
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
}
