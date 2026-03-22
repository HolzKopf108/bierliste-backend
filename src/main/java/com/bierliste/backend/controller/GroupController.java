package com.bierliste.backend.controller;

import com.bierliste.backend.dto.CreateGroupDto;
import com.bierliste.backend.dto.CounterIncrementDto;
import com.bierliste.backend.dto.CounterResponseDto;
import com.bierliste.backend.dto.GroupDto;
import com.bierliste.backend.dto.GroupActivitiesResponseDto;
import com.bierliste.backend.dto.GroupInviteResponseDto;
import com.bierliste.backend.dto.GroupMemberDto;
import com.bierliste.backend.dto.GroupRoleDto;
import com.bierliste.backend.dto.GroupSettingsResponseDto;
import com.bierliste.backend.dto.GroupSettingsUpdateDto;
import com.bierliste.backend.dto.GroupSummaryDto;
import com.bierliste.backend.dto.MoneySettlementCreateDto;
import com.bierliste.backend.dto.PromoteGroupMemberDto;
import com.bierliste.backend.dto.StricheSettlementCreateDto;
import com.bierliste.backend.model.User;
import com.bierliste.backend.service.ActivityService;
import com.bierliste.backend.service.GroupAuthorizationService;
import com.bierliste.backend.service.GroupInviteService;
import com.bierliste.backend.service.GroupService;
import com.bierliste.backend.service.SettlementService;
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
    private final GroupInviteService groupInviteService;
    private final SettlementService settlementService;
    private final ActivityService activityService;

    public GroupController(
        GroupService groupService,
        GroupAuthorizationService groupAuthorizationService,
        GroupInviteService groupInviteService,
        SettlementService settlementService,
        ActivityService activityService
    ) {
        this.groupService = groupService;
        this.groupAuthorizationService = groupAuthorizationService;
        this.groupInviteService = groupInviteService;
        this.settlementService = settlementService;
        this.activityService = activityService;
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

    @GetMapping("/{groupId}/settings")
    public ResponseEntity<GroupSettingsResponseDto> getGroupSettings(@PathVariable Long groupId, @AuthenticationPrincipal User user) {
        groupAuthorizationService.requireMember(groupId, user);
        return ResponseEntity.ok(groupService.getGroupSettingsForUser(groupId, user));
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberDto>> getGroupMembers(@PathVariable Long groupId, @AuthenticationPrincipal User user) {
        groupAuthorizationService.requireMember(groupId, user);
        return ResponseEntity.ok(groupService.getGroupMembersForUser(groupId, user));
    }

    @GetMapping("/{groupId}/activities")
    public ResponseEntity<GroupActivitiesResponseDto> getGroupActivities(
        @PathVariable Long groupId,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) Integer limit,
        @AuthenticationPrincipal User user
    ) {
        groupAuthorizationService.requireMember(groupId, user);
        return ResponseEntity.ok(activityService.getActivities(groupId, cursor, limit, user));
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

    @PutMapping("/{groupId}/settings")
    public ResponseEntity<GroupSettingsResponseDto> updateGroupSettings(
        @PathVariable Long groupId,
        @Valid @RequestBody GroupSettingsUpdateDto dto,
        @AuthenticationPrincipal User user
    ) {
        groupAuthorizationService.requireWart(groupId, user);
        return ResponseEntity.ok(groupService.updateGroupSettings(groupId, dto, user));
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

    @PostMapping("/{groupId}/members/{targetUserId}/counter/increment")
    public ResponseEntity<CounterResponseDto> incrementMemberCounter(
        @PathVariable Long groupId,
        @PathVariable Long targetUserId,
        @Valid @RequestBody CounterIncrementDto dto,
        @AuthenticationPrincipal User user
    ) {
        groupAuthorizationService.requireMember(groupId, user);
        return ResponseEntity.ok(new CounterResponseDto(groupService.incrementMemberCounterForGroup(groupId, targetUserId, dto, user)));
    }

    @PostMapping("/{groupId}/leave")
    public ResponseEntity<Map<String, String>> leaveGroup(@PathVariable Long groupId, @AuthenticationPrincipal User user) {
        groupAuthorizationService.requireMember(groupId, user);
        groupService.leaveGroup(groupId, user);
        return ResponseEntity.ok(Map.of("message", "Gruppe verlassen"));
    }

    @PostMapping("/{groupId}/invites")
    public ResponseEntity<GroupInviteResponseDto> createInvite(@PathVariable Long groupId, @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupInviteService.createInvite(groupId, user));
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

    @PostMapping("/{groupId}/members/{targetUserId}/settlements/money")
    public ResponseEntity<GroupMemberDto> createMoneySettlement(
        @PathVariable Long groupId,
        @PathVariable Long targetUserId,
        @Valid @RequestBody MoneySettlementCreateDto dto,
        @AuthenticationPrincipal User user
    ) {
        groupAuthorizationService.requireWart(groupId, user);
        return ResponseEntity.ok(settlementService.createMoneySettlement(groupId, targetUserId, dto, user));
    }

    @PostMapping("/{groupId}/members/{targetUserId}/settlements/striche")
    public ResponseEntity<GroupMemberDto> createStricheSettlement(
        @PathVariable Long groupId,
        @PathVariable Long targetUserId,
        @Valid @RequestBody StricheSettlementCreateDto dto,
        @AuthenticationPrincipal User user
    ) {
        groupAuthorizationService.requireWart(groupId, user);
        return ResponseEntity.ok(settlementService.createStricheSettlement(groupId, targetUserId, dto, user));
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
