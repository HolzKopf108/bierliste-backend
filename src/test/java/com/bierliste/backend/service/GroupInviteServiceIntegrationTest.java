package com.bierliste.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bierliste.backend.dto.GroupInviteResponseDto;
import com.bierliste.backend.model.Group;
import com.bierliste.backend.model.GroupInvite;
import com.bierliste.backend.model.GroupMember;
import com.bierliste.backend.model.GroupRole;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.GroupInviteRepository;
import com.bierliste.backend.repository.GroupMemberRepository;
import com.bierliste.backend.repository.GroupRepository;
import com.bierliste.backend.repository.UserRepository;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GroupInviteServiceIntegrationTest {

    @Autowired
    private GroupInviteService groupInviteService;

    @Autowired
    private GroupInviteTokenService groupInviteTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private GroupInviteRepository groupInviteRepository;

    @Test
    void createInviteStoresExpiresAtExactlySevenDaysAfterCreatedAt() {
        User admin = createUser("service-invite-admin@example.com", "Admin");
        Group group = createGroup("Service Invite", admin);
        createMembership(group, admin, GroupRole.ADMIN);

        GroupInviteResponseDto response = groupInviteService.createInvite(group.getId(), admin);
        GroupInvite invite = groupInviteRepository.findById(response.getInviteId()).orElseThrow();

        assertThat(invite.getCreatedByUserId()).isEqualTo(admin.getId());
        assertThat(Duration.between(invite.getCreatedAt(), invite.getExpiresAt())).isEqualTo(Duration.ofDays(7));
        assertThat(response.getJoinUrl()).isEqualTo("bierliste://join?token=" + response.getToken());
    }

    @Test
    void generatedTokensAreLongUrlSafeAndUnique() {
        Set<String> tokens = new HashSet<>();

        for (int i = 0; i < 200; i++) {
            String token = groupInviteTokenService.generateToken();
            assertThat(token).matches("^[A-Za-z0-9_-]+$");
            assertThat(token.length()).isGreaterThanOrEqualTo(43);
            tokens.add(token);
        }

        assertThat(tokens).hasSize(200);
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
