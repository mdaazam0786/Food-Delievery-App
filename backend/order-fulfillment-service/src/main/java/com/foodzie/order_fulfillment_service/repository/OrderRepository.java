package com.foodzie.order_fulfillment_service.repository;

import com.foodzie.order_fulfillment_service.data.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
}
