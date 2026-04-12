package com.bierliste.backend.dto;

import java.time.Instant;

public class GroupMemberNotificationDto {

    private Long id;
    private Long groupId;
    private Long actorUserId;
    private String actorUsername;
    private Long targetUserId;
    private String message;
    private Instant createdAt;
    private Instant confirmedAt;

    public GroupMemberNotificationDto() {
    }

    public GroupMemberNotificationDto(
        Long id,
        Long groupId,
        Long actorUserId,
        String actorUsername,
        Long targetUserId,
        String message,
        Instant createdAt,
        Instant confirmedAt
    ) {
        this.id = id;
        this.groupId = groupId;
        this.actorUserId = actorUserId;
        this.actorUsername = actorUsername;
        this.targetUserId = targetUserId;
        this.message = message;
        this.createdAt = createdAt;
        this.confirmedAt = confirmedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }
}
