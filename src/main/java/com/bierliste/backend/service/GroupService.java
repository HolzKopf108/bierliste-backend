package com.bierliste.backend.service;

import com.bierliste.backend.dto.CreateGroupDto;
import com.bierliste.backend.dto.CounterIncrementDto;
import com.bierliste.backend.dto.CounterIncrementResponseDto;
import com.bierliste.backend.dto.CounterUndoResponseDto;
import com.bierliste.backend.dto.GroupDto;
import com.bierliste.backend.dto.GroupMemberDto;
import com.bierliste.backend.dto.GroupRoleDto;
import com.bierliste.backend.dto.GroupSummaryDto;
import com.bierliste.backend.dto.GroupSettingsResponseDto;
import com.bierliste.backend.dto.GroupSettingsUpdateDto;
import com.bierliste.backend.dto.PromoteGroupMemberDto;
import com.bierliste.backend.model.CounterIncrementRequest;
import com.bierliste.backend.model.Group;
import com.bierliste.backend.model.GroupActivity;
import com.bierliste.backend.model.GroupActivityBookingMode;
import com.bierliste.backend.model.GroupInvitePermission;
import com.bierliste.backend.model.GroupMember;
import com.bierliste.backend.model.GroupRole;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.CounterIncrementRequestRepository;
import com.bierliste.backend.repository.GroupInviteRepository;
import com.bierliste.backend.repository.GroupMemberRepository;
import com.bierliste.backend.repository.GroupRepository;
import com.bierliste.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GroupService {

    private static final String GROUP_NOT_FOUND_MESSAGE = "Gruppe nicht gefunden";
    private static final String TARGET_NOT_FOUND_MESSAGE = "Gruppenmitglied nicht gefunden";
    private static final String INCREMENT_REQUEST_NOT_FOUND_MESSAGE = "Strich-Request nicht gefunden";
    private static final String INCREMENT_REQUEST_NOT_REVERSIBLE_MESSAGE = "Strich-Request kann nicht mehr rückgängig gemacht werden";
    private static final String UNDO_WINDOW_EXPIRED_MESSAGE = "Undo-Zeitfenster abgelaufen";

    private final GroupRepository groupRepository;
    private final GroupInviteRepository groupInviteRepository;
    private final CounterIncrementRequestRepository counterIncrementRequestRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final GroupAuthorizationService groupAuthorizationService;
    private final ActivityService activityService;
    private final Duration counterUndoWindow;

    public GroupService(GroupRepository groupRepository,
                        GroupInviteRepository groupInviteRepository,
                        CounterIncrementRequestRepository counterIncrementRequestRepository,
                        GroupMemberRepository groupMemberRepository,
                        UserRepository userRepository,
                        GroupAuthorizationService groupAuthorizationService,
                        @Value("${app.counter.undo-window:PT30S}") Duration counterUndoWindow,
                        ActivityService activityService) {
        this.groupRepository = groupRepository;
        this.groupInviteRepository = groupInviteRepository;
        this.counterIncrementRequestRepository = counterIncrementRequestRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.groupAuthorizationService = groupAuthorizationService;
        if (counterUndoWindow.isZero() || counterUndoWindow.isNegative()) {
            throw new IllegalArgumentException("app.counter.undo-window muss groesser als 0 sein");
        }
        this.counterUndoWindow = counterUndoWindow;
        this.activityService = activityService;
    }

    @Transactional
    public GroupDto createGroup(CreateGroupDto dto, User creator) {
        groupAuthorizationService.requireAuthenticatedUserId(creator);

        Group group = new Group();
        group.setName(dto.getName().trim());
        group.setCreatedByUser(creator);

        Group savedGroup = groupRepository.save(group);

        GroupMember membership = new GroupMember();
        membership.setGroup(savedGroup);
        membership.setUser(creator);
        membership.setRole(GroupRole.ADMIN);

        groupMemberRepository.save(membership);

        return toGroupDto(savedGroup);
    }

    public List<GroupSummaryDto> getGroupsForUser(User user) {
        Long userId = groupAuthorizationService.requireAuthenticatedUserId(user);

        return groupRepository.findDistinctByMembers_User_IdOrderByNameAsc(userId)
            .stream()
            .map(group -> new GroupSummaryDto(group.getId(), group.getName()))
            .toList();
    }

    public GroupDto getGroupForUser(Long groupId, User user) {
        Group group = groupAuthorizationService.requireMemberGroup(groupId, user);
        return toGroupDto(group);
    }

    public GroupSettingsResponseDto getGroupSettingsForUser(Long groupId, User user) {
        Group group = groupAuthorizationService.requireMemberGroup(groupId, user);
        return GroupSettingsResponseDto.fromEntity(group);
    }

    public List<GroupMemberDto> getGroupMembersForUser(Long groupId, User user) {
        groupAuthorizationService.requireMember(groupId, user);

        return groupMemberRepository.findActiveMemberDtosByGroupId(groupId);
    }

    public int getOwnCounterForGroup(Long groupId, User user) {
        GroupMember membership = groupAuthorizationService.requireMemberEntity(groupId, user);
        return membership.getStrichCount();
    }

    public GroupRoleDto getOwnRoleForGroup(Long groupId, User user) {
        GroupMember membership = groupAuthorizationService.requireMemberEntity(groupId, user);
        return new GroupRoleDto(membership.getRole());
    }

    @Transactional
    public GroupSettingsResponseDto updateGroupSettings(Long groupId, GroupSettingsUpdateDto dto, User user) {
        groupAuthorizationService.requireWart(groupId, user);

        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gruppe nicht gefunden"));

        String newName = dto.getName().trim();
        BigDecimal newPricePerStrich = dto.getPricePerStrich();
        boolean newOnlyWartsCanBookForOthers = dto.getOnlyWartsCanBookForOthers();
        boolean newAllowArbitraryMoneySettlements = dto.getAllowArbitraryMoneySettlements();
        GroupInvitePermission newInvitePermission = dto.getInvitePermission();

        Map<String, Object> oldValues = new LinkedHashMap<>();
        oldValues.put("name", group.getName());
        oldValues.put("pricePerStrich", group.getPricePerStrich());
        oldValues.put("onlyWartsCanBookForOthers", group.isOnlyWartsCanBookForOthers());
        oldValues.put("allowArbitraryMoneySettlements", group.isAllowArbitraryMoneySettlements());
        oldValues.put("invitePermission", group.getInvitePermission());

        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("name", newName);
        newValues.put("pricePerStrich", newPricePerStrich);
        newValues.put("onlyWartsCanBookForOthers", newOnlyWartsCanBookForOthers);
        newValues.put("allowArbitraryMoneySettlements", newAllowArbitraryMoneySettlements);
        newValues.put("invitePermission", newInvitePermission);

        List<String> changedFields = activityService.determineChangedFields(oldValues, newValues);

        group.setName(newName);
        group.setPricePerStrich(newPricePerStrich);
        group.setOnlyWartsCanBookForOthers(newOnlyWartsCanBookForOthers);
        group.setAllowArbitraryMoneySettlements(newAllowArbitraryMoneySettlements);
        group.setInvitePermission(newInvitePermission);

        if (!changedFields.isEmpty()) {
            activityService.logGroupSettingsChanged(
                groupId,
                ActivityUserRef.from(user),
                changedFields,
                oldValues,
                newValues
            );
        }

        return GroupSettingsResponseDto.fromEntity(group);
    }

    @Transactional
    public CounterIncrementResponseDto incrementOwnCounterForGroup(Long groupId, CounterIncrementDto dto, User user) {
        Long userId = groupAuthorizationService.requireAuthenticatedUserId(user);
        GroupMember membership = groupMemberRepository.findByGroup_IdAndUser_IdAndActiveTrueForUpdate(groupId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, GROUP_NOT_FOUND_MESSAGE));

        return createCounterIncrement(groupId, membership, membership, dto.getAmount());
    }

    @Transactional
    public CounterIncrementResponseDto incrementMemberCounterForGroup(Long groupId, Long targetUserId, CounterIncrementDto dto, User user) {
        GroupMember actorMembership = groupAuthorizationService.requireMemberEntity(groupId, user);
        GroupMember targetMembership = requireTargetMembershipForUpdate(groupId, targetUserId);

        requireCounterIncrementPermission(actorMembership, targetMembership);

        return createCounterIncrement(groupId, actorMembership, targetMembership, dto.getAmount());
    }

    @Transactional
    public CounterUndoResponseDto undoCounterIncrementForGroup(Long groupId, Long incrementRequestId, User user) {
        Long actorUserId = groupAuthorizationService.requireAuthenticatedUserId(user);
        CounterIncrementRequest incrementRequest = counterIncrementRequestRepository.findByIdAndGroupIdForUpdate(incrementRequestId, groupId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, INCREMENT_REQUEST_NOT_FOUND_MESSAGE));

        if (!incrementRequest.getActorUserId().equals(actorUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, INCREMENT_REQUEST_NOT_FOUND_MESSAGE);
        }

        if (incrementRequest.getUndoneAt() != null) {
            return new CounterUndoResponseDto(
                requireRecordedUndoCount(incrementRequest),
                incrementRequest.getId(),
                incrementRequest.getUndoneAt()
            );
        }

        Instant now = Instant.now();
        if (now.isAfter(incrementRequest.getUndoExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, UNDO_WINDOW_EXPIRED_MESSAGE);
        }

        GroupMember targetMembership = groupMemberRepository.findByGroup_IdAndUser_IdForUpdate(groupId, incrementRequest.getTargetUserId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, INCREMENT_REQUEST_NOT_REVERSIBLE_MESSAGE));

        if (targetMembership.getStrichCount() < incrementRequest.getAmount()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, INCREMENT_REQUEST_NOT_REVERSIBLE_MESSAGE);
        }

        targetMembership.setStrichCount(targetMembership.getStrichCount() - incrementRequest.getAmount());

        GroupActivity undoActivity = activityService.logStrichIncrementUndone(
            groupId,
            ActivityUserRef.from(user),
            ActivityUserRef.from(targetMembership.getUser()),
            incrementRequest.getAmount(),
            incrementRequest.getMode(),
            incrementRequest.getId(),
            incrementRequest.getIncrementActivityId()
        );

        incrementRequest.setUndoneAt(now);
        incrementRequest.setUndoActivityId(undoActivity.getId());
        incrementRequest.setCountAfterUndo(targetMembership.getStrichCount());

        return new CounterUndoResponseDto(targetMembership.getStrichCount(), incrementRequest.getId(), now);
    }

    @Transactional
    public void leaveGroup(Long groupId, User user) {
        GroupMember membership = groupAuthorizationService.requireMemberEntity(groupId, user);
        ActivityUserRef leavingUserRef = ActivityUserRef.from(membership.getUser());

        deactivateMembershipAndCleanupGroup(membership);
        activityService.logUserLeftGroup(groupId, leavingUserRef);
    }

    @Transactional
    public void removeGroupMember(Long groupId, Long targetUserId, User user) {
        groupAuthorizationService.requireWart(groupId, user);

        Long actorUserId = groupAuthorizationService.requireAuthenticatedUserId(user);
        if (actorUserId.equals(targetUserId)) {
            leaveGroup(groupId, user);
            return;
        }

        GroupMember targetMembership = requireTargetMembership(groupId, targetUserId);
        ActivityUserRef actorUserRef = ActivityUserRef.from(user);
        ActivityUserRef targetUserRef = ActivityUserRef.from(targetMembership.getUser());

        deactivateMembershipAndCleanupGroup(targetMembership);
        activityService.logUserRemovedFromGroup(groupId, actorUserRef, targetUserRef);
    }

    @Transactional
    public GroupMemberDto promoteGroupMember(Long groupId, PromoteGroupMemberDto dto, User user) {
        groupAuthorizationService.requireWart(groupId, user);

        GroupMember targetMembership = requireTargetMembership(groupId, dto.getTargetUserId());
        GroupRole previousRole = targetMembership.getRole();

        if (targetMembership.getRole() != GroupRole.ADMIN) {
            targetMembership.setRole(GroupRole.ADMIN);
            activityService.logRoleGrantedWart(
                groupId,
                ActivityUserRef.from(user),
                ActivityUserRef.from(targetMembership.getUser()),
                previousRole,
                targetMembership.getRole()
            );
        }

        return toGroupMemberDto(targetMembership);
    }

    @Transactional
    public GroupMemberDto demoteGroupMember(Long groupId, PromoteGroupMemberDto dto, User user) {
        groupAuthorizationService.requireWart(groupId, user);

        GroupMember targetMembership = requireTargetMembership(groupId, dto.getTargetUserId());
        GroupRole previousRole = targetMembership.getRole();

        if (targetMembership.getRole() == GroupRole.ADMIN
            && groupMemberRepository.countByGroup_IdAndRoleAndActiveTrue(groupId, GroupRole.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mindestens ein Wart muss in der Gruppe verbleiben");
        }

        if (targetMembership.getRole() != GroupRole.MEMBER) {
            targetMembership.setRole(GroupRole.MEMBER);
            activityService.logRoleRevokedWart(
                groupId,
                ActivityUserRef.from(user),
                ActivityUserRef.from(targetMembership.getUser()),
                previousRole,
                targetMembership.getRole()
            );
        }

        return toGroupMemberDto(targetMembership);
    }

    @Transactional
    public void removeUserFromAllGroups(User user) {
        Long userId = groupAuthorizationService.requireAuthenticatedUserId(user);
        ActivityUserRef userRef = ActivityUserRef.from(user);
        List<GroupMember> memberships = groupMemberRepository.findAllByUser_IdAndActiveTrue(userId);
        for (GroupMember membership : memberships) {
            deactivateMembershipAndCleanupGroup(membership);
            activityService.logUserLeftGroup(membership.getGroup().getId(), userRef);
        }
    }

    private void deactivateMembershipAndCleanupGroup(GroupMember membership) {
        Long groupId = membership.getGroup().getId();
        Group group = membership.getGroup();
        boolean wasAdmin = membership.getRole() == GroupRole.ADMIN;

        membership.setActive(false);
        membership.setLeftAt(Instant.now());
        groupMemberRepository.flush();

        if (!groupMemberRepository.existsByGroup_IdAndActiveTrue(groupId)) {
            groupMemberRepository.delete(membership);
            groupMemberRepository.flush();
            groupInviteRepository.deleteAllByGroup_Id(groupId);
            groupRepository.delete(group);
            return;
        }

        if (wasAdmin && !groupMemberRepository.existsByGroup_IdAndRoleAndActiveTrue(groupId, GroupRole.ADMIN)) {
            GroupMember newAdmin = groupMemberRepository.findFirstByGroup_IdAndActiveTrueOrderByJoinedAtAscIdAsc(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Admin konnte nicht neu zugewiesen werden"));
            newAdmin.setRole(GroupRole.ADMIN);
        }
    }

    private void requireCounterIncrementPermission(GroupMember actorMembership, GroupMember targetMembership) {
        if (actorMembership.getUser().getId().equals(targetMembership.getUser().getId())) {
            return;
        }

        if (!actorMembership.getGroup().isOnlyWartsCanBookForOthers()) {
            return;
        }

        if (actorMembership.getRole() != GroupRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wart-Rechte erforderlich");
        }
    }

    private GroupMember requireTargetMembership(Long groupId, Long targetUserId) {
        if (!userRepository.existsById(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User nicht gefunden");
        }

        return groupMemberRepository.findByGroup_IdAndUser_IdAndActiveTrue(groupId, targetUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, TARGET_NOT_FOUND_MESSAGE));
    }

    private GroupMember requireTargetMembershipForUpdate(Long groupId, Long targetUserId) {
        if (!userRepository.existsById(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User nicht gefunden");
        }

        return groupMemberRepository.findByGroup_IdAndUser_IdAndActiveTrueForUpdate(groupId, targetUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, TARGET_NOT_FOUND_MESSAGE));
    }

    private CounterIncrementResponseDto createCounterIncrement(
        Long groupId,
        GroupMember actorMembership,
        GroupMember targetMembership,
        int amount
    ) {
        ActivityUserRef actorUserRef = ActivityUserRef.from(actorMembership.getUser());
        ActivityUserRef targetUserRef = ActivityUserRef.from(targetMembership.getUser());
        GroupActivityBookingMode bookingMode = actorUserRef.userId().equals(targetUserRef.userId())
            ? GroupActivityBookingMode.SELF
            : GroupActivityBookingMode.OTHER;

        targetMembership.setStrichCount(targetMembership.getStrichCount() + amount);

        GroupActivity incrementActivity = activityService.logStrichIncremented(groupId, actorUserRef, targetUserRef, amount);
        Instant now = Instant.now();

        CounterIncrementRequest incrementRequest = new CounterIncrementRequest();
        incrementRequest.setGroupId(groupId);
        incrementRequest.setActorUserId(actorUserRef.userId());
        incrementRequest.setTargetUserId(targetUserRef.userId());
        incrementRequest.setAmount(amount);
        incrementRequest.setMode(bookingMode);
        incrementRequest.setIncrementActivityId(incrementActivity.getId());
        incrementRequest.setCreatedAt(now);
        incrementRequest.setUndoExpiresAt(now.plus(counterUndoWindow));
        counterIncrementRequestRepository.save(incrementRequest);

        return new CounterIncrementResponseDto(
            targetMembership.getStrichCount(),
            incrementRequest.getId(),
            incrementRequest.getUndoExpiresAt()
        );
    }

    private int requireRecordedUndoCount(CounterIncrementRequest incrementRequest) {
        Integer countAfterUndo = incrementRequest.getCountAfterUndo();
        if (countAfterUndo == null) {
            throw new IllegalStateException("Undo-Zaehlerstand fehlt fuer Strich-Request " + incrementRequest.getId());
        }
        return countAfterUndo;
    }

    private GroupMemberDto toGroupMemberDto(GroupMember membership) {
        return new GroupMemberDto(
            membership.getUser().getId(),
            membership.getUser().getUsername(),
            membership.getJoinedAt(),
            membership.getRole(),
            membership.getStrichCount()
        );
    }

    private GroupDto toGroupDto(Group group) {
        return new GroupDto(
            group.getId(),
            group.getName(),
            group.getCreatedAt(),
            group.getCreatedByUserId(),
            group.getPricePerStrich(),
            group.isOnlyWartsCanBookForOthers()
        );
    }
}
