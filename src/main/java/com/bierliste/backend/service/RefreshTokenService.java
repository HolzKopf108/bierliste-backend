package com.bierliste.backend.service;

import com.bierliste.backend.model.RefreshToken;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RefreshTokenService {
    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    @Value("${app.jwt.refresh-exp-ms}")
    private long refreshExpMs;

    private final RefreshTokenRepository repository;

    public RefreshTokenService(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public RefreshToken create(User user) {
        repository.deleteByUser(user);
        repository.flush();

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setExpiryDate(Instant.now().plusMillis(refreshExpMs));
        token.setToken(UUID.randomUUID().toString());
        return repository.save(token);
    }

    public RefreshToken verify(String tokenStr) {
        if (tokenStr == null || tokenStr.isBlank()) {
            log.warn("Refresh token verification failed: missing token");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh-Token ist erforderlich");
        }

        RefreshToken token = repository.findByToken(tokenStr)
            .orElseThrow(() -> {
                log.warn("Refresh token verification failed: token not found");
                return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ungültiges Refresh-Token");
            });

        if (token.getExpiryDate().isBefore(Instant.now())) {
            repository.delete(token);
            log.warn("Refresh token verification failed: token expired for userId={}", token.getUser().getId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh-Token abgelaufen");
        }

        return token;
    }

    @Transactional
    public void delete(String tokenStr) {
        repository.findByToken(tokenStr).ifPresent(repository::delete);
    }

    @Transactional
    public void deleteUser(User user) {
        repository.deleteByUser(user);
    }
}
