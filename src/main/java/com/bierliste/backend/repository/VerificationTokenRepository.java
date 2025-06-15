package com.bierliste.backend.repository;

import com.bierliste.backend.model.VerificationToken;
import com.bierliste.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByUserEmail(String email);
    void deleteByUser(User user);
}