package com.bierliste.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class SyncEventsRequestDto {

    @NotNull(message = "darf nicht null sein")
    @Size(max = 100, message = "darf maximal 100 Events enthalten")
    private List<SyncClientEventDto> events;

    public List<SyncClientEventDto> getEvents() {
        return events;
    }

    public void setEvents(List<SyncClientEventDto> events) {
        this.events = events;
    }
}
