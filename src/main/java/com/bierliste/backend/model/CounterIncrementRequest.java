package com.bierliste.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(
    name = "counter_increment_requests",
    indexes = {
        @Index(name = "idx_counter_increment_requests_group", columnList = "group_id"),
        @Index(name = "idx_counter_increment_requests_actor_user", columnList = "actor_user_id"),
        @Index(name = "idx_counter_increment_requests_target_user", columnList = "target_user_id"),
        @Index(name = "idx_counter_increment_requests_undo_expires", columnList = "undo_expires_at")
    }
)
@Check(constraints = """
    group_id > 0
    and actor_user_id > 0
    and target_user_id > 0
    and amount >= 1
    and increment_activity_id > 0
    and undo_expires_at >= created_at
    and (undo_activity_id is null or undo_activity_id > 0)
    and (count_after_undo is null or count_after_undo >= 0)
    and (
        (undone_at is null and undo_activity_id is null and count_after_undo is null)
        or
        (undone_at is not null and undo_activity_id is not null and count_after_undo is not null)
    )
    """)
public class CounterIncrementRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Positive
    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @NotNull
    @Positive
    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @NotNull
    @Positive
    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupActivityBookingMode mode;

    @NotNull
    @Positive
    @Column(name = "increment_activity_id", nullable = false)
    private Long incrementActivityId;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @NotNull
    @Column(name = "undo_expires_at", nullable = false)
    private Instant undoExpiresAt;

    @Column(name = "undone_at")
    private Instant undoneAt;

    @Positive
    @Column(name = "undo_activity_id")
    private Long undoActivityId;

    @PositiveOrZero
    @Column(name = "count_after_undo")
    private Integer countAfterUndo;

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

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public GroupActivityBookingMode getMode() {
        return mode;
    }

    public void setMode(GroupActivityBookingMode mode) {
        this.mode = mode;
    }

    public Long getIncrementActivityId() {
        return incrementActivityId;
    }

    public void setIncrementActivityId(Long incrementActivityId) {
        this.incrementActivityId = incrementActivityId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUndoExpiresAt() {
        return undoExpiresAt;
    }

    public void setUndoExpiresAt(Instant undoExpiresAt) {
        this.undoExpiresAt = undoExpiresAt;
    }

    public Instant getUndoneAt() {
        return undoneAt;
    }

    public void setUndoneAt(Instant undoneAt) {
        this.undoneAt = undoneAt;
    }

    public Long getUndoActivityId() {
        return undoActivityId;
    }

    public void setUndoActivityId(Long undoActivityId) {
        this.undoActivityId = undoActivityId;
    }

    public Integer getCountAfterUndo() {
        return countAfterUndo;
    }

    public void setCountAfterUndo(Integer countAfterUndo) {
        this.countAfterUndo = countAfterUndo;
    }
}
