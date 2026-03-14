package com.bierliste.backend.controller;

import com.bierliste.backend.dto.CreateGroupDto;
import com.bierliste.backend.dto.GroupCounterIncrementDto;
import com.bierliste.backend.dto.GroupDto;
import com.bierliste.backend.dto.GroupMemberDto;
import com.bierliste.backend.dto.GroupSummaryDto;
import com.bierliste.backend.model.User;
import com.bierliste.backend.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public ResponseEntity<List<GroupSummaryDto>> getGroups(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(groupService.getGroupsForUser(user));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDto> getGroup(@PathVariable Long groupId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(groupService.getGroupForUser(groupId, user));
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberDto>> getGroupMembers(@PathVariable Long groupId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(groupService.getGroupMembersForUser(groupId, user));
    }

    @GetMapping("/{groupId}/me/counter")
    public ResponseEntity<Map<String, Integer>> getOwnCounter(@PathVariable Long groupId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("count", groupService.getOwnCounterForGroup(groupId, user)));
    }

    @PostMapping("/{groupId}/me/counter/increment")
    public ResponseEntity<Map<String, Integer>> incrementOwnCounter(
        @PathVariable Long groupId,
        @Valid @RequestBody GroupCounterIncrementDto dto,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(Map.of("count", groupService.incrementOwnCounterForGroup(groupId, dto, user)));
    }

    @PostMapping("/{groupId}/join")
    public ResponseEntity<Map<String, String>> joinGroup(@PathVariable Long groupId, @AuthenticationPrincipal User user) {
        groupService.joinGroup(groupId, user);
        return ResponseEntity.ok(Map.of("message", "Mitgliedschaft aktiv"));
    }

    @PostMapping("/{groupId}/leave")
    public ResponseEntity<Map<String, String>> leaveGroup(@PathVariable Long groupId, @AuthenticationPrincipal User user) {
        groupService.leaveGroup(groupId, user);
        return ResponseEntity.ok(Map.of("message", "Gruppe verlassen"));
    }

    @PostMapping
    public ResponseEntity<GroupDto> createGroup(
        @Valid @RequestBody CreateGroupDto dto,
        @AuthenticationPrincipal User user
    ) {
        GroupDto createdGroup = groupService.createGroup(dto, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdGroup);
    }
}
