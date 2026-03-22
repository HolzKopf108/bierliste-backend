package com.bierliste.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Invite-Daten fuer eine Gruppe. Das Frontend baut daraus selbst Deep Link und Share-Link.")
public class GroupInviteResponseDto {

    private Long inviteId;
    @Schema(
        description = "Eindeutiger Invite-Token fuer QR-Code, Share-Link und spaeteren Join per API.",
        example = "abc123"
    )
    private String token;
    private Instant expiresAt;

    public GroupInviteResponseDto() {
    }

    public GroupInviteResponseDto(Long inviteId, String token, Instant expiresAt) {
        this.inviteId = inviteId;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public Long getInviteId() {
        return inviteId;
    }

    public void setInviteId(Long inviteId) {
        this.inviteId = inviteId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
