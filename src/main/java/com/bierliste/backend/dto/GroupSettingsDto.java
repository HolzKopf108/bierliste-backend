package com.bierliste.backend.dto;

import com.bierliste.backend.model.Group;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class GroupSettingsDto {

    @NotNull(message = "pricePerStrich ist erforderlich")
    @DecimalMin(value = "0.01", message = "pricePerStrich muss groesser als 0 sein")
    @Digits(integer = 8, fraction = 2, message = "pricePerStrich darf hoechstens 8 Stellen vor und 2 nach dem Komma haben")
    private BigDecimal pricePerStrich;

    @NotNull(message = "onlyWartsCanBookForOthers ist erforderlich")
    private Boolean onlyWartsCanBookForOthers;

    public GroupSettingsDto() {
    }

    public GroupSettingsDto(BigDecimal pricePerStrich, Boolean onlyWartsCanBookForOthers) {
        this.pricePerStrich = pricePerStrich;
        this.onlyWartsCanBookForOthers = onlyWartsCanBookForOthers;
    }

    public static GroupSettingsDto fromEntity(Group group) {
        return new GroupSettingsDto(group.getPricePerStrich(), group.isOnlyWartsCanBookForOthers());
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
