package com.foodzie.auth_service.repository;

import com.foodzie.auth_service.data.OAuthConnection;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OAuthConnectionRepository extends MongoRepository<OAuthConnection, String> {

    Optional<OAuthConnection> findByProviderAndProviderUserId(String provider, String providerUserId);

    List<OAuthConnection> findAllByUserId(String userId);

    boolean existsByUserIdAndProvider(String userId, String provider);
}
