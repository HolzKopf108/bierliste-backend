package com.bierliste.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.bierliste.backend.service.ActivityService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
class GroupActivityIntegrationTest {

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
    private GroupInviteRepository groupInviteRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void getActivitiesReturnsUnauthorizedWhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(get("/api/v1/groups/1/activities"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Nicht authentifiziert"));
    }

    @Test
    void joinAndLeavePersistActivitiesWithoutDuplicateJoinEvent() throws Exception {
        User admin = createUser("activity-join-admin@example.com", "JoinAdmin");
        User member = createUser("activity-join-member@example.com", "JoinMember");
        Group group = createGroup("Join Verlauf", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        GroupInvite invite = createInvite(group, admin, Instant.now().plusSeconds(3600));

        String memberToken = jwtTokenProvider.createAccessToken(member);

        mockMvc.perform(post("/api/v1/invites/" + invite.getToken() + "/join")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/invites/" + invite.getToken() + "/join")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/leave")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk());

        List<GroupActivity> activities = groupActivityRepository.findAllByGroupIdOrderByTimestampDescIdDesc(group.getId());

        assertThat(activities).hasSize(2);
        assertThat(activities.get(0).getType()).isEqualTo(ActivityType.USER_LEFT_GROUP);
        assertThat(activities.get(1).getType()).isEqualTo(ActivityType.USER_JOINED_GROUP);
        assertThat(activities.get(0).getActorUserId()).isNull();
        assertThat(activities.get(0).getTargetUserId()).isNull();
        assertThat(activities.get(0).getActorDisplayNameSnapshot()).isEqualTo(ActivityService.FORMER_MEMBER_DISPLAY_NAME);
        assertThat(activities.get(0).getTargetDisplayNameSnapshot()).isEqualTo(ActivityService.FORMER_MEMBER_DISPLAY_NAME);
        assertThat(activities.get(1).getActorUserId()).isNull();
        assertThat(activities.get(1).getTargetUserId()).isNull();
        assertThat(activities.get(1).getActorDisplayNameSnapshot()).isEqualTo(ActivityService.FORMER_MEMBER_DISPLAY_NAME);
        assertThat(activities.get(1).getTargetDisplayNameSnapshot()).isEqualTo(ActivityService.FORMER_MEMBER_DISPLAY_NAME);
        assertThat(activities.get(1).getMeta()).containsEntry("via", "INVITE");
    }

    @Test
    void removeMemberPersistsRemovedActivityWithActorAndTarget() throws Exception {
        User admin = createUser("activity-remove-admin@example.com", "RemoveAdmin");
        User target = createUser("activity-remove-target@example.com", "RemoveTarget");
        Group group = createGroup("Remove Verlauf", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        createMembership(group, target, GroupRole.MEMBER);

        String adminToken = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(delete("/api/v1/groups/" + group.getId() + "/members/" + target.getId())
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());

        List<GroupActivity> activities = groupActivityRepository.findAllByGroupIdOrderByTimestampDescIdDesc(group.getId());

        assertThat(activities).hasSize(1);
        assertThat(activities.getFirst().getType()).isEqualTo(ActivityType.USER_REMOVED_FROM_GROUP);
        assertThat(activities.getFirst().getActorUserId()).isEqualTo(admin.getId());
        assertThat(activities.getFirst().getTargetUserId()).isNull();
        assertThat(activities.getFirst().getActorDisplayNameSnapshot()).isEqualTo("RemoveAdmin");
        assertThat(activities.getFirst().getTargetDisplayNameSnapshot()).isEqualTo(ActivityService.FORMER_MEMBER_DISPLAY_NAME);
    }

    @Test
    void leaveAnonymizesHistoricalActivitiesForLeavingUser() throws Exception {
        User admin = createUser("activity-leave-anon-admin@example.com", "LeaveAdmin");
        User member = createUser("activity-leave-anon-member@example.com", "LeaveMember");
        Group group = createGroup("Leave Anon Verlauf", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        createMembership(group, member, GroupRole.MEMBER);

        Instant fixedTimestamp = Instant.parse("2026-03-21T12:00:00Z");
        createActivity(group.getId(), ActivityType.USER_JOINED_GROUP, member, member, fixedTimestamp.minusSeconds(2), Map.of());
        createActivity(
            group.getId(),
            ActivityType.STRICH_INCREMENTED,
            admin,
            member,
            fixedTimestamp.minusSeconds(1),
            Map.of("amount", 2, "mode", "OTHER")
        );
        createActivity(
            group.getId(),
            ActivityType.STRICH_INCREMENTED,
            member,
            admin,
            fixedTimestamp,
            Map.of("amount", 1, "mode", "OTHER")
        );

        String memberToken = jwtTokenProvider.createAccessToken(member);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/leave")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk());

        List<GroupActivity> activities = groupActivityRepository.findAllByGroupIdOrderByTimestampDescIdDesc(group.getId());

        assertThat(activities).hasSize(4);

        GroupActivity leaveActivity = activities.stream()
            .filter(activity -> activity.getType() == ActivityType.USER_LEFT_GROUP)
            .findFirst()
            .orElseThrow();
        assertThat(leaveActivity.getActorUserId()).isNull();
        assertThat(leaveActivity.getTargetUserId()).isNull();
        assertThat(leaveActivity.getActorDisplayNameSnapshot()).isEqualTo(ActivityService.FORMER_MEMBER_DISPLAY_NAME);
        assertThat(leaveActivity.getTargetDisplayNameSnapshot()).isEqualTo(ActivityService.FORMER_MEMBER_DISPLAY_NAME);

        GroupActivity adminToMemberIncrement = activities.stream()
            .filter(activity -> activity.getType() == ActivityType.STRICH_INCREMENTED)
            .filter(activity -> activity.getActorDisplayNameSnapshot().equals("LeaveAdmin"))
            .findFirst()
            .orElseThrow();
        assertThat(adminToMemberIncrement.getActorUserId()).isEqualTo(admin.getId());
        assertThat(adminToMemberIncrement.getTargetUserId()).isNull();
        assertThat(adminToMemberIncrement.getTargetDisplayNameSnapshot()).isEqualTo(ActivityService.FORMER_MEMBER_DISPLAY_NAME);

        GroupActivity memberToAdminIncrement = activities.stream()
            .filter(activity -> activity.getType() == ActivityType.STRICH_INCREMENTED)
            .filter(activity -> activity.getTargetDisplayNameSnapshot().equals("LeaveAdmin"))
            .findFirst()
            .orElseThrow();
        assertThat(memberToAdminIncrement.getActorUserId()).isNull();
        assertThat(memberToAdminIncrement.getActorDisplayNameSnapshot()).isEqualTo(ActivityService.FORMER_MEMBER_DISPLAY_NAME);
        assertThat(memberToAdminIncrement.getTargetUserId()).isEqualTo(admin.getId());

        GroupActivity joinActivity = activities.stream()
            .filter(activity -> activity.getType() == ActivityType.USER_JOINED_GROUP)
            .findFirst()
            .orElseThrow();
        assertThat(joinActivity.getActorUserId()).isNull();
        assertThat(joinActivity.getTargetUserId()).isNull();
        assertThat(joinActivity.getActorDisplayNameSnapshot()).isEqualTo(ActivityService.FORMER_MEMBER_DISPLAY_NAME);
        assertThat(joinActivity.getTargetDisplayNameSnapshot()).isEqualTo(ActivityService.FORMER_MEMBER_DISPLAY_NAME);
    }

    @Test
    void removeAnonymizesHistoricalActivitiesForRemovedUser() throws Exception {
        User admin = createUser("activity-remove-anon-admin@example.com", "RemoveAnonAdmin");
        User target = createUser("activity-remove-anon-target@example.com", "RemoveAnonTarget");
        Group group = createGroup("Remove Anon Verlauf", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        createMembership(group, target, GroupRole.MEMBER);

        Instant fixedTimestamp = Instant.parse("2026-03-22T12:00:00Z");
        createActivity(group.getId(), ActivityType.USER_JOINED_GROUP, target, target, fixedTimestamp.minusSeconds(2), Map.of());
        createActivity(
            group.getId(),
            ActivityType.STRICH_INCREMENTED,
            admin,
            target,
            fixedTimestamp.minusSeconds(1),
            Map.of("amount", 2, "mode", "OTHER")
        );
        createActivity(
            group.getId(),
            ActivityType.STRICH_INCREMENTED,
            target,
            admin,
            fixedTimestamp,
            Map.of("amount", 1, "mode", "OTHER")
        );

        String adminToken = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(delete("/api/v1/groups/" + group.getId() + "/members/" + target.getId())
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());

        List<GroupActivity> activities = groupActivityRepository.findAllByGroupIdOrderByTimestampDescIdDesc(group.getId());

        assertThat(activities).hasSize(4);

        GroupActivity removedActivity = activities.stream()
            .filter(activity -> activity.getType() == ActivityType.USER_REMOVED_FROM_GROUP)
            .findFirst()
            .orElseThrow();
        assertThat(removedActivity.getActorUserId()).isEqualTo(admin.getId());
        assertThat(removedActivity.getActorDisplayNameSnapshot()).isEqualTo("RemoveAnonAdmin");
        assertThat(removedActivity.getTargetUserId()).isNull();
        assertThat(removedActivity.getTargetDisplayNameSnapshot()).isEqualTo(ActivityService.FORMER_MEMBER_DISPLAY_NAME);

        GroupActivity adminToTargetIncrement = activities.stream()
            .filter(activity -> activity.getType() == ActivityType.STRICH_INCREMENTED)
            .filter(activity -> activity.getActorDisplayNameSnapshot().equals("RemoveAnonAdmin"))
            .findFirst()
            .orElseThrow();
        assertThat(adminToTargetIncrement.getActorUserId()).isEqualTo(admin.getId());
        assertThat(adminToTargetIncrement.getTargetUserId()).isNull();
        assertThat(adminToTargetIncrement.getTargetDisplayNameSnapshot()).isEqualTo(ActivityService.FORMER_MEMBER_DISPLAY_NAME);

        GroupActivity targetToAdminIncrement = activities.stream()
            .filter(activity -> activity.getType() == ActivityType.STRICH_INCREMENTED)
            .filter(activity -> activity.getTargetDisplayNameSnapshot().equals("RemoveAnonAdmin"))
            .findFirst()
            .orElseThrow();
        assertThat(targetToAdminIncrement.getActorUserId()).isNull();
        assertThat(targetToAdminIncrement.getActorDisplayNameSnapshot()).isEqualTo(ActivityService.FORMER_MEMBER_DISPLAY_NAME);
        assertThat(targetToAdminIncrement.getTargetUserId()).isEqualTo(admin.getId());

        GroupActivity joinActivity = activities.stream()
            .filter(activity -> activity.getType() == ActivityType.USER_JOINED_GROUP)
            .findFirst()
            .orElseThrow();
        assertThat(joinActivity.getActorUserId()).isNull();
        assertThat(joinActivity.getTargetUserId()).isNull();
        assertThat(joinActivity.getActorDisplayNameSnapshot()).isEqualTo(ActivityService.FORMER_MEMBER_DISPLAY_NAME);
        assertThat(joinActivity.getTargetDisplayNameSnapshot()).isEqualTo(ActivityService.FORMER_MEMBER_DISPLAY_NAME);
    }

    @Test
    void incrementHooksPersistStrichActivitiesForSelfAndOther() throws Exception {
        User admin = createUser("activity-increment-admin@example.com", "IncrementAdmin");
        User member = createUser("activity-increment-member@example.com", "IncrementMember");
        Group group = createGroup("Increment Verlauf", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        createMembership(group, member, GroupRole.MEMBER);

        String memberToken = jwtTokenProvider.createAccessToken(member);
        String adminToken = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/me/counter/increment")
                .header("Authorization", "Bearer " + memberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 2))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(2));

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members/" + member.getId() + "/counter/increment")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 3))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(5));

        List<GroupActivity> activities = groupActivityRepository.findAllByGroupIdOrderByTimestampDescIdDesc(group.getId());

        assertThat(activities).hasSize(2);
        assertThat(activities.get(0).getType()).isEqualTo(ActivityType.STRICH_INCREMENTED);
        assertThat(activities.get(0).getActorUserId()).isEqualTo(admin.getId());
        assertThat(activities.get(0).getTargetUserId()).isEqualTo(member.getId());
        assertThat(activities.get(0).getMeta()).containsEntry("amount", 3).containsEntry("mode", "OTHER");
        assertThat(activities.get(1).getType()).isEqualTo(ActivityType.STRICH_INCREMENTED);
        assertThat(activities.get(1).getActorUserId()).isEqualTo(member.getId());
        assertThat(activities.get(1).getTargetUserId()).isEqualTo(member.getId());
        assertThat(activities.get(1).getMeta()).containsEntry("amount", 2).containsEntry("mode", "SELF");
    }

    @Test
    void undoCounterIncrementPersistsSeparateActivityAndExposesItInActivitiesEndpoint() throws Exception {
        User member = createUser("activity-undo-member@example.com", "UndoMember");
        Group group = createGroup("Undo Verlauf", member);

        createMembership(group, member, GroupRole.ADMIN);

        String token = jwtTokenProvider.createAccessToken(member);

        MvcResult incrementResult = mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/me/counter/increment")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 2))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(2))
            .andReturn();

        long incrementRequestId = objectMapper.readTree(incrementResult.getResponse().getContentAsString())
            .get("incrementRequestId")
            .asLong();

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/counter/increments/" + incrementRequestId + "/undo")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(0))
            .andExpect(jsonPath("$.incrementRequestId").value(incrementRequestId))
            .andExpect(jsonPath("$.undoneAt").isNotEmpty());

        List<GroupActivity> activities = groupActivityRepository.findAllByGroupIdOrderByTimestampDescIdDesc(group.getId());

        assertThat(activities).hasSize(2);
        assertThat(activities.get(0).getType()).isEqualTo(ActivityType.STRICH_INCREMENT_UNDONE);
        assertThat(activities.get(0).getActorUserId()).isEqualTo(member.getId());
        assertThat(activities.get(0).getTargetUserId()).isEqualTo(member.getId());
        assertThat(activities.get(0).getMeta())
            .containsEntry("amount", 2)
            .containsEntry("mode", "SELF")
            .containsEntry("incrementRequestId", incrementRequestId)
            .containsEntry("originalActivityId", activities.get(1).getId());
        assertThat(activities.get(1).getType()).isEqualTo(ActivityType.STRICH_INCREMENTED);

        mockMvc.perform(get("/api/v1/groups/" + group.getId() + "/activities")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.items[0].type").value("STRICH_INCREMENT_UNDONE"))
            .andExpect(jsonPath("$.items[0].actor.userId").value(member.getId()))
            .andExpect(jsonPath("$.items[0].target.userId").value(member.getId()))
            .andExpect(jsonPath("$.items[0].amount").value(2))
            .andExpect(jsonPath("$.items[0].mode").value("SELF"))
            .andExpect(jsonPath("$.items[0].incrementRequestId").value(incrementRequestId))
            .andExpect(jsonPath("$.items[0].originalActivityId").value(activities.get(1).getId()))
            .andExpect(jsonPath("$.items[1].type").value("STRICH_INCREMENTED"))
            .andExpect(jsonPath("$.items[1].amount").value(2))
            .andExpect(jsonPath("$.items[1].mode").value("SELF"));
    }

    @Test
    void settlementHooksPersistMoneyAndStricheActivities() throws Exception {
        User admin = createUser("activity-settlement-admin@example.com", "SettlementAdmin");
        User target = createUser("activity-settlement-target@example.com", "SettlementTarget");
        Group group = createGroup("Settlement Verlauf", admin);
        group.setPricePerStrich(new BigDecimal("1.50"));
        groupRepository.save(group);

        createMembership(group, admin, GroupRole.ADMIN);
        GroupMember targetMembership = createMembership(group, target, GroupRole.MEMBER);
        targetMembership.setStrichCount(7);
        groupMemberRepository.save(targetMembership);

        String adminToken = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members/" + target.getId() + "/settlements/money")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 3.00))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.strichCount").value(5));

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/members/" + target.getId() + "/settlements/striche")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 4))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.strichCount").value(1));

        List<GroupActivity> activities = groupActivityRepository.findAllByGroupIdOrderByTimestampDescIdDesc(group.getId());

        assertThat(activities).hasSize(2);
        assertThat(activities.get(0).getType()).isEqualTo(ActivityType.STRICHE_DEDUCTED);
        assertThat(activities.get(0).getActorUserId()).isEqualTo(admin.getId());
        assertThat(activities.get(0).getTargetUserId()).isEqualTo(target.getId());
        assertThat(activities.get(0).getMeta()).containsEntry("amountStriche", 4);
        assertThat(activities.get(1).getType()).isEqualTo(ActivityType.MONEY_DEDUCTED);
        assertThat(activities.get(1).getActorUserId()).isEqualTo(admin.getId());
        assertThat(activities.get(1).getTargetUserId()).isEqualTo(target.getId());
        assertThat((BigDecimal) activities.get(1).getMeta().get("amountMoney")).isEqualByComparingTo("3.00");
        assertThat((BigDecimal) activities.get(1).getMeta().get("pricePerStrich")).isEqualByComparingTo("1.50");
    }

