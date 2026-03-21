package com.bierliste.backend.dto;

import com.bierliste.backend.model.ActivityType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeName("STRICHE_DEDUCTED")
@Schema(name = "StricheDeductedGroupActivityDto")
public class StricheDeductedGroupActivityDto extends GroupActivityDto {

    private Integer amountStriche;

    public StricheDeductedGroupActivityDto() {
        super(ActivityType.STRICHE_DEDUCTED);
    }

    public Integer getAmountStriche() {
        return amountStriche;
    }

    public void setAmountStriche(Integer amountStriche) {
        this.amountStriche = amountStriche;
    }
}
