package com.bierliste.backend.dto;

import com.bierliste.backend.model.ActivityType;
import com.bierliste.backend.model.GroupRole;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeName("ROLE_REVOKED_WART")
@Schema(name = "RoleRevokedWartGroupActivityDto")
public class RoleRevokedWartGroupActivityDto extends GroupActivityDto {

    private GroupRole previousRole;
    private GroupRole newRole;

    public RoleRevokedWartGroupActivityDto() {
        super(ActivityType.ROLE_REVOKED_WART);
    }

    public GroupRole getPreviousRole() {
        return previousRole;
    }

    public void setPreviousRole(GroupRole previousRole) {
        this.previousRole = previousRole;
    }

    public GroupRole getNewRole() {
        return newRole;
    }

    public void setNewRole(GroupRole newRole) {
        this.newRole = newRole;
    }
}
