package com.bierliste.backend.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class SyncClientEventDto {

    private UUID clientEventId;
    private String type;
    private Long groupId;
    private Map<String, Object> payload;
    private Instant clientTimestamp;

    public UUID getClientEventId() {
        return clientEventId;
    }

    public void setClientEventId(UUID clientEventId) {
        this.clientEventId = clientEventId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public Instant getClientTimestamp() {
        return clientTimestamp;
    }

    public void setClientTimestamp(Instant clientTimestamp) {
        this.clientTimestamp = clientTimestamp;
    }
}
