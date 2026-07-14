package com.foodzie.user_service.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    @Indexed(unique = true)
    private String email;

    private String fullName;

    private String phoneNumber;

    private String bio;

    private String avatarUrl;

    @Builder.Default
    private List<Address> addresses = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
