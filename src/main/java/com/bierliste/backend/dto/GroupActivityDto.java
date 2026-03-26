package com.bierliste.backend.dto;

import com.bierliste.backend.model.ActivityType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(
    description = "Gruppenaktivitaet",
    discriminatorProperty = "type",
    discriminatorMapping = {
        @DiscriminatorMapping(value = "STRICH_INCREMENTED", schema = StrichIncrementedGroupActivityDto.class),
        @DiscriminatorMapping(value = "STRICH_INCREMENT_UNDONE", schema = StrichIncrementUndoneGroupActivityDto.class),
        @DiscriminatorMapping(value = "STRICHE_DEDUCTED", schema = StricheDeductedGroupActivityDto.class),
        @DiscriminatorMapping(value = "MONEY_DEDUCTED", schema = MoneyDeductedGroupActivityDto.class),
        @DiscriminatorMapping(value = "USER_JOINED_GROUP", schema = UserJoinedGroupActivityDto.class),
        @DiscriminatorMapping(value = "USER_LEFT_GROUP", schema = UserLeftGroupActivityDto.class),
        @DiscriminatorMapping(value = "ROLE_GRANTED_WART", schema = RoleGrantedWartGroupActivityDto.class),
        @DiscriminatorMapping(value = "ROLE_REVOKED_WART", schema = RoleRevokedWartGroupActivityDto.class),
        @DiscriminatorMapping(value = "GROUP_SETTINGS_CHANGED", schema = GroupSettingsChangedGroupActivityDto.class),
        @DiscriminatorMapping(value = "USER_REMOVED_FROM_GROUP", schema = UserRemovedFromGroupActivityDto.class),
        @DiscriminatorMapping(value = "INVITE_CREATED", schema = InviteCreatedGroupActivityDto.class),
        @DiscriminatorMapping(value = "INVITE_USED", schema = InviteUsedGroupActivityDto.class)
    },
    oneOf = {
        StrichIncrementedGroupActivityDto.class,
        StrichIncrementUndoneGroupActivityDto.class,
        StricheDeductedGroupActivityDto.class,
        MoneyDeductedGroupActivityDto.class,
        UserJoinedGroupActivityDto.class,
        UserLeftGroupActivityDto.class,
        RoleGrantedWartGroupActivityDto.class,
        RoleRevokedWartGroupActivityDto.class,
        GroupSettingsChangedGroupActivityDto.class,
        UserRemovedFromGroupActivityDto.class,
        InviteCreatedGroupActivityDto.class,
        InviteUsedGroupActivityDto.class
    }
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = StrichIncrementedGroupActivityDto.class, name = "STRICH_INCREMENTED"),
    @JsonSubTypes.Type(value = StrichIncrementUndoneGroupActivityDto.class, name = "STRICH_INCREMENT_UNDONE"),
    @JsonSubTypes.Type(value = StricheDeductedGroupActivityDto.class, name = "STRICHE_DEDUCTED"),
    @JsonSubTypes.Type(value = MoneyDeductedGroupActivityDto.class, name = "MONEY_DEDUCTED"),
    @JsonSubTypes.Type(value = UserJoinedGroupActivityDto.class, name = "USER_JOINED_GROUP"),
    @JsonSubTypes.Type(value = UserLeftGroupActivityDto.class, name = "USER_LEFT_GROUP"),
    @JsonSubTypes.Type(value = RoleGrantedWartGroupActivityDto.class, name = "ROLE_GRANTED_WART"),
    @JsonSubTypes.Type(value = RoleRevokedWartGroupActivityDto.class, name = "ROLE_REVOKED_WART"),
    @JsonSubTypes.Type(value = GroupSettingsChangedGroupActivityDto.class, name = "GROUP_SETTINGS_CHANGED"),
    @JsonSubTypes.Type(value = UserRemovedFromGroupActivityDto.class, name = "USER_REMOVED_FROM_GROUP"),
    @JsonSubTypes.Type(value = InviteCreatedGroupActivityDto.class, name = "INVITE_CREATED"),
    @JsonSubTypes.Type(value = InviteUsedGroupActivityDto.class, name = "INVITE_USED")
})
public abstract class GroupActivityDto {

    private Long id;
    private Instant timestamp;
    private ActivityType type;
    private GroupActivityUserDto actor;
    private GroupActivityUserDto target;

    protected GroupActivityDto() {
    }

    protected GroupActivityDto(ActivityType type) {
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public ActivityType getType() {
        return type;
    }

    public void setType(ActivityType type) {
        this.type = type;
    }

    public GroupActivityUserDto getActor() {
        return actor;
    }

    public void setActor(GroupActivityUserDto actor) {
        this.actor = actor;
    }

    public GroupActivityUserDto getTarget() {
        return target;
    }

    public void setTarget(GroupActivityUserDto target) {
        this.target = target;
    }
}
