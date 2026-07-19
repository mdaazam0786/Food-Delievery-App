package com.foodzie.restaurant_service.repository;

import com.foodzie.restaurant_service.data.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends MongoRepository<MenuItem, String> {

    /** All items for a restaurant — used by management endpoints. */
    @Query("{ 'restaurant_id': ?0 }")
    List<MenuItem> findAllByRestaurantId(String restaurantId);

    /** Paginated items for a restaurant — used by admin menu endpoint. */
    @Query("{ 'restaurant_id': ?0 }")
    Page<MenuItem> findAllByRestaurantId(String restaurantId, Pageable pageable);

    /** Only available items — used by the public menu endpoint. */
    List<MenuItem> findAllByRestaurantIdAndAvailableTrue(String restaurantId);

    Optional<MenuItem> findByIdAndRestaurantId(String id, String restaurantId);
}
