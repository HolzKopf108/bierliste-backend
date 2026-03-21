package com.bierliste.backend.dto;

import com.bierliste.backend.model.ActivityType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeName("INVITE_USED")
@Schema(name = "InviteUsedGroupActivityDto")
public class InviteUsedGroupActivityDto extends GroupActivityDto {

    public InviteUsedGroupActivityDto() {
        super(ActivityType.INVITE_USED);
    }
}
