package com.bierliste.backend.dto;

import java.time.Instant;
import java.math.BigDecimal;

public class GroupDto {

    private Long id;
    private String name;
    private Instant createdAt;
    private Long createdByUserId;
    private BigDecimal pricePerStrich;
    private boolean onlyWartsCanBookForOthers;

    public GroupDto() {
    }

    public GroupDto(Long id, String name, Instant createdAt, Long createdByUserId, BigDecimal pricePerStrich, boolean onlyWartsCanBookForOthers) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.createdByUserId = createdByUserId;
        this.pricePerStrich = pricePerStrich;
        this.onlyWartsCanBookForOthers = onlyWartsCanBookForOthers;
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

    public BigDecimal getPricePerStrich() {
        return pricePerStrich;
    }

    public boolean isOnlyWartsCanBookForOthers() {
        return onlyWartsCanBookForOthers;
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

    public void setPricePerStrich(BigDecimal pricePerStrich) {
        this.pricePerStrich = pricePerStrich;
    }

    public void setOnlyWartsCanBookForOthers(boolean onlyWartsCanBookForOthers) {
        this.onlyWartsCanBookForOthers = onlyWartsCanBookForOthers;
    }
}
