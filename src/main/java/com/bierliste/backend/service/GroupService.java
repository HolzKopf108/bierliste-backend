package com.bierliste.backend.service;

import com.bierliste.backend.dto.CreateGroupDto;
import com.bierliste.backend.dto.CounterIncrementDto;
import com.bierliste.backend.dto.GroupDto;
import com.bierliste.backend.dto.GroupMemberDto;
import com.bierliste.backend.dto.GroupRoleDto;
import com.bierliste.backend.dto.GroupSettingsResponseDto;
import com.bierliste.backend.dto.GroupSummaryDto;
import com.bierliste.backend.dto.PromoteGroupMemberDto;
import com.bierliste.backend.dto.GroupSettingsUpdateDto;
import com.bierliste.backend.model.Group;
import com.bierliste.backend.model.GroupMember;
import com.bierliste.backend.model.GroupRole;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.GroupMemberRepository;
import com.bierliste.backend.repository.GroupRepository;
import com.bierliste.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final GroupAuthorizationService groupAuthorizationService;
    private final ActivityService activityService;

    public GroupService(GroupRepository groupRepository,
                        GroupMemberRepository groupMemberRepository,
                        UserRepository userRepository,
                        GroupAuthorizationService groupAuthorizationService,
                        ActivityService activityService) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.groupAuthorizationService = groupAuthorizationService;
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

        return groupMemberRepository.findMemberDtosByGroupId(groupId);
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

        Map<String, Object> oldValues = new LinkedHashMap<>();
        oldValues.put("name", group.getName());
        oldValues.put("pricePerStrich", group.getPricePerStrich());
        oldValues.put("onlyWartsCanBookForOthers", group.isOnlyWartsCanBookForOthers());
        oldValues.put("allowArbitraryMoneySettlements", group.isAllowArbitraryMoneySettlements());

        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("name", newName);
        newValues.put("pricePerStrich", newPricePerStrich);
        newValues.put("onlyWartsCanBookForOthers", newOnlyWartsCanBookForOthers);
        newValues.put("allowArbitraryMoneySettlements", newAllowArbitraryMoneySettlements);

        List<String> changedFields = activityService.determineChangedFields(oldValues, newValues);

        group.setName(newName);
        group.setPricePerStrich(newPricePerStrich);
        group.setOnlyWartsCanBookForOthers(newOnlyWartsCanBookForOthers);
        group.setAllowArbitraryMoneySettlements(newAllowArbitraryMoneySettlements);

        if (!changedFields.isEmpty()) {
            activityService.logGroupSettingsChanged(
                groupId,
                user,
                changedFields,
                oldValues,
                newValues
            );
        }

        return GroupSettingsResponseDto.fromEntity(group);
    }

    @Transactional
    public int incrementOwnCounterForGroup(Long groupId, CounterIncrementDto dto, User user) {
        Long userId = groupAuthorizationService.requireAuthenticatedUserId(user);
        groupAuthorizationService.requireMemberEntity(groupId, user);

        groupMemberRepository.incrementStrichCount(groupId, userId, dto.getAmount());

        int updatedCount = groupMemberRepository.findStrichCountByGroup_IdAndUser_Id(groupId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gruppe nicht gefunden"));

        activityService.logStrichIncremented(groupId, user, user, dto.getAmount());

        return updatedCount;
    }

    @Transactional
    public int incrementMemberCounterForGroup(Long groupId, Long targetUserId, CounterIncrementDto dto, User user) {
        GroupMember actorMembership = groupAuthorizationService.requireMemberEntity(groupId, user);
        GroupMember targetMembership = requireTargetMembership(groupId, targetUserId);
        User actorUser = actorMembership.getUser();
        User targetUser = targetMembership.getUser();
        actorUser.getUsername();
        targetUser.getUsername();

        requireCounterIncrementPermission(actorMembership, targetMembership);

        groupMemberRepository.incrementStrichCount(groupId, targetUserId, dto.getAmount());

        int updatedCount = groupMemberRepository.findStrichCountByGroup_IdAndUser_Id(groupId, targetUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gruppenmitglied nicht gefunden"));

        activityService.logStrichIncremented(
            groupId,
            actorUser,
            targetUser,
            dto.getAmount()
        );

        return updatedCount;
    }

    @Transactional
    public void joinGroup(Long groupId, User user) {
        Long userId = groupAuthorizationService.requireAuthenticatedUserId(user);

        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gruppe nicht gefunden"));

        if (groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, userId)) {
            return;
        }

        GroupMember membership = new GroupMember();
        membership.setGroup(group);
        membership.setUser(user);
        membership.setRole(GroupRole.MEMBER);

        try {
            groupMemberRepository.save(membership);
            activityService.logUserJoinedGroup(groupId, user);
        } catch (DataIntegrityViolationException ex) {
            if (!groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, userId)) {
                throw ex;
            }
        }
    }

    @Transactional
    public void leaveGroup(Long groupId, User user) {
        GroupMember membership = groupAuthorizationService.requireMemberEntity(groupId, user);
        activityService.logUserLeftGroup(groupId, membership.getUser());
        removeMembershipAndCleanupGroup(membership);
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
                user,
                targetMembership.getUser(),
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
            && groupMemberRepository.countByGroup_IdAndRole(groupId, GroupRole.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mindestens ein Wart muss in der Gruppe verbleiben");
        }

        if (targetMembership.getRole() != GroupRole.MEMBER) {
            targetMembership.setRole(GroupRole.MEMBER);
            activityService.logRoleRevokedWart(
                groupId,
                user,
                targetMembership.getUser(),
                previousRole,
                targetMembership.getRole()
            );
        }

        return toGroupMemberDto(targetMembership);
    }

    @Transactional
    public void removeUserFromAllGroups(User user) {
        Long userId = groupAuthorizationService.requireAuthenticatedUserId(user);
        List<GroupMember> memberships = groupMemberRepository.findAllByUser_Id(userId);
        for (GroupMember membership : memberships) {
            activityService.logUserLeftGroup(membership.getGroup().getId(), membership.getUser());
            removeMembershipAndCleanupGroup(membership);
        }
    }

    private void removeMembershipAndCleanupGroup(GroupMember membership) {
        Long groupId = membership.getGroup().getId();
        Group group = membership.getGroup();
        boolean wasAdmin = membership.getRole() == GroupRole.ADMIN;

        groupMemberRepository.delete(membership);
        groupMemberRepository.flush();

        if (!groupMemberRepository.existsByGroup_Id(groupId)) {
            groupRepository.delete(group);
            return;
        }

        if (wasAdmin && !groupMemberRepository.existsByGroup_IdAndRole(groupId, GroupRole.ADMIN)) {
            GroupMember newAdmin = groupMemberRepository.findFirstByGroup_IdOrderByJoinedAtAscIdAsc(groupId)
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

        return groupMemberRepository.findByGroup_IdAndUser_Id(groupId, targetUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gruppenmitglied nicht gefunden"));
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
