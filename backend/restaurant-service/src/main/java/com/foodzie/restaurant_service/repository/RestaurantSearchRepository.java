package com.foodzie.restaurant_service.repository;

import com.foodzie.restaurant_service.data.RestaurantDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantSearchRepository extends ElasticsearchRepository<RestaurantDocument, String> {

    /**
     * Full-text search across restaurant name, description, and nested menu item names.
     * 
     * This custom query method uses Spring Data Elasticsearch's simple query DSL.
     * For nested queries, custom @Query annotations may be needed if complex nested queries are required.
     * Currently relies on Spring Data's field mapping to handle nested searches.
     * 
     * The menuItems field is a nested type with name, description, and category sub-fields.
     */
    List<RestaurantDocument> findByNameContainingOrDescriptionContaining(
            String name, String description);
}
