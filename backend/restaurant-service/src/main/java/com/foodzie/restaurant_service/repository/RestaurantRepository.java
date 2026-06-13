package com.foodzie.restaurant_service.repository;

import com.foodzie.restaurant_service.data.Restaurant;
import com.foodzie.restaurant_service.data.RestaurantStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends MongoRepository<Restaurant, String> {

    Optional<Restaurant> findByIdAndOwnerEmail(String id, String ownerEmail);

    Optional<Restaurant> findByOwnerEmail(String ownerEmail);

    List<Restaurant> findAllByStatus(RestaurantStatus status);
}
