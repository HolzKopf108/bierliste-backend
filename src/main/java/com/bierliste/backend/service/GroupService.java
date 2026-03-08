package com.bierliste.backend.service;

import com.bierliste.backend.dto.CreateGroupDto;
import com.bierliste.backend.dto.GroupDto;
import com.bierliste.backend.model.Group;
import com.bierliste.backend.model.GroupMember;
import com.bierliste.backend.model.GroupRole;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.GroupMemberRepository;
import com.bierliste.backend.repository.GroupRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
}
