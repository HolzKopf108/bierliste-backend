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

    @Test
    void leaveGroupDeletesGroupWhenLastMemberLeaves() {
        User creator = createUser("last-member@example.com", "last-member");
        Group group = createGroup("Leere Gruppe", creator);
        createMembership(group, creator, GroupRole.ADMIN);

        groupService.leaveGroup(group.getId(), creator);

        assertThat(groupRepository.findById(group.getId())).isEmpty();
        assertThat(groupMemberRepository.findAllByGroup_Id(group.getId())).isEmpty();
    }

    @Test
    void leaveGroupPromotesNextMemberWhenLastAdminLeaves() {
        User admin = createUser("admin@example.com", "admin");
        User firstMember = createUser("first@example.com", "first");
        User secondMember = createUser("second@example.com", "second");
        Group group = createGroup("Admin Wechsel", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        GroupMember promotedCandidate = createMembership(group, firstMember, GroupRole.MEMBER);
        createMembership(group, secondMember, GroupRole.MEMBER);

        groupService.leaveGroup(group.getId(), admin);

        assertThat(groupRepository.findById(group.getId())).isPresent();
        assertThat(groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), admin.getId())).isEmpty();
        GroupMember promotedMember = groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), firstMember.getId()).orElseThrow();
        GroupMember unchangedMember = groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), secondMember.getId()).orElseThrow();

        assertThat(promotedMember.getId()).isEqualTo(promotedCandidate.getId());
        assertThat(promotedMember.getRole()).isEqualTo(GroupRole.ADMIN);
        assertThat(unchangedMember.getRole()).isEqualTo(GroupRole.MEMBER);
    }

    private User createUser(String email, String username) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash("hashed");
        return userRepository.save(user);
    }

    private Group createGroup(String name, User createdByUser) {
        Group group = new Group();
        group.setName(name);
        group.setCreatedByUser(createdByUser);
        return groupRepository.save(group);
    }

    private GroupMember createMembership(Group group, User user, GroupRole role) {
        GroupMember groupMember = new GroupMember();
        groupMember.setGroup(group);
        groupMember.setUser(user);
        groupMember.setRole(role);
        return groupMemberRepository.save(groupMember);
    }
}
