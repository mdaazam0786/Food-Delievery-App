package com.foodzie.location_update_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Pure Kafka consumer service — no REST controllers, no security layer, no database.
 *
 * Responsibility: drain the driver-location-pings Kafka topic and flush
 * GPS coordinates into Redis using pipelining for maximum throughput.
 *
 * The only HTTP endpoint is /actuator/health for container orchestration.
 */
@SpringBootApplication
public class LocationUpdateServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LocationUpdateServiceApplication.class, args);
    }
}
