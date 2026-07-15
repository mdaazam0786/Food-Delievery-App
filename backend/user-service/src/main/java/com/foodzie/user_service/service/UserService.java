package com.foodzie.user_service.service;

import com.foodzie.user_service.dto.AvatarResponse;
import com.foodzie.user_service.dto.UpdateUserRequest;
import com.foodzie.user_service.dto.UserResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    /**
     * Returns the profile of the currently authenticated user.
     */
    UserResponse getMe(String userId, String email);

    /**
     * Updates editable profile fields: fullName, phoneNumber, bio.
     * Email is immutable and cannot be changed here.
     */
    UserResponse updateMe(String userId, UpdateUserRequest request);

    /**
     * Uploads a new avatar image to cloud storage and persists the URL.
     */
    AvatarResponse uploadAvatar(String userId, MultipartFile file);
}
