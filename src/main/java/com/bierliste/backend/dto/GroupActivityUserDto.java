package com.bierliste.backend.dto;

public class GroupActivityUserDto {

    private Long userId;
    private String displayNameSnapshot;

    public GroupActivityUserDto() {
    }

    public GroupActivityUserDto(Long userId, String displayNameSnapshot) {
        this.userId = userId;
        this.displayNameSnapshot = displayNameSnapshot;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDisplayNameSnapshot() {
        return displayNameSnapshot;
    }

    public void setDisplayNameSnapshot(String displayNameSnapshot) {
        this.displayNameSnapshot = displayNameSnapshot;
    }
}
