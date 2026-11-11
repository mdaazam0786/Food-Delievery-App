package com.foodzie.delivery_matching_service.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    /**
     * Load-balanced RestTemplate — resolves "restaurant-service" via Eureka.
     * Used to fetch restaurant lat/lon for the GEOSEARCH origin point.
     * 
     * Timeout configuration:
     * - Connection timeout: 3 seconds (fail fast if service unreachable)
     * - Read timeout: 8 seconds (allow reasonable processing time)
     * 
     * This prevents blocking Kafka listener threads indefinitely when
     * restaurant-service is slow or unresponsive.
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(8))
                .build();
    }
}
