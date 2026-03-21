package com.bierliste.backend.dto;

import com.bierliste.backend.model.ActivityType;
import com.bierliste.backend.model.GroupSettingsChangedField;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;

@JsonTypeName("GROUP_SETTINGS_CHANGED")
@Schema(name = "GroupSettingsChangedGroupActivityDto")
public class GroupSettingsChangedGroupActivityDto extends GroupActivityDto {

    private List<GroupSettingsChangedField> changedFields = new ArrayList<>();
    private GroupSettingsSnapshotDto oldSettings;
    private GroupSettingsSnapshotDto newSettings;

    public GroupSettingsChangedGroupActivityDto() {
        super(ActivityType.GROUP_SETTINGS_CHANGED);
    }

    public List<GroupSettingsChangedField> getChangedFields() {
        return changedFields;
    }

    public void setChangedFields(List<GroupSettingsChangedField> changedFields) {
        this.changedFields = changedFields == null ? new ArrayList<>() : new ArrayList<>(changedFields);
    }

    public GroupSettingsSnapshotDto getOldSettings() {
        return oldSettings;
    }

    public void setOldSettings(GroupSettingsSnapshotDto oldSettings) {
        this.oldSettings = oldSettings;
    }

    public GroupSettingsSnapshotDto getNewSettings() {
        return newSettings;
    }

    public void setNewSettings(GroupSettingsSnapshotDto newSettings) {
        this.newSettings = newSettings;
    }
}
