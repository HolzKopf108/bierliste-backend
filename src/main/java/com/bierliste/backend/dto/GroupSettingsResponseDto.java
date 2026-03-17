package com.bierliste.backend.dto;

import com.bierliste.backend.model.Group;
import java.math.BigDecimal;

public class GroupSettingsResponseDto {

    private String name;
    private BigDecimal pricePerStrich;
    private Boolean onlyWartsCanBookForOthers;

    public GroupSettingsResponseDto() {
    }

    public GroupSettingsResponseDto(String name, BigDecimal pricePerStrich, Boolean onlyWartsCanBookForOthers) {
        this.name = name;
        this.pricePerStrich = pricePerStrich;
        this.onlyWartsCanBookForOthers = onlyWartsCanBookForOthers;
    }

    public static GroupSettingsResponseDto fromEntity(Group group) {
        return new GroupSettingsResponseDto(
            group.getName(),
            group.getPricePerStrich(),
            group.isOnlyWartsCanBookForOthers()
        );
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPricePerStrich() {
        return pricePerStrich;
    }

    public void setPricePerStrich(BigDecimal pricePerStrich) {
        this.pricePerStrich = pricePerStrich;
    }

    public Boolean getOnlyWartsCanBookForOthers() {
        return onlyWartsCanBookForOthers;
    }

    public void setOnlyWartsCanBookForOthers(Boolean onlyWartsCanBookForOthers) {
        this.onlyWartsCanBookForOthers = onlyWartsCanBookForOthers;
    }
}
