package com.foodzie.websocket_manager.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory map of driverId → orderId for drivers currently OUT_FOR_DELIVERY.
 *
 * This is the filter that makes the Kafka consumer efficient.
 * Without it, every ping from every driver would require a MongoDB lookup.
 * With it, 99% of pings are discarded in O(1) with a single map lookup.
 *
 * Lifecycle:
 *   Entry added:   when order-events carries newStatus=OUT_FOR_DELIVERY
 *   Entry removed: when order-events carries newStatus=DELIVERED
 *
 * Populated by: OrderEventListener (consumes order-events topic)
 *
 * Scaling note: This map is local to one pod. In a multi-pod deployment,
 * each pod independently consumes order-events (same consumer group) and
 * maintains its own copy. This is correct because:
 *   - All pods need to filter pings (they all consume the full ping topic)
 *   - The Redis backplane handles cross-pod WebSocket delivery
 */
@Slf4j
@Component
public class ActiveDeliveryRegistry {

    /** driverId → orderId */
    private final Map<String, String> activeDeliveries = new ConcurrentHashMap<>();

    /**
     * Registers a driver as actively delivering an order.
     * Called when order status transitions to OUT_FOR_DELIVERY.
     */
    public void register(String driverId, String orderId) {
        activeDeliveries.put(driverId, orderId);
        log.info("Active delivery registered: driverId={} orderId={}", driverId, orderId);
    }

    /**
     * Removes a driver from the active delivery map.
     * Called when order status transitions to DELIVERED.
     */
    public void deregister(String driverId) {
        String orderId = activeDeliveries.remove(driverId);
        if (orderId != null) {
            log.info("Active delivery deregistered: driverId={} orderId={}", driverId, orderId);
        }
    }

    /**
     * Returns the orderId for the given driver, or null if not on an active delivery.
     * Called on every Kafka ping — must be O(1).
     */
    public String getOrderId(String driverId) {
        return activeDeliveries.get(driverId);
    }

    public int activeCount() {
        return activeDeliveries.size();
    }
}
