package com.bierliste.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bierliste.backend.model.ActivityType;
import com.bierliste.backend.model.Group;
import com.bierliste.backend.model.GroupActivity;
import com.bierliste.backend.model.GroupInvite;
import com.bierliste.backend.model.GroupInvitePermission;
import com.bierliste.backend.model.GroupMember;
import com.bierliste.backend.model.GroupRole;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.GroupActivityRepository;
import com.bierliste.backend.repository.GroupInviteRepository;
import com.bierliste.backend.repository.GroupMemberRepository;
import com.bierliste.backend.repository.GroupRepository;
import com.bierliste.backend.repository.UserRepository;
import com.bierliste.backend.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InviteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private GroupInviteRepository groupInviteRepository;

    @Autowired
    private GroupActivityRepository groupActivityRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void createInviteReturnsUnauthorizedWhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(post("/api/v1/groups/1/invites"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Nicht authentifiziert"));
    }

    @Test
    void joinInviteReturnsUnauthorizedWhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(post("/api/v1/invites/test-token/join"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Nicht authentifiziert"));
    }

    @Test
    void createInviteAsWartReturnsInviteDataAndPersistsSevenDayExpiry() throws Exception {
        User admin = createUser("invite-admin@example.com", "InviteAdmin");
        Group group = createGroup("Invite Gruppe", admin);
        createMembership(group, admin, GroupRole.ADMIN);

        String token = jwtTokenProvider.createAccessToken(admin);

        MvcResult result = mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/invites")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.inviteId").isNumber())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.joinUrl").value(org.hamcrest.Matchers.startsWith("bierliste://join?token=")))
            .andExpect(jsonPath("$.expiresAt").isNotEmpty())
            .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        String createdToken = response.get("token").asText();
        Long inviteId = response.get("inviteId").asLong();

        GroupInvite invite = groupInviteRepository.findById(inviteId).orElseThrow();
        assertThat(invite.getGroupId()).isEqualTo(group.getId());
        assertThat(invite.getCreatedByUserId()).isEqualTo(admin.getId());
        assertThat(invite.getToken()).isEqualTo(createdToken);
        assertThat(response.get("joinUrl").asText()).isEqualTo("bierliste://join?token=" + createdToken);
        assertThat(Duration.between(invite.getCreatedAt(), invite.getExpiresAt())).isEqualTo(Duration.ofDays(7));
    }

    @Test
    void createInviteAsMemberIsAllowedWhenAllMembersMayInvite() throws Exception {
        User admin = createUser("invite-all-admin@example.com", "Admin");
        User member = createUser("invite-all-member@example.com", "Member");
        Group group = createGroup("Offene Einladungen", admin);
        group.setInvitePermission(GroupInvitePermission.ALL_MEMBERS);
        groupRepository.save(group);

        createMembership(group, admin, GroupRole.ADMIN);
        createMembership(group, member, GroupRole.MEMBER);

        String token = jwtTokenProvider.createAccessToken(member);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/invites")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.inviteId").isNumber())
            .andExpect(jsonPath("$.joinUrl").value(org.hamcrest.Matchers.startsWith("bierliste://join?token=")));
    }

    @Test
    void createInviteAsMemberIsForbiddenWhenOnlyWartsMayInvite() throws Exception {
        User admin = createUser("invite-restricted-admin@example.com", "Admin");
        User member = createUser("invite-restricted-member@example.com", "Member");
        Group group = createGroup("Geschlossene Einladungen", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        createMembership(group, member, GroupRole.MEMBER);

        String token = jwtTokenProvider.createAccessToken(member);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/invites")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Wart-Rechte erforderlich"));
    }

    @Test
    void joinByValidInviteCreatesMembershipAndLogsActivity() throws Exception {
        User admin = createUser("join-invite-admin@example.com", "Admin");
        User joiner = createUser("join-invite-user@example.com", "Joiner");
        Group group = createGroup("Join per Invite", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        GroupInvite invite = createInvite(group, admin, "valid-invite-token", Instant.now().plus(Duration.ofDays(1)));

        String token = jwtTokenProvider.createAccessToken(joiner);

        mockMvc.perform(post("/api/v1/invites/" + invite.getToken() + "/join")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(group.getId()))
            .andExpect(jsonPath("$.name").value("Join per Invite"));

        GroupMember membership = groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), joiner.getId()).orElseThrow();
        assertThat(membership.getRole()).isEqualTo(GroupRole.MEMBER);

        List<GroupActivity> activities = groupActivityRepository.findAllByGroupIdOrderByTimestampDescIdDesc(group.getId());
        assertThat(activities).hasSize(1);
        assertThat(activities.getFirst().getType()).isEqualTo(ActivityType.USER_JOINED_GROUP);
        assertThat(activities.getFirst().getMeta()).containsEntry("via", "INVITE");
    }

    @Test
    void joinByValidInviteIsIdempotentForExistingMembership() throws Exception {
        User admin = createUser("join-idempotent-admin@example.com", "Admin");
        User joiner = createUser("join-idempotent-user@example.com", "Joiner");
        Group group = createGroup("Join Idempotent Invite", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        GroupInvite invite = createInvite(group, admin, "idempotent-token", Instant.now().plus(Duration.ofDays(1)));

        String token = jwtTokenProvider.createAccessToken(joiner);

        mockMvc.perform(post("/api/v1/invites/" + invite.getToken() + "/join")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(group.getId()));

        mockMvc.perform(post("/api/v1/invites/" + invite.getToken() + "/join")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(group.getId()));

        long memberCount = groupMemberRepository.findAllByGroup_Id(group.getId())
            .stream()
            .filter(member -> member.getUser().getId().equals(joiner.getId()))
            .count();

        List<GroupActivity> activities = groupActivityRepository.findAllByGroupIdOrderByTimestampDescIdDesc(group.getId());

        assertThat(memberCount).isEqualTo(1);
        assertThat(activities).hasSize(1);
    }

    @Test
    void joinByInvalidInviteReturnsNotFound() throws Exception {
        User user = createUser("join-invalid@example.com", "Invalid");
        String token = jwtTokenProvider.createAccessToken(user);

        mockMvc.perform(post("/api/v1/invites/does-not-exist/join")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Einladung nicht gefunden"));
    }

    @Test
    void joinByExpiredInviteReturnsGone() throws Exception {
        User admin = createUser("join-expired-admin@example.com", "Admin");
        User user = createUser("join-expired-user@example.com", "Expired");
        Group group = createGroup("Abgelaufene Einladung", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        GroupInvite invite = createInvite(group, admin, "expired-token", Instant.now().minusSeconds(60));

        String token = jwtTokenProvider.createAccessToken(user);

        mockMvc.perform(post("/api/v1/invites/" + invite.getToken() + "/join")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isGone())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Einladung abgelaufen"));
    }

    @Test
    void oldGroupJoinEndpointIsNotAvailableAnymore() throws Exception {
        User user = createUser("legacy-join@example.com", "Legacy");
        String token = jwtTokenProvider.createAccessToken(user);

        mockMvc.perform(post("/api/v1/groups/123/join")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Endpoint nicht gefunden"));
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

    private GroupInvite createInvite(Group group, User createdByUser, String token, Instant expiresAt) {
        GroupInvite invite = new GroupInvite();
        invite.setGroup(group);
        invite.setToken(token);
        invite.setCreatedByUserId(createdByUser.getId());
        invite.setCreatedAt(expiresAt.minus(Duration.ofDays(1)));
        invite.setExpiresAt(expiresAt);
        return groupInviteRepository.save(invite);
    }
}
