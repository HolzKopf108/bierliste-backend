package com.bierliste.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(
    name = "group_member_notifications",
    indexes = {
        @Index(name = "idx_group_member_notifications_group", columnList = "group_id"),
        @Index(name = "idx_group_member_notifications_target_user", columnList = "target_user_id"),
        @Index(name = "idx_group_member_notifications_actor_user", columnList = "actor_user_id"),
        @Index(name = "idx_group_member_notifications_created_at", columnList = "created_at"),
        @Index(name = "idx_group_member_notifications_confirmed_at", columnList = "confirmed_at")
    }
)
@Check(
    name = "ck_group_member_notifications_state",
    constraints = """
        group_id > 0
        and actor_user_id > 0
        and target_user_id > 0
        and (confirmed_at is null or confirmed_at >= created_at)
        """
)
public class GroupMemberNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Positive
    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Positive
    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Column(name = "actor_username_snapshot", nullable = false, length = 120)
    private String actorUsernameSnapshot;

    @Positive
    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @Column(nullable = false, length = 500)
    private String message;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    public Long getId() {
        return id;
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

    public String getActorUsernameSnapshot() {
        return actorUsernameSnapshot;
    }

    public void setActorUsernameSnapshot(String actorUsernameSnapshot) {
        this.actorUsernameSnapshot = actorUsernameSnapshot;
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

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }
}
