package com.bierliste.backend.dto;

import com.bierliste.backend.model.ActivityType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeName("USER_REMOVED_FROM_GROUP")
@Schema(name = "UserRemovedFromGroupActivityDto")
public class UserRemovedFromGroupActivityDto extends GroupActivityDto {

    public UserRemovedFromGroupActivityDto() {
        super(ActivityType.USER_REMOVED_FROM_GROUP);
    }
}
