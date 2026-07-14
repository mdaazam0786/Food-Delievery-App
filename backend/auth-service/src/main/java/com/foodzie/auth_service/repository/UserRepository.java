package com.foodzie.auth_service.repository;

import com.foodzie.auth_service.data.User;
import com.foodzie.auth_service.data.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByPasswordResetToken(String token);

    /** Soft-delete aware lookup */
    @Query("{ 'email': ?0, 'deletedAt': null }")
    Optional<User> findActiveByEmail(String email);

    @Query("{ 'deletedAt': null }")
    Page<User> findAllActive(Pageable pageable);

    @Query("{ 'role': ?0, 'deletedAt': null }")
    Page<User> findAllByRole(UserRole role, Pageable pageable);
}
