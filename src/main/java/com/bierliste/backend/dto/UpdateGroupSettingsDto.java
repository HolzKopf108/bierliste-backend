package com.bierliste.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class UpdateGroupSettingsDto {

    @NotBlank(message = "Gruppenname ist erforderlich")
    @Size(min = 3, max = 120, message = "Gruppenname muss zwischen 3 und 120 Zeichen lang sein")
    private String name;

    @NotNull(message = "pricePerStrich ist erforderlich")
    @DecimalMin(value = "0.00", message = "pricePerStrich darf nicht negativ sein")
    @Digits(integer = 8, fraction = 2, message = "pricePerStrich darf hoechstens 8 Stellen vor und 2 nach dem Komma haben")
    private BigDecimal pricePerStrich;

    @NotNull(message = "onlyWartsCanBookForOthers ist erforderlich")
    private Boolean onlyWartsCanBookForOthers;

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
