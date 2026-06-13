package com.foodzie.cart_service.repository;

import com.foodzie.cart_service.data.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends MongoRepository<Cart, String> {

    Optional<Cart> findByUserEmail(String userEmail);

    void deleteByUserEmail(String userEmail);
}
