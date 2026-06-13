package com.foodzie.user_service.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findAllByUserProfile_Id(Long userProfileId);

    Optional<Address> findByIdAndUserProfile_Id(Long id, Long userProfileId);
}
