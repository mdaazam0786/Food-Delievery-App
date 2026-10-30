package com.foodzie.user_service.controller;

import com.foodzie.common.dto.ApiResponse;
import com.foodzie.common.exceptions.*;
import com.foodzie.user_service.dto.AddressRequest;
import com.foodzie.user_service.dto.AddressResponse;
import com.foodzie.user_service.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    /**
     * GET /api/v1/address
     * Returns all saved addresses for the authenticated user.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(@AuthenticationPrincipal Jwt jwt) {
        try {
            log.debug("Retrieving user addresses");
            
            // Validate input
            if (jwt == null) {
                log.warn("JWT token is null");
                throw new UnauthorizedException("Authentication required");
            }
            
            String userId = jwt.getClaim("userId");
            
            if (userId == null || userId.isBlank()) {
                log.warn("User ID is missing from JWT token");
                throw new UnauthorizedException("Invalid JWT: userId claim required");
            }
            
            List<AddressResponse> response = addressService.getAddresses(userId);
            log.debug("User addresses retrieved successfully");
            return ResponseEntity.ok(ApiResponse.ok(response));
            
        } catch (UnauthorizedException e) {
            log.warn("Unauthorized access to addresses: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error retrieving addresses", e);
            throw new InternalServerException("Failed to retrieve addresses", e);
        }
    }

    /**
     * POST /api/v1/address
     * Saves a new address for the authenticated user.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddressRequest request) {
        try {
            log.info("Adding new address");
            
            // Validate input
            if (jwt == null) {
                log.warn("JWT token is null");
                throw new UnauthorizedException("Authentication required");
            }
            if (request == null) {
                log.warn("Address request is null");
                throw new BadRequestException("Address request cannot be empty");
            }
            
            String userId = jwt.getClaim("userId");
            
            if (userId == null || userId.isBlank()) {
                log.warn("User ID is missing from JWT token");
                throw new UnauthorizedException("Invalid JWT: userId claim required");
            }
            
            AddressResponse response = addressService.addAddress(userId, request);
            log.info("New address added successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Address saved", response));
            
        } catch (BadRequestException | UnauthorizedException e) {
            log.warn("Bad request during address addition: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error adding address", e);
            throw new InternalServerException("Failed to add address", e);
        }
    }

    /**
     * PUT /api/v1/address/{addressId}
     * Updates an existing address. Returns 404 if the address doesn't belong to the user.
     */
    @PutMapping("/{addressId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String addressId,
            @Valid @RequestBody AddressRequest request) {
        try {
            log.info("Updating address: addressId={}", addressId);
            
            // Validate input
            if (jwt == null) {
                log.warn("JWT token is null");
                throw new UnauthorizedException("Authentication required");
            }
            if (addressId == null || addressId.isBlank()) {
                log.warn("Address ID is null or blank");
                throw BadRequestException.of("addressId", "Address ID is required");
            }
            if (request == null) {
                log.warn("Address request is null");
                throw new BadRequestException("Address request cannot be empty");
            }
            
            String userId = jwt.getClaim("userId");
            
            if (userId == null || userId.isBlank()) {
                log.warn("User ID is missing from JWT token");
                throw new UnauthorizedException("Invalid JWT: userId claim required");
            }
            
            Long addressIdLong;
            try {
                addressIdLong = Long.valueOf(addressId);
            } catch (NumberFormatException e) {
                log.warn("Invalid address ID format: {}", addressId);
                throw BadRequestException.of("addressId", "Address ID must be a valid number");
            }
            
            AddressResponse response = addressService.updateAddress(userId, addressIdLong, request);
            log.info("Address updated successfully");
            return ResponseEntity.ok(ApiResponse.ok("Address updated", response));
            
        } catch (BadRequestException | UnauthorizedException | ResourceNotFoundException e) {
            log.warn("Error during address update: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating address", e);
            throw new InternalServerException("Failed to update address", e);
        }
    }

    /**
     * DELETE /api/v1/address/{addressId}
     * Deletes a specific address. Returns 404 if the address doesn't belong to the user.
     */
    @DeleteMapping("/{addressId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String addressId) {
        try {
            log.info("Deleting address: addressId={}", addressId);
            
            // Validate input
            if (jwt == null) {
                log.warn("JWT token is null");
                throw new UnauthorizedException("Authentication required");
            }
            if (addressId == null || addressId.isBlank()) {
                log.warn("Address ID is null or blank");
                throw BadRequestException.of("addressId", "Address ID is required");
            }
            
            String userId = jwt.getClaim("userId");
            
            if (userId == null || userId.isBlank()) {
                log.warn("User ID is missing from JWT token");
                throw new UnauthorizedException("Invalid JWT: userId claim required");
            }
            
            Long addressIdLong;
            try {
                addressIdLong = Long.valueOf(addressId);
            } catch (NumberFormatException e) {
                log.warn("Invalid address ID format: {}", addressId);
                throw BadRequestException.of("addressId", "Address ID must be a valid number");
            }
            
            addressService.deleteAddress(userId, addressIdLong);
            log.info("Address deleted successfully");
            return ResponseEntity.ok(ApiResponse.ok("Address deleted", null));
            
        } catch (BadRequestException | UnauthorizedException | ResourceNotFoundException e) {
            log.warn("Error during address deletion: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error deleting address", e);
            throw new InternalServerException("Failed to delete address", e);
        }
    }
}
