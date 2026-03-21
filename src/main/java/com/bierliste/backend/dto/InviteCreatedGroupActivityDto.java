package com.bierliste.backend.dto;

import com.bierliste.backend.model.ActivityType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeName("INVITE_CREATED")
@Schema(name = "InviteCreatedGroupActivityDto")
public class InviteCreatedGroupActivityDto extends GroupActivityDto {

    public InviteCreatedGroupActivityDto() {
        super(ActivityType.INVITE_CREATED);
    }
}
