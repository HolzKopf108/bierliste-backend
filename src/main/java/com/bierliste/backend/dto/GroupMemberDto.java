package com.bierliste.backend.dto;

import com.bierliste.backend.model.GroupRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

public class GroupMemberDto {

    private Long userId;
    private String username;
    private Instant joinedAt;
    private GroupRole role;
    @Schema(description = "Aktueller Strichsaldo. Positiv bedeutet Schulden, negativ bedeutet Guthaben.")
    private int strichCount;
    private BigDecimal outstandingAmount;
    private boolean canReceiveNotification;
    private boolean hasPendingNotification;
    private Instant lastNotificationSentAt;
    private Instant lastNotificationConfirmedAt;

    public GroupMemberDto() {
    }

    public GroupMemberDto(Long userId, String username, Instant joinedAt, GroupRole role, int strichCount) {
        this(userId, username, joinedAt, role, strichCount, null, false, false, null, null);
    }

    public GroupMemberDto(
        Long userId,
        String username,
        Instant joinedAt,
        GroupRole role,
        int strichCount,
        BigDecimal outstandingAmount,
        boolean canReceiveNotification,
        boolean hasPendingNotification,
        Instant lastNotificationSentAt,
        Instant lastNotificationConfirmedAt
    ) {
        this.userId = userId;
        this.username = username;
        this.joinedAt = joinedAt;
        this.role = role;
        this.strichCount = strichCount;
        this.outstandingAmount = outstandingAmount;
        this.canReceiveNotification = canReceiveNotification;
        this.hasPendingNotification = hasPendingNotification;
        this.lastNotificationSentAt = lastNotificationSentAt;
        this.lastNotificationConfirmedAt = lastNotificationConfirmedAt;
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

    public int getStrichCount() {
        return strichCount;
    }

    public void setStrichCount(int strichCount) {
        this.strichCount = strichCount;
    }

    public BigDecimal getOutstandingAmount() {
        return outstandingAmount;
    }

    public void setOutstandingAmount(BigDecimal outstandingAmount) {
        this.outstandingAmount = outstandingAmount;
    }

    public boolean isCanReceiveNotification() {
        return canReceiveNotification;
    }

    public void setCanReceiveNotification(boolean canReceiveNotification) {
        this.canReceiveNotification = canReceiveNotification;
    }

    public boolean isHasPendingNotification() {
        return hasPendingNotification;
    }

    public void setHasPendingNotification(boolean hasPendingNotification) {
        this.hasPendingNotification = hasPendingNotification;
    }

    public Instant getLastNotificationSentAt() {
        return lastNotificationSentAt;
    }

    public void setLastNotificationSentAt(Instant lastNotificationSentAt) {
        this.lastNotificationSentAt = lastNotificationSentAt;
    }

    public Instant getLastNotificationConfirmedAt() {
        return lastNotificationConfirmedAt;
    }

    public void setLastNotificationConfirmedAt(Instant lastNotificationConfirmedAt) {
        this.lastNotificationConfirmedAt = lastNotificationConfirmedAt;
    }
}
