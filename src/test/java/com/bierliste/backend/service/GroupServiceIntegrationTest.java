package com.bierliste.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bierliste.backend.dto.CreateGroupDto;
import com.bierliste.backend.dto.GroupDto;
import com.bierliste.backend.model.Group;
import com.bierliste.backend.model.GroupMember;
import com.bierliste.backend.model.GroupRole;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.GroupMemberRepository;
import com.bierliste.backend.repository.GroupRepository;
import com.bierliste.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GroupServiceIntegrationTest {

    @Autowired
    private GroupService groupService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Test
    void createGroupCreatesGroupAndMembershipWithAdminRole() {
        User creator = new User();
        creator.setEmail("creator@example.com");
        creator.setUsername("creator");
        creator.setPasswordHash("hashed");
        creator = userRepository.save(creator);

        CreateGroupDto dto = new CreateGroupDto();
        dto.setName("  Neue Gruppe  ");

        GroupDto result = groupService.createGroup(dto, creator);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("Neue Gruppe");
        assertThat(result.getCreatedByUserId()).isEqualTo(creator.getId());

        Group persistedGroup = groupRepository.findById(result.getId()).orElseThrow();
        GroupMember persistedMember = groupMemberRepository
            .findByGroup_IdAndUser_Id(persistedGroup.getId(), creator.getId())
            .orElseThrow();

        assertThat(persistedGroup.getName()).isEqualTo("Neue Gruppe");
        assertThat(persistedMember.getRole()).isEqualTo(GroupRole.ADMIN);
        assertThat(persistedMember.getGroup().getId()).isEqualTo(persistedGroup.getId());
        assertThat(persistedMember.getUser().getId()).isEqualTo(creator.getId());
    }
}
