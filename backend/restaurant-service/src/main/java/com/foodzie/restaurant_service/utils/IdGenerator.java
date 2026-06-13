package com.foodzie.restaurant_service.utils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates human-readable IDs in the format used across the Foodzie catalog.
 *
 * <p>In production you'd replace this with a distributed ID strategy
 * (e.g. a sequence stored in MongoDB or a Snowflake ID). For now a
 * random 4-digit suffix is sufficient for development.
 */
public final class IdGenerator {

    private IdGenerator() {}

    public static String restaurantId() {
        int suffix = (int) (Math.random() * 9000) + 1000;
        return "REST-" + suffix;
    }

    public static String menuItemId() {
        int suffix = (int) (Math.random() * 900) + 100;
        return "ITEM-" + suffix;
    }

    public static String adminUserId() {
        int suffix = (int) (Math.random() * 9000) + 1000;
        return "ADMIN-" + suffix;
    }
}
