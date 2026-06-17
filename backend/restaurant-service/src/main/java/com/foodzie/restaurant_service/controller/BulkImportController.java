package com.foodzie.restaurant_service.controller;

import com.foodzie.restaurant_service.dto.*;
import com.foodzie.restaurant_service.service.RestaurantService;
import com.foodzie.restaurant_service.util.ExcelParsingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Bulk import endpoints for restaurants and menu items via Excel files.
 * Requires ROLE_ADMIN authorization.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/bulk-import")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class BulkImportController {

    private final RestaurantService restaurantService;

    /**
     * POST /api/admin/bulk-import/restaurants
     * Upload an Excel file to create multiple restaurants.
     * 
     * Expected columns in Excel:
     * - name (required)
     * - description (optional)
     * - addressText (required)
     * - latitude (required)
     * - longitude (required)
     * - gstNo (required) — format: 22AAAAA0000A1Z5
     * - fssaiNo (required) — 14 digits
     * - ownerEmail (required) — must be unique
     * - imageUrl (optional) — restaurant banner image URL
     */
    @PostMapping("/restaurants")
    public ResponseEntity<ApiResponse<BulkRestaurantImportResponse>> importRestaurants(
            @RequestParam("file") MultipartFile file) {
        
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }
            
            if (!file.getOriginalFilename().endsWith(".xlsx") && !file.getOriginalFilename().endsWith(".xls")) {
                throw new IllegalArgumentException("Only .xlsx and .xls files are supported");
            }
            
            // Parse Excel file
            List<Map<String, String>> restaurantDataList = ExcelParsingUtil.parseRestaurantExcel(file);
            List<BulkRestaurantImportResponse.RestaurantImportResult> results = new ArrayList<>();
            
            int successCount = 0;
            int failureCount = 0;
            
            for (Map<String, String> data : restaurantDataList) {
                try {
                    // Validate required fields
                    String name = data.get("name");
                    String addressText = data.get("addressText");
                    String gstNo = data.get("gstNo");
                    String fssaiNo = data.get("fssaiNo");
                    String ownerEmail = data.get("ownerEmail");
                    String latitude = data.get("latitude");
                    String longitude = data.get("longitude");
                    
                    if (name == null || name.isEmpty()) {
                        throw new IllegalArgumentException("name is required");
                    }
                    if (addressText == null || addressText.isEmpty()) {
                        throw new IllegalArgumentException("addressText is required");
                    }
                    if (gstNo == null || gstNo.isEmpty()) {
                        throw new IllegalArgumentException("gstNo is required");
                    }
                    if (fssaiNo == null || fssaiNo.isEmpty()) {
                        throw new IllegalArgumentException("fssaiNo is required");
                    }
                    if (ownerEmail == null || ownerEmail.isEmpty()) {
                        throw new IllegalArgumentException("ownerEmail is required");
                    }
                    
                    Double lat = Double.parseDouble(latitude);
                    Double lng = Double.parseDouble(longitude);
                    
                    // Create restaurant
                    CreateRestaurantRequest request = new CreateRestaurantRequest();
                    request.setName(name);
                    request.setDescription(data.get("description") != null ? data.get("description") : "");
                    request.setAddressText(addressText);
                    request.setLatitude(lat);
                    request.setLongitude(lng);
                    request.setGstNo(gstNo);
                    request.setFssaiNo(fssaiNo);
                    
                    // Set image URL if provided
                    String imageUrl = data.get("imageUrl");
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        request.setImageUrl(imageUrl);
                    }
                    
                    // Note: In a real scenario, ownerEmail would be set by the service based on JWT subject
                    // For now, we use the createRestaurant method which uses JWT subject
                    RestaurantResponse response = restaurantService.createRestaurant(ownerEmail, request);
                    
                    results.add(BulkRestaurantImportResponse.RestaurantImportResult.builder()
                            .rowNumber(Integer.parseInt(data.get("rowNumber")))
                            .name(name)
                            .status("SUCCESS")
                            .message("Restaurant created successfully")
                            .restaurantId(response.getId())
                            .build());
                    
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    results.add(BulkRestaurantImportResponse.RestaurantImportResult.builder()
                            .rowNumber(Integer.parseInt(data.get("rowNumber")))
                            .name(data.get("name") != null ? data.get("name") : "Unknown")
                            .status("FAILED")
                            .message(e.getMessage())
                            .build());
                    
                    log.warn("Failed to import restaurant at row {}: {}", data.get("rowNumber"), e.getMessage());
                }
            }
            
            BulkRestaurantImportResponse response = BulkRestaurantImportResponse.builder()
                    .totalRows(restaurantDataList.size())
                    .successCount(successCount)
                    .failureCount(failureCount)
                    .results(results)
                    .build();
            
            return ResponseEntity.ok(ApiResponse.ok("Restaurants imported", response));
            
        } catch (IOException e) {
            log.error("Error reading Excel file: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<BulkRestaurantImportResponse>builder()
                            .success(false)
                            .message("Error reading Excel file: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Error importing restaurants: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<BulkRestaurantImportResponse>builder()
                            .success(false)
                            .message("Error importing restaurants: " + e.getMessage())
                            .build());
        }
    }

    /**
     * POST /api/admin/bulk-import/menu/{restaurantId}
     * Upload an Excel file to create multiple menu items for a restaurant.
     * 
     * Expected columns in Excel:
     * - name (required)
     * - description (optional)
     * - price (required) — numeric
     * - category (required) — e.g., "Burger", "Pizza", "Drinks"
     * - isVeg (required) — "true" or "false"
     * - imageUrl (optional) — image URL
     */
    @PostMapping("/menu/{restaurantId}")
    public ResponseEntity<ApiResponse<BulkMenuImportResponse>> importMenu(
            @PathVariable String restaurantId,
            @RequestParam("file") MultipartFile file) {
        
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }
            
            if (!file.getOriginalFilename().endsWith(".xlsx") && !file.getOriginalFilename().endsWith(".xls")) {
                throw new IllegalArgumentException("Only .xlsx and .xls files are supported");
            }
            
            // Parse Excel file
            List<Map<String, String>> menuDataList = ExcelParsingUtil.parseMenuExcel(file);
            List<BulkMenuImportResponse.MenuImportResult> results = new ArrayList<>();
            
            int successCount = 0;
            int failureCount = 0;
            
            for (Map<String, String> data : menuDataList) {
                try {
                    // Validate required fields
                    String name = data.get("name");
                    String price = data.get("price");
                    String category = data.get("category");
                    String isVeg = data.get("isVeg");
                    
                    if (name == null || name.isEmpty()) {
                        throw new IllegalArgumentException("name is required");
                    }
                    if (price == null || price.isEmpty()) {
                        throw new IllegalArgumentException("price is required");
                    }
                    if (category == null || category.isEmpty()) {
                        throw new IllegalArgumentException("category is required");
                    }
                    if (isVeg == null || isVeg.isEmpty()) {
                        throw new IllegalArgumentException("isVeg is required (true/false)");
                    }
                    
                    int priceInt = Integer.parseInt(price);
                    boolean isVegBool = Boolean.parseBoolean(isVeg);
                    
                    // Create menu item
                    AddMenuItemRequest request = new AddMenuItemRequest();
                    request.setName(name);
                    request.setDescription(data.get("description") != null ? data.get("description") : "");
                    request.setPrice(new BigDecimal(price));
                    request.setCategory(category);
                    request.setVeg(isVegBool);
                    
                    // Set image URL if provided
                    String imageUrl = data.get("imageUrl");
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        request.setImageUrl(imageUrl);
                    }
                    
                    // Note: In a real scenario, you'd extract ownerEmail from JWT
                    // For now, using empty string as placeholder
                    MenuItemResponse response = restaurantService.addMenuItem(restaurantId, "", request);
                    
                    results.add(BulkMenuImportResponse.MenuImportResult.builder()
                            .rowNumber(Integer.parseInt(data.get("rowNumber")))
                            .itemName(name)
                            .status("SUCCESS")
                            .message("Menu item created successfully")
                            .itemId(response.getId())
                            .build());
                    
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    results.add(BulkMenuImportResponse.MenuImportResult.builder()
                            .rowNumber(Integer.parseInt(data.get("rowNumber")))
                            .itemName(data.get("name") != null ? data.get("name") : "Unknown")
                            .status("FAILED")
                            .message(e.getMessage())
                            .build());
                    
                    log.warn("Failed to import menu item at row {}: {}", data.get("rowNumber"), e.getMessage());
                }
            }
            
            BulkMenuImportResponse response = BulkMenuImportResponse.builder()
                    .restaurantId(restaurantId)
                    .totalRows(menuDataList.size())
                    .successCount(successCount)
                    .failureCount(failureCount)
                    .results(results)
                    .build();
            
            return ResponseEntity.ok(ApiResponse.ok("Menu items imported", response));
            
        } catch (IOException e) {
            log.error("Error reading Excel file: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<BulkMenuImportResponse>builder()
                            .success(false)
                            .message("Error reading Excel file: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Error importing menu items: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<BulkMenuImportResponse>builder()
                            .success(false)
                            .message("Error importing menu items: " + e.getMessage())
                            .build());
        }
    }
}
