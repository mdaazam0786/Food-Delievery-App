package com.foodzie.delivery_acceptance_service.service;

import com.foodzie.delivery_acceptance_service.dto.OfferPayload;
import com.foodzie.delivery_acceptance_service.event.DeliveryMatchingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Manages the multi-driver broadcast phase.
 *
 * When the delivery-matching-service publishes a DELIVERY_MATCHING_INITIATED event,
 * this service:
 *   1. Stores the offer context in Redis (orderId → metadata) with the offer TTL.
 *      This is the "pending offer" key — its expiry triggers the timeout flow.
 *   2. Pushes an SSE event to each candidate driver's open connection.
 *
 * The offer is sent to all candidates simultaneously (non-blocking) — the first
 * driver to tap Accept wins via the distributed lock in DriverAcceptanceService.
 *
 * Redis key: offer:pending:{orderId}
 *   Value: "{restaurantId}:{cityZone}:{radiusKm}"  (compact string, no JSON overhead)
 *   TTL:   app.acceptance.offer-ttl-seconds (default 30s)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfferBroadcastService {

    private static final String OFFER_KEY_PREFIX = "offer:pending:";

    private final RedisTemplate<String, String> redisTemplate;
    private final DriverSseRegistry sseRegistry;

    @Value("${app.acceptance.offer-ttl-seconds}")
    private int offerTtlSeconds;

    /**
     * Broadcasts the delivery offer to all candidate drivers simultaneously.
     * Called by the Kafka listener after consuming a DELIVERY_MATCHING_INITIATED event.
     */
    public void broadcastOffer(DeliveryMatchingEvent event) {
        DeliveryMatchingEvent.Payload p = event.getPayload();
        String orderId = p.getOrderId();

        // 1. Store offer context in Redis with TTL.
        //    Expiry is detected by RedisOfferExpiryListener → triggers timeout flow.
        String offerValue = p.getRestaurantId() + ":" + p.getCityZone() + ":" + p.getSearchRadiusKm();
        redisTemplate.opsForValue().set(
                OFFER_KEY_PREFIX + orderId,
                offerValue,
                Duration.ofSeconds(offerTtlSeconds)
        );
        log.info("Offer stored in Redis: orderId={} ttl={}s candidates={}",
                orderId, offerTtlSeconds, p.getCandidateDriverIds().size());

        // 2. Push SSE offer to each candidate driver simultaneously.
        //    Drivers without an active SSE connection are skipped — they will
        //    not receive this round's offer (treated as implicit rejection).
        List<String> candidates = p.getCandidateDriverIds();
        int notified = 0;
        for (String driverId : candidates) {
            OfferPayload offer = OfferPayload.builder()
                    .orderId(orderId)
                    .restaurantId(p.getRestaurantId())
                    .distanceToRestaurantKm(0.0) // distance not in matching event; driver app calculates from GPS
                    .estimatedPayout(calculatePayout(p.getSearchRadiusKm()))
                    .offerTtlSeconds(offerTtlSeconds)
                    .build();

            boolean sent = sseRegistry.pushOffer(driverId, offer);
            if (sent) notified++;
        }

        log.info("Offer broadcast complete: orderId={} notified={}/{} drivers",
                orderId, notified, candidates.size());
    }

    /**
     * Placeholder payout calculation.
     * A real implementation would call a pricing service.
     * Base: ₹30 + ₹5 per km of search radius.
     */
    private double calculatePayout(double radiusKm) {
        return 30.0 + (radiusKm * 5.0);
    }
}
