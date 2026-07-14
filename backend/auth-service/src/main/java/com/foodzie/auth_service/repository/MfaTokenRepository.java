package com.foodzie.auth_service.repository;

import com.foodzie.auth_service.data.MfaToken;
import com.foodzie.auth_service.data.MfaTokenType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MfaTokenRepository extends MongoRepository<MfaToken, String> {

    @Query("{ 'userId': ?0, 'type': ?1, 'used': false }")
    Optional<MfaToken> findLatestValidToken(String userId, MfaTokenType type);

    void deleteByUserIdAndType(String userId, MfaTokenType type);
}
