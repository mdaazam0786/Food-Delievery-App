package com.foodzie.order_service.service;

import com.foodzie.order_service.dto.CreateOrderRequest;
import com.foodzie.order_service.dto.OrderResponse;
import com.foodzie.order_service.dto.UpdateOrderStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(String userEmail, CreateOrderRequest request);

    List<OrderResponse> getMyOrders(String userEmail);

    /**
     * Returns a paginated list of all orders for the authenticated user.
     * Ordered newest-first.
     *
     * @param userEmail the authenticated user's email
     * @param pageable  page number, size, and optional sort overrides
     */
    Page<OrderResponse> getMyOrders(String userEmail, Pageable pageable);

    OrderResponse getOrderById(String orderId, String userEmail);

    OrderResponse updateOrderStatus(String orderId, UpdateOrderStatusRequest request);

    /**
     * Returns a paginated list of all orders belonging to a restaurant.
     * Ordered newest-first. Accessible to ROLE_RESTAURANT and ROLE_ADMIN only.
     *
     * @param restaurantId the restaurant's ID (e.g. "REST-9942")
     * @param pageable     page number, size, and optional sort overrides
     */
    Page<OrderResponse> getOrdersByRestaurant(String restaurantId, Pageable pageable);
}
