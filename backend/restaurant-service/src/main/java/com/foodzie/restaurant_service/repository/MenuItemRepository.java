package com.foodzie.restaurant_service.repository;

import com.foodzie.restaurant_service.data.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends MongoRepository<MenuItem, String> {

    /** All items for a restaurant — used by management endpoints. */
    List<MenuItem> findAllByRestaurantId(String restaurantId);

    /** Paginated items for a restaurant — used by admin menu endpoint. */
    Page<MenuItem> findAllByRestaurantId(String restaurantId, Pageable pageable);

    /** Only available items — used by the public menu endpoint. */
    List<MenuItem> findAllByRestaurantIdAndAvailableTrue(String restaurantId);

    Optional<MenuItem> findByIdAndRestaurantId(String id, String restaurantId);
}
