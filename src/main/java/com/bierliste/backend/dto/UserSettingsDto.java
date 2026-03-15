package com.bierliste.backend.dto;

import java.time.Instant;

import com.bierliste.backend.model.UserSettings;

public class UserSettingsDto {
    private String theme;
    private Instant lastUpdated;

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }

    public static UserSettingsDto fromEntity(UserSettings settings) {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setTheme(settings.getTheme());
        dto.setLastUpdated(settings.getLastUpdated());
        return dto;
    }
}
