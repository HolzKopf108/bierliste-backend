package com.bierliste.backend.service;

import com.bierliste.backend.dto.AndroidPushTokenDto;
import com.bierliste.backend.model.AndroidPushToken;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.AndroidPushTokenRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AndroidPushTokenService {

    private final AndroidPushTokenRepository androidPushTokenRepository;
    private final GroupAuthorizationService groupAuthorizationService;

    public AndroidPushTokenService(
        AndroidPushTokenRepository androidPushTokenRepository,
        GroupAuthorizationService groupAuthorizationService
    ) {
        this.androidPushTokenRepository = androidPushTokenRepository;
        this.groupAuthorizationService = groupAuthorizationService;
    }

    @Transactional
    public void registerToken(User user, AndroidPushTokenDto dto) {
        groupAuthorizationService.requireAuthenticatedUserId(user);

        String normalizedToken = dto.getToken().trim();
        Instant now = Instant.now();

        AndroidPushToken androidPushToken = androidPushTokenRepository.findByToken(normalizedToken)
            .orElseGet(AndroidPushToken::new);

        androidPushToken.setUser(user);
        androidPushToken.setToken(normalizedToken);
        androidPushToken.setLastSeenAt(now);

        androidPushTokenRepository.save(androidPushToken);
    }

    @Transactional
    public void unregisterToken(User user, AndroidPushTokenDto dto) {
        Long userId = groupAuthorizationService.requireAuthenticatedUserId(user);
        androidPushTokenRepository.deleteByUser_IdAndToken(userId, dto.getToken().trim());
    }

    @Transactional
    public void deleteTokensForUser(User user) {
        androidPushTokenRepository.deleteByUser(user);
    }

    public Set<Long> findUserIdsWithTokens(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(androidPushTokenRepository.findDistinctUserIdsWithTokens(userIds));
    }
}
