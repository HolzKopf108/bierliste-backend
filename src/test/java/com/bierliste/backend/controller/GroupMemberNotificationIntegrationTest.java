package com.bierliste.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bierliste.backend.model.Group;
import com.bierliste.backend.model.GroupMember;
import com.bierliste.backend.model.GroupMemberNotification;
import com.bierliste.backend.model.GroupRole;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.AndroidPushTokenRepository;
import com.bierliste.backend.repository.GroupMemberNotificationRepository;
import com.bierliste.backend.repository.GroupMemberRepository;
import com.bierliste.backend.repository.GroupRepository;
import com.bierliste.backend.repository.UserRepository;
import com.bierliste.backend.security.JwtTokenProvider;
import com.bierliste.backend.service.AndroidPushSender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GroupMemberNotificationIntegrationTest {

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
    private GroupMemberNotificationRepository groupMemberNotificationRepository;

    @Autowired
    private AndroidPushTokenRepository androidPushTokenRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AndroidPushSender androidPushSender;

    @BeforeEach
    void setUp() {
        when(androidPushSender.sendGroupMemberNotification(any(Group.class), any(GroupMemberNotification.class), anyList()))
            .thenReturn(new AndroidPushSender.PushDispatchResult(1, 1, List.of(), true));
    }

    @Test
    void sendNotificationFlowPersistsPendingNotificationAndExposesStatusInMembersOverview() throws Exception {
        User admin = createUser("notification-admin@example.com", "Admin");
        User member = createUser("notification-member@example.com", "Target");
        Group group = createGroup("Benachrichtigungsgruppe", admin);
        group.setPricePerStrich(new BigDecimal("2.50"));
        groupRepository.save(group);

        createMembership(group, admin, GroupRole.ADMIN, 0);
        createMembership(group, member, GroupRole.MEMBER, 3);

        String adminToken = jwtTokenProvider.createAccessToken(admin);
        String memberToken = jwtTokenProvider.createAccessToken(member);

        registerAndroidToken(memberToken, "member-device-token");

        MvcResult sendResult = mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members/" + member.getId() + "/notifications")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("message", "Bring heute 7,50€ mit"))))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.targetUserId").value(member.getId()))
            .andExpect(jsonPath("$.message").value("Bring heute 7,50€ mit"))
            .andExpect(jsonPath("$.confirmedAt").doesNotExist())
            .andReturn();

        Long notificationId = objectMapper.readTree(sendResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/groups/" + group.getId() + "/notifications/me")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(notificationId))
            .andExpect(jsonPath("$[0].message").value("Bring heute 7,50€ mit"))
            .andExpect(jsonPath("$[0].actorUsername").value("Admin"));

        mockMvc.perform(get("/api/v1/groups/" + group.getId() + "/members")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[1].userId").value(member.getId()))
            .andExpect(jsonPath("$[1].outstandingAmount").value(7.5))
            .andExpect(jsonPath("$[1].canReceiveNotification").value(true))
            .andExpect(jsonPath("$[1].hasPendingNotification").value(true))
            .andExpect(jsonPath("$[1].lastNotificationSentAt").isNotEmpty())
            .andExpect(jsonPath("$[1].lastNotificationConfirmedAt").doesNotExist());

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/notifications/" + notificationId + "/confirm")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(notificationId))
            .andExpect(jsonPath("$.confirmedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/groups/" + group.getId() + "/members")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[1].userId").value(member.getId()))
            .andExpect(jsonPath("$[1].canReceiveNotification").value(true))
            .andExpect(jsonPath("$[1].hasPendingNotification").value(false))
            .andExpect(jsonPath("$[1].lastNotificationSentAt").isNotEmpty())
            .andExpect(jsonPath("$[1].lastNotificationConfirmedAt").isNotEmpty());

        assertThat(groupMemberNotificationRepository.findAll()).hasSize(1);
        assertThat(groupMemberNotificationRepository.findAll().getFirst().getConfirmedAt()).isNotNull();
    }

    @Test
    void sendNotificationReturnsConflictWhenTargetHasNoRegisteredAndroidToken() throws Exception {
        User admin = createUser("notification-conflict-admin@example.com", "Admin");
        User member = createUser("notification-conflict-member@example.com", "Member");
        Group group = createGroup("Konfliktgruppe", admin);

        createMembership(group, admin, GroupRole.ADMIN, 0);
        createMembership(group, member, GroupRole.MEMBER, 2);

        String adminToken = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members/" + member.getId() + "/notifications")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("message", "Bring heute 2,00€ mit"))))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Benachrichtigungen fuer dieses Mitglied sind nicht verfuegbar"));

        assertThat(groupMemberNotificationRepository.findAll()).isEmpty();
    }

    @Test
    void sendNotificationRequiresAdminRole() throws Exception {
        User admin = createUser("notification-role-admin@example.com", "Admin");
        User caller = createUser("notification-role-member@example.com", "Caller");
        User target = createUser("notification-role-target@example.com", "Target");
        Group group = createGroup("Rollengruppe", admin);

        createMembership(group, admin, GroupRole.ADMIN, 0);
        createMembership(group, caller, GroupRole.MEMBER, 0);
        createMembership(group, target, GroupRole.MEMBER, 2);

        String callerToken = jwtTokenProvider.createAccessToken(caller);
        String targetToken = jwtTokenProvider.createAccessToken(target);

        registerAndroidToken(targetToken, "target-device-token");

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members/" + target.getId() + "/notifications")
                .header("Authorization", "Bearer " + callerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("message", "Bring heute 5,00€ mit"))))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Wart-Rechte erforderlich"));

        assertThat(groupMemberNotificationRepository.findAll()).isEmpty();
    }

    @Test
    void sendNotificationTriggersPushDispatchForAllRegisteredTokens() throws Exception {
        User admin = createUser("notification-dispatch-admin@example.com", "Admin");
        User target = createUser("notification-dispatch-target@example.com", "Target");
        Group group = createGroup("Dispatchgruppe", admin);

        createMembership(group, admin, GroupRole.ADMIN, 0);
        createMembership(group, target, GroupRole.MEMBER, 2);

        String adminToken = jwtTokenProvider.createAccessToken(admin);
        String targetToken = jwtTokenProvider.createAccessToken(target);

        registerAndroidToken(targetToken, "target-device-token-a");
        registerAndroidToken(targetToken, "target-device-token-b");

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members/" + target.getId() + "/notifications")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("message", "Bring heute 2,00€ mit"))))
            .andExpect(status().isCreated());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> tokensCaptor = ArgumentCaptor.forClass(List.class);
        verify(androidPushSender).sendGroupMemberNotification(any(Group.class), any(GroupMemberNotification.class), tokensCaptor.capture());
        assertThat(tokensCaptor.getValue()).containsExactlyInAnyOrder("target-device-token-a", "target-device-token-b");
    }

    @Test
    void sendNotificationCleansUpInvalidTokensAfterPartialDispatchFailures() throws Exception {
        User admin = createUser("notification-cleanup-admin@example.com", "Admin");
        User target = createUser("notification-cleanup-target@example.com", "Target");
        Group group = createGroup("Cleanupgruppe", admin);

        createMembership(group, admin, GroupRole.ADMIN, 0);
        createMembership(group, target, GroupRole.MEMBER, 2);

        String adminToken = jwtTokenProvider.createAccessToken(admin);
        String targetToken = jwtTokenProvider.createAccessToken(target);

        registerAndroidToken(targetToken, "cleanup-invalid-token");
        registerAndroidToken(targetToken, "cleanup-valid-token");

        when(androidPushSender.sendGroupMemberNotification(any(Group.class), any(GroupMemberNotification.class), anyList()))
            .thenReturn(new AndroidPushSender.PushDispatchResult(2, 1, List.of("cleanup-invalid-token"), true));

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members/" + target.getId() + "/notifications")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("message", "Bring heute 2,00€ mit"))))
            .andExpect(status().isCreated());

        assertThat(androidPushTokenRepository.findByToken("cleanup-invalid-token")).isEmpty();
        assertThat(androidPushTokenRepository.findByToken("cleanup-valid-token")).isPresent();
        assertThat(groupMemberNotificationRepository.findAll()).hasSize(1);
    }

    @Test
    void getGroupMembersHidesOtherUsersNotificationStatusForNonAdmins() throws Exception {
        User admin = createUser("notification-visibility-admin@example.com", "Admin");
        User caller = createUser("notification-visibility-caller@example.com", "Caller");
        User target = createUser("notification-visibility-target@example.com", "Target");
        Group group = createGroup("Sichtbarkeitsgruppe", admin);
        group.setPricePerStrich(new BigDecimal("2.50"));
        groupRepository.save(group);

        createMembership(group, admin, GroupRole.ADMIN, 0);
        createMembership(group, caller, GroupRole.MEMBER, 1);
        createMembership(group, target, GroupRole.MEMBER, 2);

        String adminToken = jwtTokenProvider.createAccessToken(admin);
        String callerToken = jwtTokenProvider.createAccessToken(caller);
        String targetToken = jwtTokenProvider.createAccessToken(target);

        registerAndroidToken(callerToken, "caller-device-token");
        registerAndroidToken(targetToken, "target-device-token");

        sendNotification(adminToken, group.getId(), caller.getId(), "Bring heute 2,50€ mit");
        sendNotification(adminToken, group.getId(), target.getId(), "Bring heute 5,00€ mit");

        MvcResult result = mockMvc.perform(get("/api/v1/groups/" + group.getId() + "/members")
                .header("Authorization", "Bearer " + callerToken))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andReturn();

        JsonNode members = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode callerNode = findMemberNode(members, caller.getId());
        JsonNode targetNode = findMemberNode(members, target.getId());

        assertThat(callerNode.get("canReceiveNotification").asBoolean()).isTrue();
        assertThat(callerNode.get("hasPendingNotification").asBoolean()).isTrue();
        assertThat(callerNode.hasNonNull("lastNotificationSentAt")).isTrue();

        assertThat(targetNode.get("canReceiveNotification").asBoolean()).isFalse();
        assertThat(targetNode.get("hasPendingNotification").asBoolean()).isFalse();
        assertThat(targetNode.get("lastNotificationSentAt").isNull()).isTrue();
        assertThat(targetNode.get("lastNotificationConfirmedAt").isNull()).isTrue();
    }

    private void registerAndroidToken(String accessToken, String token) throws Exception {
        mockMvc.perform(put("/api/v1/user/notifications/android")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", token))))
            .andExpect(status().isOk());
    }

    private void sendNotification(String accessToken, Long groupId, Long targetUserId, String message) throws Exception {
        mockMvc.perform(post("/api/v1/groups/" + groupId + "/members/" + targetUserId + "/notifications")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("message", message))))
            .andExpect(status().isCreated());
    }

    private JsonNode findMemberNode(JsonNode members, Long userId) {
        for (JsonNode member : members) {
            if (member.get("userId").asLong() == userId) {
                return member;
            }
        }
        throw new IllegalStateException("Mitglied nicht gefunden: " + userId);
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

    private GroupMember createMembership(Group group, User user, GroupRole role, int strichCount) {
        GroupMember groupMember = new GroupMember();
        groupMember.setGroup(group);
        groupMember.setUser(user);
        groupMember.setRole(role);
        groupMember.setStrichCount(strichCount);
        return groupMemberRepository.save(groupMember);
    }
}
