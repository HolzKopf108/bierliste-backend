package com.bierliste.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GroupAuthorizationServiceIntegrationTest {

    @Autowired
    private GroupAuthorizationService groupAuthorizationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Test
    void requireMemberReturnsUnauthorizedWhenUserIsMissing() {
        assertThatThrownBy(() -> groupAuthorizationService.requireMember(1L, (User) null))
            .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(ex.getReason()).isEqualTo("Nicht authentifiziert");
            });
    }

    @Test
    void requireMemberReturnsNotFoundForNonMember() {
        User member = createUser("member@example.com", "Member");
        User outsider = createUser("outsider@example.com", "Outsider");
        Group group = createGroup("Authorization", member);
        createMembership(group, member, GroupRole.ADMIN);

        assertThatThrownBy(() -> groupAuthorizationService.requireMember(group.getId(), outsider))
            .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(ex.getReason()).isEqualTo("Gruppe nicht gefunden");
            });
    }

    @Test
    void requireWartReturnsForbiddenForMemberWithoutAdminRole() {
        User admin = createUser("admin@example.com", "Admin");
        User member = createUser("plain-member@example.com", "PlainMember");
        Group group = createGroup("Wart Rechte", admin);
        createMembership(group, admin, GroupRole.ADMIN);
        createMembership(group, member, GroupRole.MEMBER);

        assertThatThrownBy(() -> groupAuthorizationService.requireWart(group.getId(), member))
            .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(ex.getReason()).isEqualTo("Wart-Rechte erforderlich");
            });
    }

    @Test
    void isMemberAndIsWartReflectPersistedMembershipRole() {
        User admin = createUser("role-admin@example.com", "RoleAdmin");
        User member = createUser("role-member@example.com", "RoleMember");
        User outsider = createUser("role-outsider@example.com", "RoleOutsider");
        Group group = createGroup("Role Check", admin);
        createMembership(group, admin, GroupRole.ADMIN);
        createMembership(group, member, GroupRole.MEMBER);

        assertThat(groupAuthorizationService.isMember(group.getId(), admin)).isTrue();
        assertThat(groupAuthorizationService.isMember(group.getId(), member)).isTrue();
        assertThat(groupAuthorizationService.isMember(group.getId(), outsider)).isFalse();
        assertThat(groupAuthorizationService.isWart(group.getId(), admin)).isTrue();
        assertThat(groupAuthorizationService.isWart(group.getId(), member)).isFalse();
        assertThat(groupAuthorizationService.isWart(group.getId(), outsider)).isFalse();
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