    @Test
    void roleAndSettingsHooksPersistActivities() throws Exception {
        User admin = createUser("activity-role-admin@example.com", "RoleAdmin");
        User target = createUser("activity-role-target@example.com", "RoleTarget");
        Group group = createGroup("Role Verlauf", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        createMembership(group, target, GroupRole.MEMBER);

        String adminToken = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/roles/promote")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("targetUserId", target.getId()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/roles/demote")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("targetUserId", target.getId()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("MEMBER"));

        mockMvc.perform(put("/api/v1/groups/" + group.getId() + "/settings")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "name", "Role Verlauf Neu",
                    "pricePerStrich", 2.75,
                    "onlyWartsCanBookForOthers", false,
                    "allowArbitraryMoneySettlements", true,
                    "invitePermission", "ALL_MEMBERS"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Role Verlauf Neu"));

        List<GroupActivity> activities = groupActivityRepository.findAllByGroupIdOrderByTimestampDescIdDesc(group.getId());

        assertThat(activities).hasSize(3);
        assertThat(activities.get(0).getType()).isEqualTo(ActivityType.GROUP_SETTINGS_CHANGED);
        assertThat(activities.get(0).getActorUserId()).isEqualTo(admin.getId());
        assertThat(activities.get(0).getTargetUserId()).isNull();
        assertThat(readStringList(activities.get(0), "changedFields"))
            .containsExactly("name", "pricePerStrich", "onlyWartsCanBookForOthers", "allowArbitraryMoneySettlements", "invitePermission");
        assertThat(readMetaMap(activities.get(0), "old")).containsEntry("name", "Role Verlauf");
        assertThat(readMetaMap(activities.get(0), "new")).containsEntry("name", "Role Verlauf Neu");
        assertThat(readMetaMap(activities.get(0), "new")).containsEntry("invitePermission", GroupInvitePermission.ALL_MEMBERS);
        assertThat(activities.get(1).getType()).isEqualTo(ActivityType.ROLE_REVOKED_WART);
        assertThat(activities.get(1).getActorUserId()).isEqualTo(admin.getId());
        assertThat(activities.get(1).getTargetUserId()).isEqualTo(target.getId());
        assertThat(activities.get(1).getMeta()).containsEntry("previousRole", "ADMIN").containsEntry("newRole", "MEMBER");
        assertThat(activities.get(2).getType()).isEqualTo(ActivityType.ROLE_GRANTED_WART);
        assertThat(activities.get(2).getActorUserId()).isEqualTo(admin.getId());
        assertThat(activities.get(2).getTargetUserId()).isEqualTo(target.getId());
        assertThat(activities.get(2).getMeta()).containsEntry("previousRole", "MEMBER").containsEntry("newRole", "ADMIN");
    }

    @Test
    void activitiesEndpointReturnsPaginatedActivitiesWithStableOrderingAndNoEmails() throws Exception {
        User admin = createUser("activity-endpoint-admin@example.com", "EndpointAdmin");
        User target = createUser("activity-endpoint-target@example.com", "EndpointTarget");
        Group group = createGroup("Endpoint Verlauf", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        createMembership(group, target, GroupRole.MEMBER);

        Instant fixedTimestamp = Instant.parse("2026-03-21T12:00:00Z");
        GroupActivity first = createActivity(group.getId(), ActivityType.USER_JOINED_GROUP, admin, target, fixedTimestamp, Map.of());
        GroupActivity second = createActivity(group.getId(), ActivityType.STRICH_INCREMENTED, target, target, fixedTimestamp, Map.of("amount", 1, "mode", "SELF"));
        GroupActivity third = createActivity(
            group.getId(),
            ActivityType.GROUP_SETTINGS_CHANGED,
            admin,
            null,
            fixedTimestamp,
            Map.of(
                "changedFields", List.of("name"),
                "old", Map.of(
                    "name", "Endpoint Verlauf",
                    "pricePerStrich", new BigDecimal("1.00"),
                    "onlyWartsCanBookForOthers", true,
                    "allowArbitraryMoneySettlements", false,
                    "invitePermission", GroupInvitePermission.ONLY_WARTS.name()
                ),
                "new", Map.of(
                    "name", "Endpoint Verlauf Neu",
                    "pricePerStrich", new BigDecimal("1.00"),
                    "onlyWartsCanBookForOthers", true,
                    "allowArbitraryMoneySettlements", false,
                    "invitePermission", GroupInvitePermission.ONLY_WARTS.name()
                )
            )
        );

        String adminToken = jwtTokenProvider.createAccessToken(admin);

        MvcResult firstPageResult = mockMvc.perform(get("/api/v1/groups/" + group.getId() + "/activities")
                .header("Authorization", "Bearer " + adminToken)
                .param("limit", "2"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[0].id").value(third.getId()))
            .andExpect(jsonPath("$.items[0].type").value("GROUP_SETTINGS_CHANGED"))
            .andExpect(jsonPath("$.items[0].actor.userId").value(admin.getId()))
            .andExpect(jsonPath("$.items[0].actor.displayNameSnapshot").value("EndpointAdmin"))
            .andExpect(jsonPath("$.items[0].actor.email").doesNotExist())
            .andExpect(jsonPath("$.items[0].meta").doesNotExist())
            .andExpect(jsonPath("$.items[0].changedFields[0]").value("NAME"))
            .andExpect(jsonPath("$.items[0].oldSettings.name").value("Endpoint Verlauf"))
            .andExpect(jsonPath("$.items[0].newSettings.name").value("Endpoint Verlauf Neu"))
            .andExpect(jsonPath("$.items[0].newSettings.invitePermission").value("ONLY_WARTS"))
            .andExpect(jsonPath("$.items[1].id").value(second.getId()))
            .andExpect(jsonPath("$.items[1].type").value("STRICH_INCREMENTED"))
            .andExpect(jsonPath("$.items[1].target.userId").value(target.getId()))
            .andExpect(jsonPath("$.items[1].target.displayNameSnapshot").value("EndpointTarget"))
            .andExpect(jsonPath("$.items[1].target.email").doesNotExist())
            .andExpect(jsonPath("$.items[1].meta").doesNotExist())
            .andExpect(jsonPath("$.items[1].amount").value(1))
            .andExpect(jsonPath("$.items[1].mode").value("SELF"))
            .andReturn();

        JsonNode firstPageJson = objectMapper.readTree(firstPageResult.getResponse().getContentAsString());
        String nextCursor = firstPageJson.get("nextCursor").asText();

        assertThat(nextCursor).isNotBlank();

        MvcResult secondPageResult = mockMvc.perform(get("/api/v1/groups/" + group.getId() + "/activities")
                .header("Authorization", "Bearer " + adminToken)
                .param("limit", "2")
                .param("cursor", nextCursor))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].id").value(first.getId()))
            .andExpect(jsonPath("$.items[0].type").value("USER_JOINED_GROUP"))
            .andExpect(jsonPath("$.items[0].actor.userId").value(admin.getId()))
            .andExpect(jsonPath("$.items[0].target.userId").value(target.getId()))
            .andExpect(jsonPath("$.items[0].meta").doesNotExist())
            .andReturn();

        JsonNode secondPageJson = objectMapper.readTree(secondPageResult.getResponse().getContentAsString());
        assertThat(secondPageJson.get("nextCursor").isNull()).isTrue();
    }

    @Test
    void getActivitiesReturnsNotFoundForNonMember() throws Exception {
        User admin = createUser("activity-non-member-admin@example.com", "ActivityAdmin");
        User outsider = createUser("activity-non-member-outsider@example.com", "ActivityOutsider");
        Group group = createGroup("Privater Verlauf", admin);

        createMembership(group, admin, GroupRole.ADMIN);

        String outsiderToken = jwtTokenProvider.createAccessToken(outsider);

        mockMvc.perform(get("/api/v1/groups/" + group.getId() + "/activities")
                .header("Authorization", "Bearer " + outsiderToken))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Gruppe nicht gefunden"));
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

    private GroupInvite createInvite(Group group, User createdByUser, Instant expiresAt) {
        GroupInvite invite = new GroupInvite();
        invite.setGroup(group);
        invite.setToken("invite-token-" + createdByUser.getId() + "-" + System.nanoTime());
        invite.setCreatedByUserId(createdByUser.getId());
        invite.setExpiresAt(expiresAt);
        return groupInviteRepository.save(invite);
    }

    private GroupActivity createActivity(
        Long groupId,
        ActivityType type,
        User actor,
        User target,
        Instant timestamp,
        Map<String, Object> meta
    ) {
        GroupActivity activity = new GroupActivity();
        activity.setGroupId(groupId);
        activity.setTimestamp(timestamp);
        activity.setActorUserId(actor.getId());
        activity.setActorDisplayNameSnapshot(actor.getUsername());
        activity.setTargetUserId(target != null ? target.getId() : null);
        activity.setTargetDisplayNameSnapshot(target != null ? target.getUsername() : null);
        activity.setType(type);
        activity.setMeta(meta);
        return groupActivityRepository.save(activity);
    }

    @SuppressWarnings("unchecked")
    private List<String> readStringList(GroupActivity activity, String key) {
        return (List<String>) activity.getMeta().get(key);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMetaMap(GroupActivity activity, String key) {
        return (Map<String, Object>) activity.getMeta().get(key);
    }
}
