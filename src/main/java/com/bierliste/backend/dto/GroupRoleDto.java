package com.bierliste.backend.dto;

import com.bierliste.backend.model.GroupRole;

public class GroupRoleDto {

    private GroupRole role;

    public GroupRoleDto() {
    }

    public GroupRoleDto(GroupRole role) {
        this.role = role;
    }

    public GroupRole getRole() {
        return role;
    }

    public void setRole(GroupRole role) {
        this.role = role;
    }
}
