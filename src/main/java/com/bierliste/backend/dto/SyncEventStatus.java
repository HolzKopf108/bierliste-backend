package com.bierliste.backend.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SyncEventStatus {
    APPLIED,
    DUPLICATE,
    FAILED;

    @JsonValue
    public String getJsonValue() {
        return name().toLowerCase();
    }
}
