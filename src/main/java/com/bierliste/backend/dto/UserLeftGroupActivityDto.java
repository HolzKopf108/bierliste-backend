package com.bierliste.backend.dto;

import com.bierliste.backend.model.ActivityType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeName("USER_LEFT_GROUP")
@Schema(name = "UserLeftGroupActivityDto")
public class UserLeftGroupActivityDto extends GroupActivityDto {

    public UserLeftGroupActivityDto() {
        super(ActivityType.USER_LEFT_GROUP);
    }
}
