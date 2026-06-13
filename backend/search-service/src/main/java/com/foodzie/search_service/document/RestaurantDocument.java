package com.foodzie.search_service.document;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

/**
 * Elasticsearch document mapped to the "restaurants" index.
 *
 * Matches the schema written by restaurant-service's RestaurantServiceImpl#indexRestaurant()
 * exactly — includes nested menu items for cross-field searching.
 *
 * restaurant-service writes to this index whenever a restaurant or menu item changes.
 * search-service only reads from it.
 */
@Document(indexName = "restaurants", createIndex = false)
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestaurantDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String ownerEmail;

    @Field(type = FieldType.Text)
    private String addressText;

    /** Raw latitude — used to build the geo_distance filter via script. Must have doc_values=true for script access. */
    @Field(type = FieldType.Double, docValues = true)
    private double latitude;

    /** Raw longitude — used in scripts. Must have doc_values=true for script access. */
    @Field(type = FieldType.Double, docValues = true)
    private double longitude;

    @Field(type = FieldType.Text)
    private String imageUrl;

    /** Average rating for the restaurant (0.0 to 5.0). */
    @Field(type = FieldType.Double)
    private Double rating;

    /** Total number of ratings received. */
    @Field(type = FieldType.Integer)
    private Integer totalRatings;

    /** Promotional discount string, e.g. "20% OFF". */
    @Field(type = FieldType.Keyword)
    private String discount;

    /**
     * Nested menu items with full details for cross-field searching.
     * Allows searching on menu item names and descriptions.
     * Defaults to empty list.
     */
    @Field(type = FieldType.Nested, includeInParent = true)
    @Builder.Default
    private List<MenuItemIndexDoc> menuItems = new java.util.ArrayList<>();

    /**
     * Nested menu item document for Elasticsearch indexing.
     * Includes all relevant fields for search and faceting.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MenuItemIndexDoc {
        @Field(type = FieldType.Keyword)
        private String id;

        @Field(type = FieldType.Text, analyzer = "standard")
        private String name;

        @Field(type = FieldType.Text, analyzer = "standard")
        private String description;

        @Field(type = FieldType.Keyword)
        private String category;

        @Field(type = FieldType.Double)
        private Double price;

        @Field(type = FieldType.Boolean)
        private boolean available;

        @Field(type = FieldType.Boolean)
        @JsonProperty("isVeg")
        private boolean isVeg;

        @Field(type = FieldType.Double)
        private Double rating;

        @Field(type = FieldType.Integer)
        private Integer totalRatings;
    }
}
