package com.foodzie.delivery_acceptance_service.controller;

import com.foodzie.delivery_acceptance_service.dto.AcceptOfferRequest;
import com.foodzie.delivery_acceptance_service.dto.AcceptOfferResponse;
import com.foodzie.delivery_acceptance_service.dto.ApiResponse;
import com.foodzie.delivery_acceptance_service.service.DriverAcceptanceService;
import com.foodzie.delivery_acceptance_service.service.DriverSseRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST + SSE API for the driver-facing acceptance flow.
 *
 * SSE (driver app opens on startup):
 *   GET /api/delivery/drivers/{driverId}/offers/stream
 *   → Persistent stream; server pushes "delivery_offer" events when a new
 *     order is available for this driver to accept.
 *
 * Accept (driver taps "Accept"):
 *   POST /api/delivery/offers/accept
 *   → Attempts the distributed lock. Returns assigned=true/false.
 */
@Slf4j
@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
public class DriverOfferController {

    private final DriverSseRegistry sseRegistry;
    private final DriverAcceptanceService acceptanceService;

    // ── SSE stream ────────────────────────────────────────────────────────────

    /**
     * GET /api/delivery/drivers/{driverId}/offers/stream
     *
     * The driver app opens this connection when the driver clocks in.
     * The server pushes delivery offers down this pipe in real time.
     * EventSource on the client reconnects automatically on disconnect.
     */
    @GetMapping(
            value = "/drivers/{driverId}/offers/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_DRIVER')")
    public SseEmitter streamOffers(@PathVariable String driverId) {
        log.info("SSE stream requested: driverId={}", driverId);
        return sseRegistry.register(driverId);
    }

    // ── Accept offer ──────────────────────────────────────────────────────────

    /**
     * POST /api/delivery/offers/accept
     *
     * Called when the driver taps "Accept" on the offer screen.
     * Uses a Redis distributed lock (SET NX EX) to ensure only one driver wins.
     *
     * Response:
     *   200 + assigned=true  → this driver won, order is theirs
     *   409 + assigned=false → another driver was faster
     */
    @PostMapping("/offers/accept")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_DRIVER')")
    public ResponseEntity<ApiResponse<AcceptOfferResponse>> acceptOffer(
            @Valid @RequestBody AcceptOfferRequest request) {

        log.info("Accept attempt: orderId={} driverId={}", request.getOrderId(), request.getDriverId());

        AcceptOfferResponse result = acceptanceService.acceptOffer(request);

        if (result.isAssigned()) {
            return ResponseEntity.ok(ApiResponse.ok("Order assigned successfully", result));
        } else {
            // 409 Conflict — race condition lost
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.<AcceptOfferResponse>builder()
                            .success(false)
                            .message(result.getMessage())
                            .data(result)
                            .build());
        }
    }

    /**
     * POST /api/delivery/offers/{orderId}/decline
     *
     * Called when the driver taps "Decline" on the offer screen.
     * Removes the offer from the driver's view without affecting availability for other drivers.
     */
    @PostMapping("/offers/{orderId}/decline")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_DRIVER')")
    public ResponseEntity<ApiResponse<Void>> declineOffer(
            @PathVariable String orderId,
            @RequestParam String driverId) {

        log.info("Decline attempt: orderId={} driverId={}", orderId, driverId);
        acceptanceService.declineOffer(orderId, driverId);

        return ResponseEntity.ok(ApiResponse.ok("Offer declined"));
    }
}
