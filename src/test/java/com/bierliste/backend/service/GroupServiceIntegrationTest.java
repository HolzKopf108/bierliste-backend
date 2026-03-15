package com.bierliste.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bierliste.backend.dto.CreateGroupDto;
import com.bierliste.backend.dto.GroupDto;
import com.bierliste.backend.dto.GroupMemberDto;
import com.bierliste.backend.model.Group;
import com.bierliste.backend.model.GroupMember;
import com.bierliste.backend.model.GroupRole;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.GroupMemberRepository;
import com.bierliste.backend.repository.GroupRepository;
import com.bierliste.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

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
        assertThat(persistedMember.getStrichCount()).isZero();
    }

    @Test
    void groupMemberRejectsNegativeStrichCount() {
        User user = createUser("invalid-counter@example.com", "invalid-counter");
        Group group = createGroup("Ungueltiger Counter", user);

        GroupMember invalidMembership = new GroupMember();
        invalidMembership.setGroup(group);
        invalidMembership.setUser(user);
        invalidMembership.setRole(GroupRole.ADMIN);
        invalidMembership.setStrichCount(-1);

        assertThatThrownBy(() -> groupMemberRepository.saveAndFlush(invalidMembership))
            .isInstanceOf(ConstraintViolationException.class);
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

    @Test
    void getGroupMembersForUserLoadsLargeMemberListWithoutNPlusOneQueries() {
        User requester = createUser("bulk-requester@example.com", "Requester");
        Group group = createGroup("Große Gruppe", requester);
        createMembership(group, requester, GroupRole.ADMIN);

        for (int i = 0; i < 120; i++) {
            User member = createUser("bulk-member-" + i + "@example.com", String.format("Member-%03d", i));
            GroupMember membership = createMembership(group, member, GroupRole.MEMBER);
            membership.setStrichCount(i);
        }

        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        List<GroupMemberDto> members = groupService.getGroupMembersForUser(group.getId(), requester);

        assertThat(members).hasSize(121);
        assertThat(members.getFirst().getUsername()).isEqualTo("Member-000");
        assertThat(members.getFirst().getStrichCount()).isZero();
        assertThat(members.getLast().getUsername()).isEqualTo("Requester");
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
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
