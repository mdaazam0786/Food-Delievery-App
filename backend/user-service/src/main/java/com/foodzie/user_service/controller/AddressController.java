package com.foodzie.user_service.controller;

import com.foodzie.user_service.dto.AddressRequest;
import com.foodzie.user_service.dto.AddressResponse;
import com.foodzie.user_service.dto.ApiResponse;
import com.foodzie.user_service.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(ApiResponse.ok(addressService.getAddresses(userId)));
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
        Long userId = jwt.getClaim("userId");
        AddressResponse response = addressService.addAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Address saved", response));
    }

    /**
     * PUT /api/v1/address/{addressId}
     * Updates an existing address. Returns 404 if the address doesn't belong to the user.
     */
    @PutMapping("/{addressId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(ApiResponse.ok("Address updated", addressService.updateAddress(userId, addressId, request)));
    }

    /**
     * DELETE /api/v1/address/{addressId}
     * Deletes a specific address. Returns 404 if the address doesn't belong to the user.
     */
    @DeleteMapping("/{addressId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long addressId) {
        Long userId = jwt.getClaim("userId");
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.ok("Address deleted", null));
    }
}
