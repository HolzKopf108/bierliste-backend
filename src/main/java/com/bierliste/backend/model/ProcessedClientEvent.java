package com.bierliste.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(
    name = "processed_client_event",
    indexes = {
        @Index(name = "idx_processed_client_event_user", columnList = "user_id"),
        @Index(name = "idx_processed_client_event_processed_at", columnList = "processed_at"),
        @Index(name = "idx_processed_client_event_group", columnList = "group_id")
    }
)
@Check(
    name = "ck_processed_client_event_state",
    constraints = "user_id > 0 and group_id > 0"
)
public class ProcessedClientEvent {

    @Id
    @Column(name = "client_event_id", nullable = false, updatable = false)
    private UUID clientEventId;

    @NotNull
    @Positive
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40, updatable = false)
    private ClientEventType type;

    @NotNull
    @Positive
    @Column(name = "group_id", nullable = false, updatable = false)
    private Long groupId;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt = Instant.now();

    @NotNull
    @ColumnDefault("'{}'")
    @Convert(converter = JsonMapConverter.class)
    @Column(name = "result_json", nullable = false, columnDefinition = "TEXT")
    private Map<String, Object> result = new LinkedHashMap<>();

    public UUID getClientEventId() {
        return clientEventId;
    }

    public void setClientEventId(UUID clientEventId) {
        this.clientEventId = clientEventId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public ClientEventType getType() {
        return type;
    }

    public void setType(ClientEventType type) {
        this.type = type;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result == null ? new LinkedHashMap<>() : new LinkedHashMap<>(result);
    }
}
