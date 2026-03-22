package com.bierliste.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Invite-Link für eine Gruppe. Der Link gilt 7 Tage.")
public class GroupInviteResponseDto {

    private Long inviteId;
    private String token;
    @Schema(
        description = "Custom-App-Link zum Beitreten in der mobilen App.",
        example = "bierliste://join?token=abc123"
    )
    private String joinUrl;
    private Instant expiresAt;

    public GroupInviteResponseDto() {
    }

    public GroupInviteResponseDto(Long inviteId, String token, String joinUrl, Instant expiresAt) {
        this.inviteId = inviteId;
        this.token = token;
        this.joinUrl = joinUrl;
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

    public String getJoinUrl() {
        return joinUrl;
    }

    public void setJoinUrl(String joinUrl) {
        this.joinUrl = joinUrl;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
