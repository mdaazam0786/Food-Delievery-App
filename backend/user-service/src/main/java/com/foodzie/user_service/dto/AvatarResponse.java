package com.foodzie.user_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AvatarResponse {

    private String avatarUrl;
}
