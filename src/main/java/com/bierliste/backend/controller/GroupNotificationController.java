package com.bierliste.backend.controller;

import com.bierliste.backend.dto.GroupMemberNotificationCreateDto;
import com.bierliste.backend.dto.GroupMemberNotificationDto;
import com.bierliste.backend.model.User;
import com.bierliste.backend.service.GroupMemberNotificationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupNotificationController {

    private final GroupMemberNotificationService groupMemberNotificationService;

    public GroupNotificationController(GroupMemberNotificationService groupMemberNotificationService) {
        this.groupMemberNotificationService = groupMemberNotificationService;
    }

    @PostMapping("/{groupId}/members/{targetUserId}/notifications")
    public ResponseEntity<GroupMemberNotificationDto> sendNotification(
        @PathVariable Long groupId,
        @PathVariable Long targetUserId,
        @Valid @RequestBody GroupMemberNotificationCreateDto dto,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(groupMemberNotificationService.sendNotification(groupId, targetUserId, dto, user));
    }

    @GetMapping("/{groupId}/notifications/me")
    public ResponseEntity<List<GroupMemberNotificationDto>> getPendingNotifications(
        @PathVariable Long groupId,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(groupMemberNotificationService.getPendingNotificationsForUser(groupId, user));
    }

    @PostMapping("/{groupId}/notifications/{notificationId}/confirm")
    public ResponseEntity<GroupMemberNotificationDto> confirmNotification(
        @PathVariable Long groupId,
        @PathVariable Long notificationId,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(groupMemberNotificationService.confirmNotification(groupId, notificationId, user));
    }
}
