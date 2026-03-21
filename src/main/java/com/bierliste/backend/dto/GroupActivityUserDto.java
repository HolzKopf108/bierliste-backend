package com.bierliste.backend.dto;

public class GroupActivityUserDto {

    private Long userId;
    private String usernameSnapshot;

    public GroupActivityUserDto() {
    }

    public GroupActivityUserDto(Long userId, String usernameSnapshot) {
        this.userId = userId;
        this.usernameSnapshot = usernameSnapshot;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsernameSnapshot() {
        return usernameSnapshot;
    }

    public void setUsernameSnapshot(String usernameSnapshot) {
        this.usernameSnapshot = usernameSnapshot;
    }
}
