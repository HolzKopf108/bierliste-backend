package com.bierliste.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bierliste.backend.model.RefreshToken;
import com.bierliste.backend.model.User;
import com.bierliste.backend.model.VerificationToken;
import com.bierliste.backend.repository.RefreshTokenRepository;
import com.bierliste.backend.repository.UserRepository;
import com.bierliste.backend.repository.VerificationTokenRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DataCleanupServiceIntegrationTest {

    @Autowired
    private DataCleanupService dataCleanupService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Test
    void cleanupExpiredRefreshTokensDeletesOnlyExpiredTokens() {
        User expiredUser = createUser("expired-refresh@example.com", false, Instant.now());
        User validUser = createUser("valid-refresh@example.com", false, Instant.now());

        RefreshToken expiredToken = refreshTokenService.create(expiredUser);
        expiredToken.setExpiryDate(Instant.now().minusSeconds(60));
        refreshTokenRepository.save(expiredToken);

        RefreshToken validToken = refreshTokenService.create(validUser);
        validToken.setExpiryDate(Instant.now().plusSeconds(3600));
        refreshTokenRepository.save(validToken);

        long deletedCount = dataCleanupService.cleanupExpiredRefreshTokens();

        assertThat(deletedCount).isEqualTo(1);
        assertThat(refreshTokenRepository.findByToken(expiredToken.getToken())).isEmpty();
        assertThat(refreshTokenRepository.findByToken(validToken.getToken())).isPresent();
    }

    @Test
    void cleanupExpiredVerificationTokensDeletesOnlyExpiredTokens() {
        User expiredUser = createUser("expired-verification@example.com", false, Instant.now());
        User validUser = createUser("valid-verification@example.com", false, Instant.now());

        VerificationToken expiredToken = createVerificationToken(expiredUser, Instant.now().minusSeconds(60), "111111");
        VerificationToken validToken = createVerificationToken(validUser, Instant.now().plusSeconds(3600), "222222");

        long deletedCount = dataCleanupService.cleanupExpiredVerificationTokens();

        assertThat(deletedCount).isEqualTo(1);
        assertThat(verificationTokenRepository.findById(expiredToken.getId())).isEmpty();
        assertThat(verificationTokenRepository.findById(validToken.getId())).isPresent();
    }

    @Test
    void cleanupExpiredUnverifiedUsersDeletesOnlyStaleUnverifiedUsers() {
        User staleUnverifiedUser = createUser("stale-unverified@example.com", false, Instant.now().minusSeconds(8 * 24 * 3600L));
        User recentUnverifiedUser = createUser("recent-unverified@example.com", false, Instant.now().minusSeconds(24 * 3600L));
        User verifiedUser = createUser("verified@example.com", true, Instant.now().minusSeconds(8 * 24 * 3600L));

        createVerificationToken(staleUnverifiedUser, Instant.now().plusSeconds(3600), "333333");
        createVerificationToken(recentUnverifiedUser, Instant.now().plusSeconds(3600), "444444");

        int deletedCount = dataCleanupService.cleanupExpiredUnverifiedUsers();

        assertThat(deletedCount).isEqualTo(1);
        assertThat(userRepository.findById(staleUnverifiedUser.getId())).isEmpty();
        assertThat(userRepository.findById(recentUnverifiedUser.getId())).isPresent();
        assertThat(userRepository.findById(verifiedUser.getId())).isPresent();
        assertThat(verificationTokenRepository.findByUserEmail("stale-unverified@example.com")).isEmpty();
        assertThat(verificationTokenRepository.findByUserEmail("recent-unverified@example.com")).isPresent();
    }

    private User createUser(String email, boolean emailVerified, Instant createdAt) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(email);
        user.setPasswordHash("hashed");
        user.setEmailVerified(emailVerified);
        user.setCreatedAt(createdAt);
        user.setLastUpdated(createdAt);
        return userRepository.save(user);
    }

    private VerificationToken createVerificationToken(User user, Instant expiryDate, String code) {
        VerificationToken token = new VerificationToken();
        token.setUser(user);
        token.setCode(code);
        token.setExpiryDate(expiryDate);
        return verificationTokenRepository.save(token);
    }
}
