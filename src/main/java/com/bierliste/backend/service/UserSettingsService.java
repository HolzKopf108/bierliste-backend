package com.bierliste.backend.service;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bierliste.backend.dto.UserSettingsDto;
import com.bierliste.backend.model.User;
import com.bierliste.backend.model.UserSettings;
import com.bierliste.backend.repository.UserSettingsRepository;

@Service
public class UserSettingsService {

    private final UserSettingsRepository repo;
    private final PasswordEncoder pwdEnc;

    public UserSettingsService(UserSettingsRepository repo, PasswordEncoder pwdEnc) {
        this.repo = repo;
        this.pwdEnc = pwdEnc;
    }

    public UserSettingsDto getUserSettings(User user) {
        UserSettings settings = repo.findByUser(user)
            .orElseThrow(() -> new UsernameNotFoundException("Benutzereinstellungen nicht gefunden"));

        return UserSettingsDto.fromEntity(settings);
    }

    @Transactional
    public UserSettingsDto updateSettingsForUser(User user, UserSettingsDto dto) {
        UserSettings settings = repo.findByUser(user).orElse(new UserSettings());
        settings.setUser(user);

        if (settings.getLastUpdated() == null || dto.getLastUpdated().isAfter(settings.getLastUpdated())) {
            settings.setTheme(dto.getTheme());
            settings.setAutoSyncEnabled(dto.isAutoSyncEnabled());
            settings.setLastUpdated(dto.getLastUpdated());
        } 

        return UserSettingsDto.fromEntity(repo.save(settings));
    }

    public boolean verifyPassword(User user, String password) {
        return pwdEnc.matches(password, user.getPasswordHash());
    }

    @Transactional
    public void deleteUser(User user) {
        repo.deleteByUser(user);
    }
}
