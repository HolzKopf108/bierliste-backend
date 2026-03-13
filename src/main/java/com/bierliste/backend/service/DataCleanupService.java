package com.bierliste.backend.service;

import com.bierliste.backend.repository.RefreshTokenRepository;
import com.bierliste.backend.repository.VerificationTokenRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataCleanupService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final UserService userService;

    @Value("${app.cleanup.unverified-user-max-age:P7D}")
    private Duration unverifiedUserMaxAge;

    public DataCleanupService(
        RefreshTokenRepository refreshTokenRepository,
        VerificationTokenRepository verificationTokenRepository,
        UserService userService
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.userService = userService;
    }

    @Scheduled(cron = "${app.cleanup.cron:0 0 3 * * *}")
    @Transactional
    public void runScheduledCleanup() {
        cleanupExpiredRefreshTokens();
        cleanupExpiredVerificationTokens();
        cleanupExpiredUnverifiedUsers();
    }

    @Transactional
    public long cleanupExpiredRefreshTokens() {
        return refreshTokenRepository.deleteByExpiryDateBefore(Instant.now());
    }

    @Transactional
    public long cleanupExpiredVerificationTokens() {
        return verificationTokenRepository.deleteByExpiryDateBefore(Instant.now());
    }

    @Transactional
    public int cleanupExpiredUnverifiedUsers() {
        Instant cutoff = Instant.now().minus(unverifiedUserMaxAge);
        return userService.deleteExpiredUnverifiedUsers(cutoff);
    }
}
