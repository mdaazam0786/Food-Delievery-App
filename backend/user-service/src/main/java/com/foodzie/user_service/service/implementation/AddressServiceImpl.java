package com.foodzie.user_service.service.implementation;

import com.foodzie.user_service.data.Address;
import com.foodzie.user_service.data.UserProfile;
import com.foodzie.user_service.data.UserProfileRepository;
import com.foodzie.user_service.dto.AddressRequest;
import com.foodzie.user_service.dto.AddressResponse;
import com.foodzie.user_service.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final UserProfileRepository profileRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(String userId) {
        UserProfile profile = findProfile(userId);
        return profile.getAddresses().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse addAddress(String userId, AddressRequest request) {
        UserProfile profile = findProfile(userId);

        Address address = Address.builder()
                .id(UUID.randomUUID().toString())
                .formattedAddress(request.getFormattedAddress())
                .street(request.getStreet())
                .postCode(request.getPostCode())
                .apartment(request.getApartment())
                .label(request.getLabel().toUpperCase())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        profile.getAddresses().add(address);
        profileRepository.save(profile);

        return toResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(String userId, Long addressId, AddressRequest request) {
        UserProfile profile = findProfile(userId);
        
        Address address = profile.getAddresses().stream()
                .filter(a -> a.getId().equals(String.valueOf(addressId)))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Address not found: " + addressId));

        address.setFormattedAddress(request.getFormattedAddress());
        address.setStreet(request.getStreet());
        address.setPostCode(request.getPostCode());
        address.setApartment(request.getApartment());
        address.setLabel(request.getLabel().toUpperCase());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setUpdatedAt(LocalDateTime.now());

        profileRepository.save(profile);

        return toResponse(address);
    }

    @Override
    @Transactional
    public void deleteAddress(String userId, Long addressId) {
        UserProfile profile = findProfile(userId);
        
        boolean removed = profile.getAddresses().removeIf(a -> a.getId().equals(String.valueOf(addressId)));
        
        if (!removed) {
            throw new NoSuchElementException("Address not found: " + addressId);
        }
        
        profileRepository.save(profile);
    }

    // -------------------------------------------------------------------------

    private UserProfile findProfile(String userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("Profile not found for user: " + userId));
    }

    private AddressResponse toResponse(Address a) {
        return AddressResponse.builder()
                .id(a.getId())
                .formattedAddress(a.getFormattedAddress())
                .street(a.getStreet())
                .postCode(a.getPostCode())
                .apartment(a.getApartment())
                .label(a.getLabel())
                .latitude(a.getLatitude())
                .longitude(a.getLongitude())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
