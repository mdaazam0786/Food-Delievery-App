package com.foodzie.user_service.service;

import com.foodzie.user_service.dto.AddressRequest;
import com.foodzie.user_service.dto.AddressResponse;

import java.util.List;

public interface AddressService {

    List<AddressResponse> getAddresses(String userId);

    AddressResponse addAddress(String userId, AddressRequest request);

    AddressResponse updateAddress(String userId, Long addressId, AddressRequest request);

    void deleteAddress(String userId, Long addressId);
}
