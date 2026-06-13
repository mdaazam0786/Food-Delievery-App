package com.foodzie.order_service.service.implementation;

import com.foodzie.order_service.data.Order;
import com.foodzie.order_service.data.OrderItem;
import com.foodzie.order_service.dto.*;
import com.foodzie.order_service.event.OrderCreatedEvent;
import com.foodzie.order_service.event.OrderEventPublisher;
import com.foodzie.order_service.event.OrderStatusUpdatedEvent;
import com.foodzie.order_service.repository.OrderRepository;
import com.foodzie.order_service.service.OrderService;
import com.foodzie.order_service.exception.OrderNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    // ── Pricing Constants ────────────────────────────────────────────────────

    /** Delivery rate per kilometer in INR. */
    private static final BigDecimal DELIVERY_RATE_PER_KM = new BigDecimal("10.00");

    /** GST percentage (18% of food subtotal). */
    private static final BigDecimal GST_PERCENTAGE = new BigDecimal("0.18");

    /** Rounding scale for monetary calculations. */
    private static final int CURRENCY_SCALE = 2;

    @Override
    public OrderResponse createOrder(String userEmail, CreateOrderRequest request) {
        // Calculate food subtotal
        BigDecimal foodSubtotal = request.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate distance using Haversine formula
        double distance = calculateDistance(
                request.getCustomerLatitude(),
                request.getCustomerLongitude(),
                request.getRestaurantLatitude(),
                request.getRestaurantLongitude()
        );

        // Calculate delivery fee
        BigDecimal deliveryFee = BigDecimal.valueOf(distance)
                .multiply(DELIVERY_RATE_PER_KM)
                .setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);

        // Calculate GST
        BigDecimal gstAmount = foodSubtotal
                .multiply(GST_PERCENTAGE)
                .setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);

        // Calculate total
        BigDecimal totalAmount = foodSubtotal
                .add(deliveryFee)
                .add(gstAmount)
                .setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);

        List<OrderItem> items = request.getItems().stream()
                .map(itemReq -> OrderItem.builder()
                        .menuItemId(itemReq.getMenuItemId())
                        .itemName(itemReq.getItemName())
                        .quantity(itemReq.getQuantity())
                        .priceAtPurchase(itemReq.getPrice())
                        .build())
                .toList();

        Order order = Order.builder()
                .userEmail(userEmail)
                .restaurantId(request.getRestaurantId())
                .deliveryAddressId(request.getDeliveryAddressId())
                .foodSubtotal(foodSubtotal)
                .deliveryFee(deliveryFee)
                .gstAmount(gstAmount)
                .totalAmount(totalAmount)
                .items(items)
                .build();

        Order saved = orderRepository.save(order);
        log.info("Order created id={} for user={} distance={:.2f}km deliveryFee={} gst={} total={}",
                saved.getId(), userEmail, distance, deliveryFee, gstAmount, totalAmount);

        // Build the full event payload so downstream services don't need to
        // query the order-service database.
        List<OrderCreatedEvent.OrderItem> eventItems = saved.getItems().stream()
                .map(i -> OrderCreatedEvent.OrderItem.builder()
                        .menuItemId(i.getMenuItemId())
                        .itemName(i.getItemName())
                        .quantity(i.getQuantity())
                        .priceAtPurchase(i.getPriceAtPurchase())
                        .build())
                .toList();

        OrderCreatedEvent.Payload payload = OrderCreatedEvent.Payload.builder()
                .orderId(saved.getId())
                .userEmail(saved.getUserEmail())
                .restaurantId(saved.getRestaurantId())
                .deliveryAddressId(saved.getDeliveryAddressId())
                .financials(OrderCreatedEvent.Financials.builder()
                        .foodSubtotal(saved.getFoodSubtotal())
                        .deliveryFee(saved.getDeliveryFee())
                        .gstAmount(saved.getGstAmount())
                        .totalAmount(saved.getTotalAmount())
                        .currency(saved.getCurrency())
                        .build())
                .items(eventItems)
                .build();

        // Fire Kafka event → payment-service reads financials to create a Razorpay
        // Payment Intent; notification-service may use it to send a confirmation email.
        eventPublisher.publishOrderCreated(OrderCreatedEvent.builder()
                .payload(payload)
                .build());

        return toResponse(saved);
    }

    @Override
    public List<OrderResponse> getMyOrders(String userEmail) {
        return orderRepository.findAllByUserEmailOrderByCreatedAtDesc(userEmail)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public Page<OrderResponse> getMyOrders(String userEmail, Pageable pageable) {
        log.info("Fetching paginated orders for userEmail={} page={} size={}",
                userEmail, pageable.getPageNumber(), pageable.getPageSize());
        return orderRepository
                .findAllByUserEmailOrderByCreatedAtDesc(userEmail, pageable)
                .map(this::toResponse);
    }

    @Override
    public OrderResponse getOrderById(String orderId, String userEmail) {
        Order order = findOrder(orderId);
        if (!order.getUserEmail().equals(userEmail)) {
            throw new AccessDeniedException("Order does not belong to this user");
        }
        return toResponse(order);
    }

    @Override
    public OrderResponse updateOrderStatus(String orderId, UpdateOrderStatusRequest request) {
        Order order = findOrder(orderId);
        String previousStatus = order.getStatus().name();

        order.setStatus(request.getStatus());
        Order saved = orderRepository.save(order);

        log.info("Order id={} status updated {} → {}", orderId, previousStatus, request.getStatus());

        // Fire enriched Kafka event → restaurant-acceptance-service + notification-service
        eventPublisher.publishOrderStatusUpdated(OrderStatusUpdatedEvent.builder()
                .payload(OrderStatusUpdatedEvent.Payload.builder()
                        .orderId(saved.getId())
                        .userEmail(saved.getUserEmail())
                        .restaurantId(saved.getRestaurantId())
                        .previousStatus(previousStatus)
                        .newStatus(saved.getStatus().name())
                        .triggerReason(request.getStatus().toString())
                        .build())
                .build());

        return toResponse(saved);
    }

    @Override
    public Page<OrderResponse> getOrdersByRestaurant(String restaurantId, Pageable pageable) {
        log.info("Fetching orders for restaurantId={} page={} size={}",
                restaurantId, pageable.getPageNumber(), pageable.getPageSize());
        return orderRepository
                .findAllByRestaurantIdOrderByCreatedAtDesc(restaurantId, pageable)
                .map(this::toResponse);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Calculates the great-circle distance between two points on Earth
     * using the Haversine formula.
     *
     * @param lat1 Latitude of first point (degrees)
     * @param lon1 Longitude of first point (degrees)
     * @param lat2 Latitude of second point (degrees)
     * @param lon2 Longitude of second point (degrees)
     * @return Distance in kilometers
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;

        // Convert degrees to radians
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        // Haversine formula
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    private Order findOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .menuItemId(item.getMenuItemId())
                        .itemName(item.getItemName())
                        .quantity(item.getQuantity())
                        .priceAtPurchase(item.getPriceAtPurchase())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .userEmail(order.getUserEmail())
                .restaurantId(order.getRestaurantId())
                .deliveryAddressId(order.getDeliveryAddressId())
                .driverId(order.getDriverId())
                .status(order.getStatus())
                .foodSubtotal(order.getFoodSubtotal())
                .deliveryFee(order.getDeliveryFee())
                .gstAmount(order.getGstAmount())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
