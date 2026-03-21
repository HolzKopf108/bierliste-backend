package com.bierliste.backend.dto;

import com.bierliste.backend.model.Group;
import java.math.BigDecimal;

public class GroupSettingsResponseDto {

    private String name;
    private BigDecimal pricePerStrich;
    private Boolean onlyWartsCanBookForOthers;
    private Boolean allowArbitraryMoneySettlements;

    public GroupSettingsResponseDto() {
    }

    public GroupSettingsResponseDto(
        String name,
        BigDecimal pricePerStrich,
        Boolean onlyWartsCanBookForOthers,
        Boolean allowArbitraryMoneySettlements
    ) {
        this.name = name;
        this.pricePerStrich = pricePerStrich;
        this.onlyWartsCanBookForOthers = onlyWartsCanBookForOthers;
        this.allowArbitraryMoneySettlements = allowArbitraryMoneySettlements;
    }

    public static GroupSettingsResponseDto fromEntity(Group group) {
        return new GroupSettingsResponseDto(
            group.getName(),
            group.getPricePerStrich(),
            group.isOnlyWartsCanBookForOthers(),
            group.isAllowArbitraryMoneySettlements()
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

    public Boolean getAllowArbitraryMoneySettlements() {
        return allowArbitraryMoneySettlements;
    }

    public void setAllowArbitraryMoneySettlements(Boolean allowArbitraryMoneySettlements) {
        this.allowArbitraryMoneySettlements = allowArbitraryMoneySettlements;
    }
}
