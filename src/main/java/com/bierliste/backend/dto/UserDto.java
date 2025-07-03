package com.bierliste.backend.dto;

import java.time.Instant;

import com.bierliste.backend.model.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UserDto {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String username;

    @NotNull
    private Instant lastUpdated;

    private boolean googleUser;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
    public boolean isGoogleUser() { return googleUser; }
    public void setGoogleUser(boolean googleUser) { this.googleUser = googleUser; }

    public static UserDto fromEntity(User user) {
        UserDto dto = new UserDto();
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setLastUpdated(user.getLastUpdated());
        dto.setGoogleUser(user.istGoogleUser());
        return dto;
    }
}