package com.foodzie.restaurant_service.repository;

import com.foodzie.restaurant_service.data.RestaurantDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantSearchRepository extends MongoRepository<RestaurantDocument, String> {

    /**
     * Full-text search across restaurant name and description using MongoDB text search.
     * Requires a text index on the name and description fields.
     */
    @Query("{ $text: { $search: ?0 } }")
    List<RestaurantDocument> searchByText(String searchTerm);

    /**
     * Find restaurants by name containing the given string.
     */
    List<RestaurantDocument> findByNameContainingIgnoreCase(String name);

    /**
     * Find restaurants by description containing the given string.
     */
    List<RestaurantDocument> findByDescriptionContainingIgnoreCase(String description);
}
