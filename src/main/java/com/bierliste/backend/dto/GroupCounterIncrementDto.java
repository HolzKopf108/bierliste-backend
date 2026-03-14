package com.bierliste.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class GroupCounterIncrementDto {

    @NotNull(message = "darf nicht null sein")
    @Min(value = 1, message = "muss mindestens 1 sein")
    private Integer amount;

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }
}
