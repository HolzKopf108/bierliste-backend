package com.bierliste.backend.service;

import com.bierliste.backend.model.Group;
import com.bierliste.backend.model.GroupMember;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.GroupMemberRepository;
import com.bierliste.backend.repository.GroupRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GroupAccessService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    public GroupAccessService(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    public Long requireAuthenticatedUserId(User user) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nicht authentifiziert");
        }
        return user.getId();
    }

    public Group requireGroupMembership(Long groupId, User user) {
        Long userId = requireAuthenticatedUserId(user);
        return groupRepository.findByIdAndMembers_User_Id(groupId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gruppe nicht gefunden"));
    }

    public GroupMember requireMembershipEntity(Long groupId, User user) {
        Long userId = requireAuthenticatedUserId(user);
        return groupMemberRepository.findByGroup_IdAndUser_Id(groupId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gruppe nicht gefunden"));
    }
}
