package com.bierliste.backend.dto;

import com.bierliste.backend.model.Group;
import java.math.BigDecimal;

public class GroupSettingsDto {

    private Long groupId;
    private String name;
    private BigDecimal pricePerStrich;
    private Boolean onlyWartsCanBookForOthers;

    public GroupSettingsDto() {
    }

    public GroupSettingsDto(Long groupId, String name, BigDecimal pricePerStrich, Boolean onlyWartsCanBookForOthers) {
        this.groupId = groupId;
        this.name = name;
        this.pricePerStrich = pricePerStrich;
        this.onlyWartsCanBookForOthers = onlyWartsCanBookForOthers;
    }

    public static GroupSettingsDto fromEntity(Group group) {
        return new GroupSettingsDto(
            group.getId(),
            group.getName(),
            group.getPricePerStrich(),
            group.isOnlyWartsCanBookForOthers()
        );
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
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
