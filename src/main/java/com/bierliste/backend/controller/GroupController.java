package com.bierliste.backend.controller;

import com.bierliste.backend.dto.CreateGroupDto;
import com.bierliste.backend.dto.CounterIncrementDto;
import com.bierliste.backend.dto.CounterResponseDto;
import com.bierliste.backend.dto.GroupDto;
import com.bierliste.backend.dto.GroupMemberDto;
import com.bierliste.backend.dto.GroupRoleDto;
import com.bierliste.backend.dto.GroupSummaryDto;
import com.bierliste.backend.dto.PromoteGroupMemberDto;
import com.bierliste.backend.model.User;
import com.bierliste.backend.service.GroupAuthorizationService;
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
    private final GroupAuthorizationService groupAuthorizationService;

    public GroupController(GroupService groupService, GroupAuthorizationService groupAuthorizationService) {
        this.groupService = groupService;
        this.groupAuthorizationService = groupAuthorizationService;
    }

    @GetMapping
    public ResponseEntity<List<GroupSummaryDto>> getGroups(@AuthenticationPrincipal User user) {
        groupAuthorizationService.requireAuthenticatedUserId(user);
        return ResponseEntity.ok(groupService.getGroupsForUser(user));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDto> getGroup(@PathVariable Long groupId, @AuthenticationPrincipal User user) {
        groupAuthorizationService.requireMember(groupId, user);
        return ResponseEntity.ok(groupService.getGroupForUser(groupId, user));
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberDto>> getGroupMembers(@PathVariable Long groupId, @AuthenticationPrincipal User user) {
        groupAuthorizationService.requireMember(groupId, user);
        return ResponseEntity.ok(groupService.getGroupMembersForUser(groupId, user));
    }

    @GetMapping("/{groupId}/me/counter")
    public ResponseEntity<CounterResponseDto> getOwnCounter(@PathVariable Long groupId, @AuthenticationPrincipal User user) {
        groupAuthorizationService.requireMember(groupId, user);
        return ResponseEntity.ok(new CounterResponseDto(groupService.getOwnCounterForGroup(groupId, user)));
    }

    @GetMapping("/{groupId}/me/role")
    public ResponseEntity<GroupRoleDto> getOwnRole(@PathVariable Long groupId, @AuthenticationPrincipal User user) {
        groupAuthorizationService.requireMember(groupId, user);
        return ResponseEntity.ok(groupService.getOwnRoleForGroup(groupId, user));
    }

    @PostMapping("/{groupId}/me/counter/increment")
    public ResponseEntity<CounterResponseDto> incrementOwnCounter(
        @PathVariable Long groupId,
        @Valid @RequestBody CounterIncrementDto dto,
        @AuthenticationPrincipal User user
    ) {
        groupAuthorizationService.requireMember(groupId, user);
        return ResponseEntity.ok(new CounterResponseDto(groupService.incrementOwnCounterForGroup(groupId, dto, user)));
    }

    @PostMapping("/{groupId}/join")
    public ResponseEntity<Map<String, String>> joinGroup(@PathVariable Long groupId, @AuthenticationPrincipal User user) {
        groupAuthorizationService.requireAuthenticatedUserId(user);
        groupService.joinGroup(groupId, user);
        return ResponseEntity.ok(Map.of("message", "Mitgliedschaft aktiv"));
    }

    @PostMapping("/{groupId}/leave")
    public ResponseEntity<Map<String, String>> leaveGroup(@PathVariable Long groupId, @AuthenticationPrincipal User user) {
        groupAuthorizationService.requireMember(groupId, user);
        groupService.leaveGroup(groupId, user);
        return ResponseEntity.ok(Map.of("message", "Gruppe verlassen"));
    }

    @PostMapping("/{groupId}/roles/promote")
    public ResponseEntity<GroupMemberDto> promoteGroupMember(
        @PathVariable Long groupId,
        @Valid @RequestBody PromoteGroupMemberDto dto,
        @AuthenticationPrincipal User user
    ) {
        groupAuthorizationService.requireWart(groupId, user);
        return ResponseEntity.ok(groupService.promoteGroupMember(groupId, dto, user));
    }

    @PostMapping("/{groupId}/roles/demote")
    public ResponseEntity<GroupMemberDto> demoteGroupMember(
        @PathVariable Long groupId,
        @Valid @RequestBody PromoteGroupMemberDto dto,
        @AuthenticationPrincipal User user
    ) {
        groupAuthorizationService.requireWart(groupId, user);
        return ResponseEntity.ok(groupService.demoteGroupMember(groupId, dto, user));
    }

    @PostMapping
    public ResponseEntity<GroupDto> createGroup(
        @Valid @RequestBody CreateGroupDto dto,
        @AuthenticationPrincipal User user
    ) {
        groupAuthorizationService.requireAuthenticatedUserId(user);
        GroupDto createdGroup = groupService.createGroup(dto, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdGroup);
    }
}
