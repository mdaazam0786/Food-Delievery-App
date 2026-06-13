package com.foodzie.websocket_manager.repository;

import com.foodzie.websocket_manager.data.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
}
