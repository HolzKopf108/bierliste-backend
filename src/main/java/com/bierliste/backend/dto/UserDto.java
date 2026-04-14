package com.bierliste.backend.dto;

import java.time.Instant;

import com.bierliste.backend.model.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(requiredProperties = {"userId"})
public class UserDto {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(
        accessMode = Schema.AccessMode.READ_ONLY,
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "Stabile numerische ID des Nutzers."
    )
    private Long userId;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String username;

    @NotNull
    private Instant lastUpdated;

    private boolean googleUser;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
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
        dto.setUserId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setLastUpdated(user.getLastUpdated());
        dto.setGoogleUser(user.isGoogleUser());
        return dto;
    }
}
