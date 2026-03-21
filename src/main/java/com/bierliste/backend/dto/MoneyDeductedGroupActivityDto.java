package com.bierliste.backend.dto;

import com.bierliste.backend.model.ActivityType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@JsonTypeName("MONEY_DEDUCTED")
@Schema(name = "MoneyDeductedGroupActivityDto")
public class MoneyDeductedGroupActivityDto extends GroupActivityDto {

    private BigDecimal amountMoney;
    private BigDecimal pricePerStrich;

    public MoneyDeductedGroupActivityDto() {
        super(ActivityType.MONEY_DEDUCTED);
    }

    public BigDecimal getAmountMoney() {
        return amountMoney;
    }

    public void setAmountMoney(BigDecimal amountMoney) {
        this.amountMoney = amountMoney;
    }

    public BigDecimal getPricePerStrich() {
        return pricePerStrich;
    }

    public void setPricePerStrich(BigDecimal pricePerStrich) {
        this.pricePerStrich = pricePerStrich;
    }
}
