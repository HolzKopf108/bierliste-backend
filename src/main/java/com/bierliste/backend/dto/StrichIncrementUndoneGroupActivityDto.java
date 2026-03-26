package com.bierliste.backend.dto;

import com.bierliste.backend.model.ActivityType;
import com.bierliste.backend.model.GroupActivityBookingMode;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeName("STRICH_INCREMENT_UNDONE")
@Schema(name = "StrichIncrementUndoneGroupActivityDto")
public class StrichIncrementUndoneGroupActivityDto extends GroupActivityDto {

    private Integer amount;
    private GroupActivityBookingMode mode;
    private Long incrementRequestId;
    private Long originalActivityId;

    public StrichIncrementUndoneGroupActivityDto() {
        super(ActivityType.STRICH_INCREMENT_UNDONE);
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

    public Long getIncrementRequestId() {
        return incrementRequestId;
    }

    public void setIncrementRequestId(Long incrementRequestId) {
        this.incrementRequestId = incrementRequestId;
    }

    public Long getOriginalActivityId() {
        return originalActivityId;
    }

    public void setOriginalActivityId(Long originalActivityId) {
        this.originalActivityId = originalActivityId;
    }
}
