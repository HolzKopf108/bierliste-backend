package com.bierliste.backend.repository;

import com.bierliste.backend.model.User;
import com.bierliste.backend.model.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
    Optional<UserSettings> findByUser(User user);
    void deleteByUser(User user);
}
