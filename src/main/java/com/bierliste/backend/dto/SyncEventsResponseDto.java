package com.bierliste.backend.dto;

import java.util.List;

public class SyncEventsResponseDto {

    private List<SyncEventResultDto> results;

    public SyncEventsResponseDto() {
    }

    public SyncEventsResponseDto(List<SyncEventResultDto> results) {
        this.results = results;
    }

    public List<SyncEventResultDto> getResults() {
        return results;
    }

    public void setResults(List<SyncEventResultDto> results) {
        this.results = results;
    }
}
