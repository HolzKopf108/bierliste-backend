package com.bierliste.backend.dto;

import java.math.BigDecimal;

public class GroupSettingsSnapshotDto {

    private String name;
    private BigDecimal pricePerStrich;
    private Boolean onlyWartsCanBookForOthers;
    private Boolean allowArbitraryMoneySettlements;

    public GroupSettingsSnapshotDto() {
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
