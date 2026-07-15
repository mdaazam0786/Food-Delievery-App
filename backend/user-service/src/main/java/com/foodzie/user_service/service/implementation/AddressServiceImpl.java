package com.foodzie.user_service.service.implementation;

import com.foodzie.user_service.data.Address;
import com.foodzie.user_service.data.AddressRepository;
import com.foodzie.user_service.data.UserProfile;
import com.foodzie.user_service.data.UserProfileRepository;
import com.foodzie.user_service.dto.AddressRequest;
import com.foodzie.user_service.dto.AddressResponse;
import com.foodzie.user_service.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserProfileRepository profileRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(Long userId) {
        UserProfile profile = findProfile(userId);
        return addressRepository.findAllByUserProfile_Id(profile.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest request) {
        UserProfile profile = findProfile(userId);

        Address address = Address.builder()
                .userProfile(profile)
                .formattedAddress(request.getFormattedAddress())
                .street(request.getStreet())
                .postCode(request.getPostCode())
                .apartment(request.getApartment())
                .label(request.getLabel().toUpperCase())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        return toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        UserProfile profile = findProfile(userId);
        Address address = addressRepository.findByIdAndUserProfile_Id(addressId, profile.getId())
                .orElseThrow(() -> new NoSuchElementException("Address not found: " + addressId));

        address.setFormattedAddress(request.getFormattedAddress());
        address.setStreet(request.getStreet());
        address.setPostCode(request.getPostCode());
        address.setApartment(request.getApartment());
        address.setLabel(request.getLabel().toUpperCase());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());

        return toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        UserProfile profile = findProfile(userId);
        Address address = addressRepository.findByIdAndUserProfile_Id(addressId, profile.getId())
                .orElseThrow(() -> new NoSuchElementException("Address not found: " + addressId));
        addressRepository.delete(address);
    }

    // -------------------------------------------------------------------------

    private UserProfile findProfile(Long userId) {
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
