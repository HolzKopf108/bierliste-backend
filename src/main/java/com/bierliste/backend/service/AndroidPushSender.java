package com.bierliste.backend.service;

import com.bierliste.backend.model.Group;
import com.bierliste.backend.model.GroupMemberNotification;
import java.util.List;

public interface AndroidPushSender {

    PushDispatchResult sendGroupMemberNotification(Group group, GroupMemberNotification notification, List<String> deviceTokens);

    record PushDispatchResult(int attemptedCount, int successCount, List<String> invalidTokens, boolean configured) {
    }
}
