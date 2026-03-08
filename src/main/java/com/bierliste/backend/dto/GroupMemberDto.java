package com.bierliste.backend.dto;

import com.bierliste.backend.model.GroupRole;

import java.time.Instant;

public class GroupMemberDto {

    private Long userId;
    private String username;
    private Instant joinedAt;
    private GroupRole role;

    public GroupMemberDto() {
    }

    public GroupMemberDto(Long userId, String username, Instant joinedAt, GroupRole role) {
        this.userId = userId;
        this.username = username;
        this.joinedAt = joinedAt;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public GroupRole getRole() {
        return role;
    }

    public void setRole(GroupRole role) {
        this.role = role;
    }
}
