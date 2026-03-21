package com.bierliste.backend.dto;

import com.bierliste.backend.model.ActivityType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeName("USER_JOINED_GROUP")
@Schema(name = "UserJoinedGroupActivityDto")
public class UserJoinedGroupActivityDto extends GroupActivityDto {

    public UserJoinedGroupActivityDto() {
        super(ActivityType.USER_JOINED_GROUP);
    }
}
