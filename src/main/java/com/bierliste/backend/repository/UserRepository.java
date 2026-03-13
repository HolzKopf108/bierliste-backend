package com.bierliste.backend.repository;

import com.bierliste.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email); 
    List<User> findAllByEmailVerifiedFalseAndCreatedAtBeforeAndDeletedFalse(Instant createdAt);
}
