package com.bierliste.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class MoneySettlementCreateDto {

    @NotNull(message = "darf nicht null sein")
    @DecimalMin(value = "0.00", inclusive = false, message = "muss groesser als 0 sein")
    @Digits(integer = 8, fraction = 2, message = "darf hoechstens 8 Stellen vor und 2 nach dem Komma haben")
    private BigDecimal amount;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
