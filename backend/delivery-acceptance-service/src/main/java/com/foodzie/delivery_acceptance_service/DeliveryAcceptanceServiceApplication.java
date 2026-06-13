package com.foodzie.delivery_acceptance_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DeliveryAcceptanceServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeliveryAcceptanceServiceApplication.class, args);
    }
}
