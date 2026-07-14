package com.foodzie.payment_service.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    private String id;

    @Indexed(unique = true)
    private String orderId;

    private String userEmail;

    private BigDecimal amount;

    @Builder.Default
    private String currency = "INR";

    @Indexed(unique = true)
    private String razorpayOrderId;

    private String razorpayPaymentId;

    private String razorpaySignature;

    @Builder.Default
    private PaymentStatus status = PaymentStatus.CREATED;

    private LocalDateTime createdAt;
}
