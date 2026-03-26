package com.bierliste.backend.dto;

import java.time.Instant;

public class CounterUndoResponseDto {

    private int count;
    private Long incrementRequestId;
    private Instant undoneAt;

    public CounterUndoResponseDto() {
    }

    public CounterUndoResponseDto(int count, Long incrementRequestId, Instant undoneAt) {
        this.count = count;
        this.incrementRequestId = incrementRequestId;
        this.undoneAt = undoneAt;
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

    public Instant getUndoneAt() {
        return undoneAt;
    }

    public void setUndoneAt(Instant undoneAt) {
        this.undoneAt = undoneAt;
    }
}
