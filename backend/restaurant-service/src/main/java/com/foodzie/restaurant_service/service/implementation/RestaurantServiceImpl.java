package com.foodzie.restaurant_service.service.implementation;

import com.foodzie.restaurant_service.data.MenuItem;
import com.foodzie.restaurant_service.data.Restaurant;
import com.foodzie.restaurant_service.data.RestaurantDocument;
import com.foodzie.restaurant_service.dto.*;
import com.foodzie.restaurant_service.exception.MenuItemNotFoundException;
import com.foodzie.restaurant_service.exception.RestaurantNotFoundException;
import com.foodzie.restaurant_service.repository.MenuItemRepository;
import com.foodzie.restaurant_service.repository.RestaurantRepository;
import com.foodzie.restaurant_service.repository.RestaurantSearchRepository;
import com.foodzie.restaurant_service.service.RestaurantService;
import com.foodzie.restaurant_service.utils.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantSearchRepository searchRepository;

    // ── Management ────────────────────────────────────────────────────────────

    @Override
    public RestaurantResponse createRestaurant(String ownerEmail, CreateRestaurantRequest request) {
        Restaurant restaurant = restaurantRepository.findByOwnerEmail(ownerEmail)
                .orElseGet(() -> Restaurant.builder()
                        .id(IdGenerator.restaurantId())
                        .ownerEmail(ownerEmail)
                        .build());

        applyProvisionFields(restaurant, request);

        Restaurant saved = restaurantRepository.save(restaurant);
        log.info("Restaurant provisioned id={} owner={}", saved.getId(), ownerEmail);

        indexRestaurant(saved, menuItemRepository.findAllByRestaurantId(saved.getId()));
        return toResponse(saved);
    }

    @Override
    public RestaurantResponse getMyRestaurant(String ownerEmail) {
        return restaurantRepository.findByOwnerEmail(ownerEmail)
                .map(this::toResponse)
                .orElseThrow(() -> new RestaurantNotFoundException(
                        "No restaurant found for owner: " + ownerEmail));
    }

    private Restaurant applyProvisionFields(Restaurant restaurant, CreateRestaurantRequest request) {
        restaurant.setName(request.getName());
        restaurant.setDescription(request.getDescription());
        restaurant.setAddressText(request.getAddressText());
        restaurant.setLatitude(request.getLatitude());
        restaurant.setLongitude(request.getLongitude());
        restaurant.setGstNo(request.getGstNo());
        restaurant.setFssaiNo(request.getFssaiNo());
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            restaurant.setImageUrl(request.getImageUrl());
        }
        return restaurant;
    }

    @Override
    public RestaurantResponse updateStatus(String restaurantId, String ownerEmail,
                                           UpdateRestaurantStatusRequest request) {
        Restaurant restaurant = findAndVerifyOwner(restaurantId, ownerEmail);
        restaurant.setStatus(request.getStatus());
        Restaurant saved = restaurantRepository.save(restaurant);

        log.info("Restaurant id={} status → {}", restaurantId, request.getStatus());
        indexRestaurant(saved, menuItemRepository.findAllByRestaurantId(restaurantId));
        return toResponse(saved);
    }

    @Override
    public RestaurantResponse updateRating(String restaurantId, String ownerEmail,
                                           UpdateRestaurantRatingRequest request) {
        Restaurant restaurant = findAndVerifyOwner(restaurantId, ownerEmail);
        
        if (request.getRating() != null) {
            restaurant.setRating(request.getRating());
        }
        if (request.getTotalRatings() != null) {
            restaurant.setTotalRatings(request.getTotalRatings());
        }
        if (request.getDiscount() != null) {
            restaurant.setDiscount(request.getDiscount());
        }
        
        Restaurant saved = restaurantRepository.save(restaurant);
        log.info("Restaurant id={} rating updated: rating={}, totalRatings={}, discount={}", 
                restaurantId, restaurant.getRating(), restaurant.getTotalRatings(), restaurant.getDiscount());
        
        // Re-index so search results reflect the new rating and discount
        indexRestaurant(saved, menuItemRepository.findAllByRestaurantId(restaurantId));
        return toResponse(saved);
    }

    @Override
    public MenuItemResponse addMenuItem(String restaurantId, String ownerEmail,
                                        AddMenuItemRequest request) {
        // Verify the restaurant exists and belongs to this owner
        Restaurant restaurant = findAndVerifyOwner(restaurantId, ownerEmail);

        MenuItem item = MenuItem.builder()
                .id(IdGenerator.menuItemId())
                .restaurantId(restaurantId)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .isVeg(request.isVeg())
                .build();

        MenuItem saved = menuItemRepository.save(item);
        log.info("MenuItem id={} added to restaurant={}", saved.getId(), restaurantId);

        // Re-index the restaurant so the new item name is searchable immediately
        indexRestaurant(restaurant, menuItemRepository.findAllByRestaurantId(restaurantId));
        return toMenuItemResponse(saved);
    }

    @Override
    public MenuItemResponse updateMenuItem(String restaurantId, String itemId,
                                           String ownerEmail, UpdateMenuItemRequest request) {
        findAndVerifyOwner(restaurantId, ownerEmail);

        MenuItem item = menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId)
                .orElseThrow(() -> new MenuItemNotFoundException(
                        "Menu item not found: " + itemId + " in restaurant: " + restaurantId));

        // Patch semantics — only apply non-null fields
        if (request.getName() != null)        item.setName(request.getName());
        if (request.getDescription() != null) item.setDescription(request.getDescription());
        if (request.getPrice() != null)       item.setPrice(request.getPrice());
        if (request.getCategory() != null)    item.setCategory(request.getCategory());
        if (request.getImageUrl() != null)    item.setImageUrl(request.getImageUrl());
        if (request.getAvailable() != null)   item.setAvailable(request.getAvailable());
        if (request.getIsVeg() != null)       item.setVeg(request.getIsVeg());

        MenuItem saved = menuItemRepository.save(item);
        log.info("MenuItem id={} updated in restaurant={}", itemId, restaurantId);

        // Re-index so availability/name changes are reflected in search
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException("Restaurant not found: " + restaurantId));
        indexRestaurant(restaurant, menuItemRepository.findAllByRestaurantId(restaurantId));

        return toMenuItemResponse(saved);
    }

    @Override
    public org.springframework.data.domain.Page<MenuItemResponse> getMenuItemsByOwner(String restaurantId, String ownerEmail, org.springframework.data.domain.Pageable pageable) {
        // Verify the restaurant exists and belongs to this owner
        findAndVerifyOwner(restaurantId, ownerEmail);
        
        // Use repository's paginated query
        return menuItemRepository.findAllByRestaurantId(restaurantId, pageable)
                .map(this::toMenuItemResponse);
    }

    // ── Public ────────────────────────────────────────────────────────────────

    @Override
    public RestaurantResponse getRestaurant(String restaurantId) {
        return toResponse(restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(
                        "Restaurant not found: " + restaurantId)));
    }

    @Override
    public List<MenuItemResponse> getPublicMenu(String restaurantId) {
        // Verify the restaurant exists before returning items
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new RestaurantNotFoundException("Restaurant not found: " + restaurantId);
        }
        // Return all menu items for the restaurant
        return menuItemRepository.findAllByRestaurantId(restaurantId)
                .stream()
                .map(this::toMenuItemResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void reindexRestaurant(String restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException("Restaurant not found: " + restaurantId));
        indexRestaurant(restaurant, menuItemRepository.findAllByRestaurantId(restaurantId));
    }

    @Override
    public MenuItemResponse uploadMenuItemImage(String itemId, String ownerEmail, String imageUrl, String imagePublicId) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new MenuItemNotFoundException("Menu item not found: " + itemId));

        Restaurant restaurant = restaurantRepository.findById(item.getRestaurantId())
                .orElseThrow(() -> new RestaurantNotFoundException("Restaurant not found"));

        // Verify ownership
        if (!restaurant.getOwnerEmail().equals(ownerEmail)) {
            throw new AccessDeniedException("You do not own this restaurant");
        }

        // Update the menu item with the image URL
        item.setImageUrl(imageUrl);
        item.setImagePublicId(imagePublicId);
        MenuItem saved = menuItemRepository.save(item);

        // Re-index to Elasticsearch
        indexRestaurant(restaurant, menuItemRepository.findAllByRestaurantId(item.getRestaurantId()));

        log.info("Menu item id={} image updated: {}", itemId, imageUrl);
        return toMenuItemResponse(saved);
    }

    // ── Elasticsearch indexing ────────────────────────────────────────────────

    /**
     * Builds a denormalized {@link RestaurantDocument} and upserts it into Elasticsearch.
     * Called after every write operation so the search index stays in sync.
     */
    @Async
    private void indexRestaurant(Restaurant restaurant, List<MenuItem> items) {
        try {
            List<RestaurantDocument.MenuItemIndexDoc> menuItemDocs = items != null
                    ? items.stream()
                        .filter(MenuItem::isAvailable)
                        .map(item -> RestaurantDocument.MenuItemIndexDoc.builder()
                                .id(item.getId())
                                .name(item.getName())
                                .description(item.getDescription())
                                .category(item.getCategory())
                                .price(item.getPrice() != null ? item.getPrice().doubleValue() : 0.0)
                                .available(item.isAvailable())
                                .isVeg(item.isVeg())
                                .rating(item.getRating())
                                .totalRatings(item.getTotalRatings())
                                .build())
                        .collect(Collectors.toList())
                    : new java.util.ArrayList<>();

            RestaurantDocument doc = RestaurantDocument.builder()
                    .id(restaurant.getId())
                    .name(restaurant.getName())
                    .description(restaurant.getDescription())
                    .status(restaurant.getStatus().name())
                    .ownerEmail(restaurant.getOwnerEmail())
                    .addressText(restaurant.getAddressText())
                    .latitude(restaurant.getLatitude())
                    .longitude(restaurant.getLongitude())
                    .imageUrl(restaurant.getImageUrl())
                    .rating(restaurant.getRating())
                    .totalRatings(restaurant.getTotalRatings())
                    .discount(restaurant.getDiscount())
                    .menuItems(menuItemDocs)
                    .build();

            searchRepository.save(doc);
            log.debug("Elasticsearch index updated for restaurant={}", restaurant.getId());
        } catch (Exception e) {
            // Search index failure must never break the primary write path.
            log.error("Failed to index restaurant={} in Elasticsearch: {}", restaurant.getId(), e.getMessage(), e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Restaurant findAndVerifyOwner(String restaurantId, String ownerEmail) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(
                        "Restaurant not found: " + restaurantId));
        if (!restaurant.getOwnerEmail().equals(ownerEmail)) {
            throw new AccessDeniedException("You do not own this restaurant");
        }
        return restaurant;
    }

    private RestaurantResponse toResponse(Restaurant r) {
        List<MenuItemResponse> menuItems = r.getMenuItems() == null
                ? List.of()
                : r.getMenuItems().stream()
                    .map(this::toMenuItemResponse)
                    .collect(Collectors.toList());

        return RestaurantResponse.builder()
                .id(r.getId())
                .ownerEmail(r.getOwnerEmail())
                .name(r.getName())
                .description(r.getDescription())
                .addressText(r.getAddressText())
                .latitude(r.getLatitude())
                .longitude(r.getLongitude())
                .imageUrl(r.getImageUrl())
                .imagePublicId(r.getImagePublicId())
                .gstNo(r.getGstNo())
                .fssaiNo(r.getFssaiNo())
                .status(r.getStatus())
                .rating(r.getRating())
                .totalRatings(r.getTotalRatings())
                .discount(r.getDiscount())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .menuItems(menuItems)
                .build();
    }

    private MenuItemResponse toMenuItemResponse(MenuItem i) {
        return MenuItemResponse.builder()
                .id(i.getId())
                .restaurantId(i.getRestaurantId())
                .name(i.getName())
                .description(i.getDescription())
                .price(i.getPrice())
                .category(i.getCategory())
                .imageUrl(i.getImageUrl())
                .imagePublicId(i.getImagePublicId())
                .available(i.isAvailable())
                .rating(i.getRating())
                .totalRatings(i.getTotalRatings())
                .isVeg(i.isVeg())
                .updatedAt(i.getUpdatedAt())
                .build();
    }
}
