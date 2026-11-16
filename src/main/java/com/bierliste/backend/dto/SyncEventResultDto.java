package com.bierliste.backend.dto;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatusCode;

public class SyncEventResultDto {

    private UUID clientEventId;
    private SyncEventStatus status;
    private int statusCode;
    private String error;
    private SyncFailureResolution resolution;
    private Map<String, Object> serverStateDelta;

    public SyncEventResultDto() {
    }

    public SyncEventResultDto(
        UUID clientEventId,
        SyncEventStatus status,
        int statusCode,
        String error,
        SyncFailureResolution resolution,
        Map<String, Object> serverStateDelta
    ) {
        this.clientEventId = clientEventId;
        this.status = status;
        this.statusCode = statusCode;
        this.error = error;
        this.resolution = resolution;
        this.serverStateDelta = serverStateDelta == null ? null : new LinkedHashMap<>(serverStateDelta);
    }

    public static SyncEventResultDto applied(UUID clientEventId, Map<String, Object> serverStateDelta) {
        return new SyncEventResultDto(
            clientEventId,
            SyncEventStatus.APPLIED,
            200,
            null,
            null,
            serverStateDelta
        );
    }

    public static SyncEventResultDto duplicate(UUID clientEventId, Map<String, Object> serverStateDelta) {
        return new SyncEventResultDto(
            clientEventId,
            SyncEventStatus.DUPLICATE,
            200,
            null,
            null,
            serverStateDelta
        );
    }

    public static SyncEventResultDto failed(
        UUID clientEventId,
        HttpStatusCode statusCode,
        String error,
        SyncFailureResolution resolution
    ) {
        return new SyncEventResultDto(
            clientEventId,
            SyncEventStatus.FAILED,
            statusCode.value(),
            error,
            resolution,
            null
        );
    }

    public UUID getClientEventId() {
        return clientEventId;
    }

    public void setClientEventId(UUID clientEventId) {
        this.clientEventId = clientEventId;
    }

    public SyncEventStatus getStatus() {
        return status;
    }

    public void setStatus(SyncEventStatus status) {
        this.status = status;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public SyncFailureResolution getResolution() {
        return resolution;
    }

    public void setResolution(SyncFailureResolution resolution) {
        this.resolution = resolution;
    }

    public Map<String, Object> getServerStateDelta() {
        return serverStateDelta;
    }

    public void setServerStateDelta(Map<String, Object> serverStateDelta) {
        this.serverStateDelta = serverStateDelta == null ? null : new LinkedHashMap<>(serverStateDelta);
    }
}
