package com.foodzie.auth_service.repository;

import com.foodzie.auth_service.data.OAuthConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OAuthConnectionRepository extends JpaRepository<OAuthConnection, Long> {

    Optional<OAuthConnection> findByProviderAndProviderUserId(String provider, String providerUserId);

    List<OAuthConnection> findAllByUserId(Long userId);

    boolean existsByUserIdAndProvider(Long userId, String provider);
}
