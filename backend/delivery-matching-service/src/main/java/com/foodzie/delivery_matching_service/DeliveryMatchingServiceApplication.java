package com.foodzie.delivery_matching_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableDiscoveryClient
@EnableRetry
public class DeliveryMatchingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeliveryMatchingServiceApplication.class, args);
    }
}
