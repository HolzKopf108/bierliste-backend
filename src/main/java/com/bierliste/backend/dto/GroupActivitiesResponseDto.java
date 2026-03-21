package com.bierliste.backend.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;

public class GroupActivitiesResponseDto {

    @ArraySchema(schema = @Schema(implementation = GroupActivityDto.class))
    private List<GroupActivityDto> items = new ArrayList<>();
    private String nextCursor;

    public GroupActivitiesResponseDto() {
    }

    public GroupActivitiesResponseDto(List<GroupActivityDto> items, String nextCursor) {
        setItems(items);
        this.nextCursor = nextCursor;
    }

    public List<GroupActivityDto> getItems() {
        return items;
    }

    public void setItems(List<GroupActivityDto> items) {
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }
}
