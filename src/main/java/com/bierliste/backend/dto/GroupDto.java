package com.bierliste.backend.dto;

import java.time.Instant;

public class GroupDto {

    private Long id;
    private String name;
    private Instant createdAt;
    private Long createdByUserId;

    public GroupDto() {
    }

    public GroupDto(Long id, String name, Instant createdAt, Long createdByUserId) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.createdByUserId = createdByUserId;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }
}