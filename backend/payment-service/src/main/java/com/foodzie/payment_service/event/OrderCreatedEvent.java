package com.foodzie.payment_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mirror of order-service's OrderCreatedEvent — consumed from the "order-events" Kafka topic.
 * Only the fields this service actually needs are mapped; unknown fields are ignored by Jackson.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

    private String eventId;
    private String eventType;   // "ORDER_CREATED"
    private String source;

    private Payload payload;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private String orderId;
        private String userEmail;
        private String restaurantId;
        private Long deliveryAddressId;
        private Financials financials;
        private List<OrderItem> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Financials {
        private BigDecimal totalAmount;
        private String currency;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private String menuItemId;
        private String itemName;
        private Integer quantity;
        private BigDecimal priceAtPurchase;
    }
}
