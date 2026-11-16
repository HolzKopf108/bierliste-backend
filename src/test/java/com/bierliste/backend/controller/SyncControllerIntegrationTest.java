package com.bierliste.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bierliste.backend.model.Group;
import com.bierliste.backend.model.GroupMember;
import com.bierliste.backend.model.GroupRole;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.CounterIncrementRequestRepository;
import com.bierliste.backend.repository.GroupActivityRepository;
import com.bierliste.backend.repository.GroupMemberRepository;
import com.bierliste.backend.repository.GroupRepository;
import com.bierliste.backend.repository.ProcessedClientEventRepository;
import com.bierliste.backend.repository.UserRepository;
import com.bierliste.backend.security.JwtTokenProvider;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SyncControllerIntegrationTest {

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
    private GroupActivityRepository groupActivityRepository;

    @Autowired
    private CounterIncrementRequestRepository counterIncrementRequestRepository;

    @Autowired
    private ProcessedClientEventRepository processedClientEventRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @AfterEach
    void cleanup() {
        processedClientEventRepository.deleteAll();
        counterIncrementRequestRepository.deleteAll();
        groupActivityRepository.deleteAll();
        groupMemberRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void syncEventsReturnsUnauthorizedWhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(post("/api/v1/sync/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("events", List.of()))))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Nicht authentifiziert"));
    }

    @Test
    void syncEventsAppliesIncrementEventsInRequestOrder() throws Exception {
        User user = createUser("sync-increment@example.com", "SyncIncrement");
        Group group = createGroup("Sync Gruppe", user);
        createMembership(group, user, GroupRole.ADMIN);

        UUID firstEventId = UUID.randomUUID();
        UUID secondEventId = UUID.randomUUID();
        String token = jwtTokenProvider.createAccessToken(user);

        mockMvc.perform(post("/api/v1/sync/events")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(syncRequest(List.of(
                    incrementSelfEvent(firstEventId, group.getId(), 2),
                    incrementSelfEvent(secondEventId, group.getId(), 3)
                )))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.results.length()").value(2))
            .andExpect(jsonPath("$.results[0].clientEventId").value(firstEventId.toString()))
            .andExpect(jsonPath("$.results[0].status").value("applied"))
            .andExpect(jsonPath("$.results[0].serverStateDelta.count").value(2))
            .andExpect(jsonPath("$.results[1].clientEventId").value(secondEventId.toString()))
            .andExpect(jsonPath("$.results[1].status").value("applied"))
            .andExpect(jsonPath("$.results[1].serverStateDelta.count").value(5));

        assertThat(groupMemberRepository.findActiveStrichCountByGroup_IdAndUser_Id(group.getId(), user.getId()))
            .contains(5);
        assertThat(processedClientEventRepository.countByUserId(user.getId())).isEqualTo(2);
        assertThat(processedClientEventRepository.findById(firstEventId)).isPresent();
        assertThat(processedClientEventRepository.findById(secondEventId)).isPresent();
        assertThat(counterIncrementRequestRepository.count()).isEqualTo(2);
    }

    @Test
    void syncEventsReturnsDuplicateForRepeatedClientEventIdWithoutIncrementingAgain() throws Exception {
        User user = createUser("sync-duplicate@example.com", "SyncDuplicate");
        Group group = createGroup("Duplicate Sync Gruppe", user);
        createMembership(group, user, GroupRole.ADMIN);

        UUID clientEventId = UUID.randomUUID();
        String token = jwtTokenProvider.createAccessToken(user);

        mockMvc.perform(post("/api/v1/sync/events")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(syncRequest(List.of(
                    incrementSelfEvent(clientEventId, group.getId(), 2),
                    incrementSelfEvent(clientEventId, group.getId(), 5)
                )))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.results.length()").value(2))
            .andExpect(jsonPath("$.results[0].status").value("applied"))
            .andExpect(jsonPath("$.results[0].serverStateDelta.count").value(2))
            .andExpect(jsonPath("$.results[1].status").value("duplicate"))
            .andExpect(jsonPath("$.results[1].serverStateDelta.count").value(2));

        assertThat(groupMemberRepository.findActiveStrichCountByGroup_IdAndUser_Id(group.getId(), user.getId()))
            .contains(2);
        assertThat(processedClientEventRepository.countByUserId(user.getId())).isEqualTo(1);
        assertThat(counterIncrementRequestRepository.count()).isEqualTo(1);
    }

    @Test
    void syncEventsAppliesValidEventAndReturnsFailedForInvalidEvent() throws Exception {
        User user = createUser("sync-partial@example.com", "SyncPartial");
        Group group = createGroup("Partial Sync Gruppe", user);
        createMembership(group, user, GroupRole.ADMIN);

        UUID validEventId = UUID.randomUUID();
        UUID invalidEventId = UUID.randomUUID();
        String token = jwtTokenProvider.createAccessToken(user);

        mockMvc.perform(post("/api/v1/sync/events")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(syncRequest(List.of(
                    incrementSelfEvent(validEventId, group.getId(), 2),
                    incrementSelfEvent(invalidEventId, group.getId(), 0)
                )))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.results.length()").value(2))
            .andExpect(jsonPath("$.results[0].status").value("applied"))
            .andExpect(jsonPath("$.results[0].serverStateDelta.count").value(2))
            .andExpect(jsonPath("$.results[1].clientEventId").value(invalidEventId.toString()))
            .andExpect(jsonPath("$.results[1].status").value("failed"))
            .andExpect(jsonPath("$.results[1].statusCode").value(400))
            .andExpect(jsonPath("$.results[1].resolution").value("drop"))
            .andExpect(jsonPath("$.results[1].error").value("amount muss mindestens 1 sein"));

        assertThat(groupMemberRepository.findActiveStrichCountByGroup_IdAndUser_Id(group.getId(), user.getId()))
            .contains(2);
        assertThat(processedClientEventRepository.countByUserId(user.getId())).isEqualTo(1);
        assertThat(processedClientEventRepository.findById(validEventId)).isPresent();
        assertThat(processedClientEventRepository.findById(invalidEventId)).isEmpty();
        assertThat(counterIncrementRequestRepository.count()).isEqualTo(1);
    }

    @Test
    void syncEventsReturnsFailedForNonMemberEvent() throws Exception {
        User admin = createUser("sync-non-member-admin@example.com", "SyncAdmin");
        User nonMember = createUser("sync-non-member@example.com", "SyncNonMember");
        Group group = createGroup("Private Sync Gruppe", admin);
        createMembership(group, admin, GroupRole.ADMIN);

        UUID clientEventId = UUID.randomUUID();
        String token = jwtTokenProvider.createAccessToken(nonMember);

        mockMvc.perform(post("/api/v1/sync/events")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(syncRequest(List.of(
                    incrementSelfEvent(clientEventId, group.getId(), 1)
                )))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.results.length()").value(1))
            .andExpect(jsonPath("$.results[0].clientEventId").value(clientEventId.toString()))
            .andExpect(jsonPath("$.results[0].status").value("failed"))
            .andExpect(jsonPath("$.results[0].statusCode").value(404))
            .andExpect(jsonPath("$.results[0].resolution").value("drop"))
            .andExpect(jsonPath("$.results[0].error").value("Gruppe nicht gefunden"));

        assertThat(groupMemberRepository.findActiveStrichCountByGroup_IdAndUser_Id(group.getId(), admin.getId()))
            .contains(0);
        assertThat(processedClientEventRepository.countByUserId(nonMember.getId())).isZero();
        assertThat(counterIncrementRequestRepository.count()).isZero();
    }

    private Map<String, Object> syncRequest(List<Map<String, Object>> events) {
        return Map.of("events", events);
    }

    private Map<String, Object> incrementSelfEvent(UUID clientEventId, Long groupId, int amount) {
        return Map.of(
            "clientEventId", clientEventId,
            "type", "INCREMENT_SELF",
            "groupId", groupId,
            "payload", Map.of("amount", amount),
            "clientTimestamp", Instant.now()
        );
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
