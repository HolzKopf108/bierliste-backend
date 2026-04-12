package com.bierliste.backend.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bierliste.backend.dto.UserDto;
import com.bierliste.backend.dto.UserPasswordDto;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.VerificationTokenRepository;
import com.bierliste.backend.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private final UserRepository userRepo;
    private final PasswordEncoder pwdEnc;
    private final RefreshTokenService refreshService;
    private final UserSettingsService userSettingsService;
    private final VerificationTokenRepository verificationTokenRepository;
    private final GroupService groupService;
    private final AndroidPushTokenService androidPushTokenService;

    public UserService(
        UserRepository userRepo, 
        PasswordEncoder pwdEnc,
        RefreshTokenService refreshService,
        UserSettingsService userSettingsService,
        VerificationTokenRepository verificationTokenRepository,
        GroupService groupService,
        AndroidPushTokenService androidPushTokenService
    ) {
        this.userRepo = userRepo;
        this.pwdEnc = pwdEnc;
        this.refreshService = refreshService;
        this.userSettingsService = userSettingsService;
        this.verificationTokenRepository = verificationTokenRepository;
        this.groupService = groupService;
        this.androidPushTokenService = androidPushTokenService;
    }

    public UserDto getUser(User user) {
        User backendUser = userRepo.findByEmail(user.getEmail())
            .orElseThrow(() -> new UsernameNotFoundException("Benutzer nicht gefunden"));

        return UserDto.fromEntity(backendUser);
    }

    @Transactional
    public UserDto updateUsername(UserDto dto) {
        User user = userRepo.findByEmail(dto.getEmail())
            .orElseThrow(() -> new UsernameNotFoundException("Benutzer nicht gefunden"));

        if(dto.getLastUpdated().isAfter(user.getLastUpdated())) {
            user.setUsername(dto.getUsername());
            userRepo.save(user);
        }

        return UserDto.fromEntity(user);
    }

    @Transactional
    public void updatePassword(UserPasswordDto dto) {
        User user = userRepo.findByEmail(dto.getEmail())
            .orElseThrow(() -> new UsernameNotFoundException("Benutzer nicht gefunden"));

        if(dto.getLastUpdated().isAfter(user.getLastUpdated())) {
            user.setPasswordHash(pwdEnc.encode(dto.getPassword()));
            userRepo.save(user);
        }
        else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwort konnte nicht geändert werden. Versuche es erneut!");
        }
    }
    
    @Transactional
    public void logout(String refreshToken) {
        refreshService.delete(refreshToken);
    }

    @Transactional
    public void deleteAccount(User user) {
        User persistedUser = userRepo.findById(user.getId())
            .orElseThrow(() -> new UsernameNotFoundException("Benutzer nicht gefunden"));

        groupService.removeUserFromAllGroups(persistedUser);
        userSettingsService.deleteUser(persistedUser);
        refreshService.deleteUser(persistedUser);
        verificationTokenRepository.deleteByUser(persistedUser);
        androidPushTokenService.deleteTokensForUser(persistedUser);

        anonymizeUser(persistedUser);
        userRepo.save(persistedUser);
    }

    @Transactional
    public int deleteExpiredUnverifiedUsers(Instant cutoff) {
        List<User> users = userRepo.findAllByEmailVerifiedFalseAndCreatedAtBeforeAndDeletedFalse(cutoff);
        for (User user : users) {
            deleteUnverifiedUser(user);
        }
        return users.size();
    }

    private void deleteUnverifiedUser(User user) {
        groupService.removeUserFromAllGroups(user);
        userSettingsService.deleteUser(user);
        refreshService.deleteUser(user);
        verificationTokenRepository.deleteByUser(user);
        androidPushTokenService.deleteTokensForUser(user);
        userRepo.delete(user);
    }

    private void anonymizeUser(User user) {
        user.setEmail("deleted-user-" + user.getId() + "@deleted.local");
        user.setUsername("Gelöschter Benutzer");
        user.setPasswordHash(pwdEnc.encode(UUID.randomUUID().toString()));
        user.setEmailVerified(false);
        user.setGoogleUser(false);
        user.setDeleted(true);
        user.setLastUpdated(Instant.now());
    }
}
