package com.bierliste.backend.dto;

public class GroupSummaryDto {

    private Long id;
    private String name;

    public GroupSummaryDto() {
    }

    public GroupSummaryDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
