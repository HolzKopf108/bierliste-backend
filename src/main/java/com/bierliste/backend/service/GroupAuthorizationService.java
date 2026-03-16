package com.bierliste.backend.service;

import com.bierliste.backend.model.Group;
import com.bierliste.backend.model.GroupMember;
import com.bierliste.backend.model.GroupRole;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.GroupMemberRepository;
import com.bierliste.backend.repository.GroupRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GroupAuthorizationService {

    private static final String UNAUTHORIZED_MESSAGE = "Nicht authentifiziert";
    private static final String GROUP_NOT_FOUND_MESSAGE = "Gruppe nicht gefunden";
    private static final String WART_REQUIRED_MESSAGE = "Wart-Rechte erforderlich";

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    public GroupAuthorizationService(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    public Long requireAuthenticatedUserId(User user) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED_MESSAGE);
        }
        return user.getId();
    }

    public Group requireMemberGroup(Long groupId, User user) {
        Long userId = requireAuthenticatedUserId(user);
        return groupRepository.findByIdAndMembers_User_Id(groupId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, GROUP_NOT_FOUND_MESSAGE));
    }

    public void requireMember(Long groupId, User user) {
        Long userId = requireAuthenticatedUserId(user);
        requireMember(groupId, userId);
    }

    public void requireMember(Long groupId, Long userId) {
        if (!isMember(groupId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, GROUP_NOT_FOUND_MESSAGE);
        }
    }

    public GroupMember requireMemberEntity(Long groupId, User user) {
        Long userId = requireAuthenticatedUserId(user);
        return groupMemberRepository.findByGroup_IdAndUser_Id(groupId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, GROUP_NOT_FOUND_MESSAGE));
    }

    public void requireWart(Long groupId, User user) {
        GroupMember membership = requireMemberEntity(groupId, user);
        if (membership.getRole() != GroupRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, WART_REQUIRED_MESSAGE);
        }
    }

    public boolean isMember(Long groupId, User user) {
        return user != null && user.getId() != null && isMember(groupId, user.getId());
    }

    public boolean isMember(Long groupId, Long userId) {
        return groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, userId);
    }

    public boolean isWart(Long groupId, User user) {
        return user != null && user.getId() != null && isWart(groupId, user.getId());
    }

    public boolean isWart(Long groupId, Long userId) {
        return groupMemberRepository.existsByGroup_IdAndUser_IdAndRole(groupId, userId, GroupRole.ADMIN);
    }
}
