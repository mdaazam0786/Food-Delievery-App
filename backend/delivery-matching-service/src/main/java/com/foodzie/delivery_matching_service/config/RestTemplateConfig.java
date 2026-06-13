package com.foodzie.delivery_matching_service.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * Load-balanced RestTemplate — resolves "restaurant-service" via Eureka.
     * Used to fetch restaurant lat/lon for the GEOSEARCH origin point.
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
