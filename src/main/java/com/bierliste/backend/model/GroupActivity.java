package com.bierliste.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(
    name = "group_activities",
    indexes = {
        @Index(name = "idx_group_activities_group_timestamp_id", columnList = "group_id, activity_timestamp, id"),
        @Index(name = "idx_group_activities_actor_user", columnList = "actor_user_id"),
        @Index(name = "idx_group_activities_target_user", columnList = "target_user_id")
    }
)
@Check(name = "ck_group_activities_actor_target_state", constraints = """
    group_id > 0
    and meta_version >= 1
    and (actor_user_id is null or actor_user_id > 0)
    and (target_user_id is null or target_user_id > 0)
    and (
        (target_user_id is null and target_display_name_snapshot is null)
        or target_display_name_snapshot is not null
    )
    """)
public class GroupActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Positive
    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "activity_timestamp", nullable = false, updatable = false)
    private Instant timestamp = Instant.now();

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @NotBlank
    @Column(name = "actor_display_name_snapshot", nullable = false, length = 50)
    private String actorDisplayNameSnapshot;

    @Positive
    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "target_display_name_snapshot", length = 50)
    private String targetDisplayNameSnapshot;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ActivityType type;

    @Min(1)
    @ColumnDefault("1")
    @Column(name = "meta_version", nullable = false)
    private int metaVersion = 1;

    @NotNull
    @ColumnDefault("'{}'")
    @Convert(converter = JsonMapConverter.class)
    @Column(name = "meta_json", nullable = false, columnDefinition = "TEXT")
    private Map<String, Object> meta = new LinkedHashMap<>();

    public Long getId() {
        return id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActorDisplayNameSnapshot() {
        return actorDisplayNameSnapshot;
    }

    public void setActorDisplayNameSnapshot(String actorDisplayNameSnapshot) {
        this.actorDisplayNameSnapshot = actorDisplayNameSnapshot;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getTargetDisplayNameSnapshot() {
        return targetDisplayNameSnapshot;
    }

    public void setTargetDisplayNameSnapshot(String targetDisplayNameSnapshot) {
        this.targetDisplayNameSnapshot = targetDisplayNameSnapshot;
    }

    public ActivityType getType() {
        return type;
    }

    public void setType(ActivityType type) {
        this.type = type;
    }

    public int getMetaVersion() {
        return metaVersion;
    }

    public void setMetaVersion(int metaVersion) {
        this.metaVersion = metaVersion;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, Object> meta) {
        this.meta = meta == null ? new LinkedHashMap<>() : new LinkedHashMap<>(meta);
    }
}
