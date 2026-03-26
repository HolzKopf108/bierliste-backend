package com.bierliste.backend.dto;

import java.time.Instant;

public class CounterIncrementResponseDto {

    private int count;
    private Long incrementRequestId;
    private Instant undoExpiresAt;

    public CounterIncrementResponseDto() {
    }

    public CounterIncrementResponseDto(int count, Long incrementRequestId, Instant undoExpiresAt) {
        this.count = count;
        this.incrementRequestId = incrementRequestId;
        this.undoExpiresAt = undoExpiresAt;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Long getIncrementRequestId() {
        return incrementRequestId;
    }

    public void setIncrementRequestId(Long incrementRequestId) {
        this.incrementRequestId = incrementRequestId;
    }

    public Instant getUndoExpiresAt() {
        return undoExpiresAt;
    }

    public void setUndoExpiresAt(Instant undoExpiresAt) {
        this.undoExpiresAt = undoExpiresAt;
    }
}
