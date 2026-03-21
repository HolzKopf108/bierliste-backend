package com.bierliste.backend.dto;

import com.bierliste.backend.model.ActivityType;
import com.bierliste.backend.model.GroupActivityBookingMode;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeName("STRICH_INCREMENTED")
@Schema(name = "StrichIncrementedGroupActivityDto")
public class StrichIncrementedGroupActivityDto extends GroupActivityDto {

    private Integer amount;
    private GroupActivityBookingMode mode;

    public StrichIncrementedGroupActivityDto() {
        super(ActivityType.STRICH_INCREMENTED);
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public GroupActivityBookingMode getMode() {
        return mode;
    }

    public void setMode(GroupActivityBookingMode mode) {
        this.mode = mode;
    }
}
