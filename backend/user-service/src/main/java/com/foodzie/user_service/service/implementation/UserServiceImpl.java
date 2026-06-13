package com.foodzie.user_service.service.implementation;

import com.foodzie.user_service.data.UserProfile;
import com.foodzie.user_service.data.UserProfileRepository;
import com.foodzie.user_service.dto.AvatarResponse;
import com.foodzie.user_service.dto.UpdateUserRequest;
import com.foodzie.user_service.dto.UserResponse;
import com.foodzie.user_service.service.CloudStorageService;
import com.foodzie.user_service.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String AVATAR_FOLDER = "foodzie/avatars";

    private final UserProfileRepository profileRepository;
    private final CloudStorageService cloudStorageService;

    @Override
    @Transactional
    public UserResponse getMe(Long userId, String email) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> profileRepository.save(
                        UserProfile.builder()
                                .userId(userId)
                                .email(email)
                                .build()
                ));
        return toResponse(profile);
    }

    @Override
    @Transactional
    public UserResponse updateMe(Long userId, UpdateUserRequest request) {
        UserProfile profile = findProfile(userId);

        if (request.getFullName() != null)    profile.setFullName(request.getFullName());
        if (request.getPhoneNumber() != null) profile.setPhoneNumber(request.getPhoneNumber());
        if (request.getBio() != null)         profile.setBio(request.getBio());

        return toResponse(profileRepository.save(profile));
    }

    @Override
    @Transactional
    public AvatarResponse uploadAvatar(Long userId, MultipartFile file) {
        UserProfile profile = findProfile(userId);

        // Use userId as the stable public ID so re-uploads overwrite the old asset
        String publicId = "user_" + userId + "_avatar";
        String url = cloudStorageService.upload(file, AVATAR_FOLDER, publicId);

        profile.setAvatarUrl(url);
        profileRepository.save(profile);

        log.info("Avatar updated for userId={}: {}", userId, url);
        return AvatarResponse.builder().avatarUrl(url).build();
    }

    // -------------------------------------------------------------------------

    private UserProfile findProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found for user: " + userId));
    }

    private UserResponse toResponse(UserProfile p) {
        return UserResponse.builder()
                .id(p.getUserId())
                .email(p.getEmail())
                .fullName(p.getFullName())
                .phoneNumber(p.getPhoneNumber())
                .bio(p.getBio())
                .avatarUrl(p.getAvatarUrl())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
