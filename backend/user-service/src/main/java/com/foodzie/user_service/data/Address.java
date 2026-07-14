package com.foodzie.user_service.data;

import org.springframework.data.annotation.Id;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    private String id;

    private String formattedAddress;

    private String street;

    private String postCode;

    private String apartment;

    private String label;

    private Double latitude;

    private Double longitude;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
