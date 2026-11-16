package com.bierliste.backend.service;

import com.bierliste.backend.repository.RefreshTokenRepository;
import com.bierliste.backend.repository.ProcessedClientEventRepository;
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
    private final ProcessedClientEventRepository processedClientEventRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final UserService userService;

    @Value("${app.cleanup.unverified-user-max-age:P7D}")
    private Duration unverifiedUserMaxAge;

    @Value("${app.sync.processed-event-retention:P90D}")
    private Duration processedEventRetention;

    public DataCleanupService(
        RefreshTokenRepository refreshTokenRepository,
        ProcessedClientEventRepository processedClientEventRepository,
        VerificationTokenRepository verificationTokenRepository,
        UserService userService
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.processedClientEventRepository = processedClientEventRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.userService = userService;
    }

    @Scheduled(cron = "${app.cleanup.cron:0 0 3 * * *}")
    @Transactional
    public void runScheduledCleanup() {
        cleanupExpiredRefreshTokens();
        cleanupExpiredVerificationTokens();
        cleanupExpiredUnverifiedUsers();
        cleanupOldProcessedClientEvents();
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

    @Transactional
    public long cleanupOldProcessedClientEvents() {
        if (processedEventRetention.isZero() || processedEventRetention.isNegative()) {
            throw new IllegalStateException("app.sync.processed-event-retention muss groesser als 0 sein");
        }
        Instant cutoff = Instant.now().minus(processedEventRetention);
        return processedClientEventRepository.deleteByProcessedAtBefore(cutoff);
    }
}
