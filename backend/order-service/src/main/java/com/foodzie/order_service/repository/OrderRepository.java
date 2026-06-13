package com.foodzie.order_service.repository;

import com.foodzie.order_service.data.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    /** Returns all orders for a user, newest first. */
    List<Order> findAllByUserEmailOrderByCreatedAtDesc(String userEmail);

    /**
     * Returns a paginated slice of orders for a given user, newest first.
     * Used by the user profile to display order history.
     */
    Page<Order> findAllByUserEmailOrderByCreatedAtDesc(String userEmail, Pageable pageable);

    /**
     * Returns a paginated slice of orders for a given restaurant, newest first.
     * Used by the restaurant admin panel to display and manage incoming orders.
     */
    Page<Order> findAllByRestaurantIdOrderByCreatedAtDesc(String restaurantId, Pageable pageable);
}
