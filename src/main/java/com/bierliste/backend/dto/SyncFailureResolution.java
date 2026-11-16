package com.bierliste.backend.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SyncFailureResolution {
    DROP,
    RETRY;

    @JsonValue
    public String getJsonValue() {
        return name().toLowerCase();
    }
}
