package com.foodzie.restaurant_service.data;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Primary document for a restaurant.
 * ID is a human-readable slug (e.g. "REST-9942") assigned at onboarding.
 * Menu items are stored in a separate collection and reference this ID.
 */
@Document(collection = "restaurants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant {

    /** Human-readable ID assigned at onboarding, e.g. "REST-9942". */
    @Id
    private String id;

    /** Email of the owner — links to user-service. */
    @Indexed
    @Field("owner_email")
    private String ownerEmail;

    @Field("name")
    private String name;

    @Field("description")
    private String description;

    @Field("address_text")
    private String addressText;

    /** Latitude — stored as part of a GeoJSON point for geospatial queries. */
    private double latitude;

    /** Longitude — stored as part of a GeoJSON point for geospatial queries. */
    private double longitude;

    @Field("image_url")
    private String imageUrl;

    /**
     * Cloudinary public_id for this image.
     * Required to delete or replace the image via the Cloudinary API.
     * Null until an image is uploaded.
     */
    @Field("image_public_id")
    private String imagePublicId;

    @Builder.Default
    private RestaurantStatus status = RestaurantStatus.CLOSED;

    @Field("gst_no")
    private String gstNo;
    @Field("fssai_no")
    private String fssaiNo;

    /**
     * Average rating for the restaurant (0.0 to 5.0).
     * Calculated from user reviews; starts at 0 when no ratings exist.
     */
    @Builder.Default
    @Field("rating")
    private Double rating = 0.0;

    /**
     * Total number of ratings/reviews received for this restaurant.
     * Used to compute the average rating and show rating confidence.
     */
    @Builder.Default
    @Field("total_ratings")
    private Integer totalRatings = 0;

    /**
     * Promotional discount string, e.g. "20% OFF" or "Free Delivery".
     * Null or empty when no active promotional offer.
     */
    @Field("discount")
    private String discount;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    /**
     * Embedded list of menu items for this restaurant.
     * Stored directly in the restaurant document.
     */
    @Builder.Default
    @Field("menu_items")
    private List<MenuItem> menuItems = new ArrayList<>();
}
