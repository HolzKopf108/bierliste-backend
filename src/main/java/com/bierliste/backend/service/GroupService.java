package com.bierliste.backend.service;

import com.bierliste.backend.dto.CreateGroupDto;
import com.bierliste.backend.dto.GroupDto;
import com.bierliste.backend.dto.GroupMemberDto;
import com.bierliste.backend.dto.GroupSummaryDto;
import com.bierliste.backend.model.Group;
import com.bierliste.backend.model.GroupMember;
import com.bierliste.backend.model.GroupRole;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.GroupMemberRepository;
import com.bierliste.backend.repository.GroupRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    public GroupService(GroupRepository groupRepository,
                        GroupMemberRepository groupMemberRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    @Transactional
    public GroupDto createGroup(CreateGroupDto dto, User creator) {
        if (creator == null || creator.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nicht authentifiziert");
        }

        Group group = new Group();
        group.setName(dto.getName().trim());
        group.setCreatedByUserId(creator.getId());

        Group savedGroup = groupRepository.save(group);

        GroupMember membership = new GroupMember();
        membership.setGroup(savedGroup);
        membership.setUser(creator);
        membership.setRole(GroupRole.ADMIN);

        groupMemberRepository.save(membership);

        return new GroupDto(
            savedGroup.getId(),
            savedGroup.getName(),
            savedGroup.getCreatedAt(),
            savedGroup.getCreatedByUserId()
        );
    }

    public List<GroupSummaryDto> getGroupsForUser(User user) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nicht authentifiziert");
        }

        return groupRepository.findDistinctByMembers_User_IdOrderByNameAsc(user.getId())
            .stream()
            .map(group -> new GroupSummaryDto(group.getId(), group.getName()))
            .toList();
    }

    public GroupDto getGroupForUser(Long groupId, User user) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nicht authentifiziert");
        }

        Group group = groupRepository.findByIdAndMembers_User_Id(groupId, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gruppe nicht gefunden"));

        return new GroupDto(
            group.getId(),
            group.getName(),
            group.getCreatedAt(),
            group.getCreatedByUserId()
        );
    }

    public List<GroupMemberDto> getGroupMembersForUser(Long groupId, User user) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nicht authentifiziert");
        }

        if (!groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Gruppe nicht gefunden");
        }

        return groupMemberRepository.findMemberDtosByGroupId(groupId);
    }

    @Transactional
    public void joinGroup(Long groupId, User user) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nicht authentifiziert");
        }

        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gruppe nicht gefunden"));

        if (groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, user.getId())) {
            return;
        }

        GroupMember membership = new GroupMember();
        membership.setGroup(group);
        membership.setUser(user);
        membership.setRole(GroupRole.MEMBER);

        try {
            groupMemberRepository.save(membership);
        } catch (DataIntegrityViolationException ex) {
            if (!groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, user.getId())) {
                throw ex;
            }
        }
    }
}
