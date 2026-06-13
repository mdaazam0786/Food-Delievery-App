package com.foodzie.delivery_service.repository;

import com.foodzie.delivery_service.data.DeliveryPartner;
import com.foodzie.delivery_service.data.DriverStatus;
import com.foodzie.delivery_service.data.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, String> {

    Optional<DeliveryPartner> findByPhoneNumber(String phoneNumber);

    Optional<DeliveryPartner> findByEmail(String email);

    List<DeliveryPartner> findAllByCurrentStatusAndKycStatus(
            DriverStatus status, KycStatus kycStatus);
}
