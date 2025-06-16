package com.bierliste.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.bierliste.backend.model.RefreshToken;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {
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
        RefreshToken token = repository.findByToken(tokenStr)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ungültiges Refresh-Token"));

        if (token.getExpiryDate().isBefore(Instant.now())) {
            repository.delete(token);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh-Token abgelaufen");
        }

        return token;
    }

    public void delete(String tokenStr) {
        repository.findByToken(tokenStr).ifPresent(repository::delete);
    }
}
