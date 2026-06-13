package com.foodzie.auth_service.repository;

import com.foodzie.auth_service.data.MfaToken;
import com.foodzie.auth_service.data.MfaTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MfaTokenRepository extends JpaRepository<MfaToken, Long> {

    @Query("SELECT t FROM MfaToken t WHERE t.user.id = :userId AND t.type = :type AND t.used = false ORDER BY t.createdAt DESC")
    Optional<MfaToken> findLatestValidToken(@Param("userId") Long userId, @Param("type") MfaTokenType type);

    @Modifying
    @Query("UPDATE MfaToken t SET t.used = true WHERE t.user.id = :userId AND t.type = :type")
    void invalidateAllByUserAndType(@Param("userId") Long userId, @Param("type") MfaTokenType type);
}
