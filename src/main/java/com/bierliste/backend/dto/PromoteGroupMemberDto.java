package com.bierliste.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class PromoteGroupMemberDto {

    @NotNull(message = "darf nicht null sein")
    @Positive(message = "muss größer als 0 sein")
    private Long targetUserId;

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }
}
