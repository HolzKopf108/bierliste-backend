package com.bierliste.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bierliste.backend.model.Group;
import com.bierliste.backend.model.GroupMember;
import com.bierliste.backend.model.GroupRole;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.GroupMemberRepository;
import com.bierliste.backend.repository.GroupRepository;
import com.bierliste.backend.repository.UserRepository;
import com.bierliste.backend.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GroupControllerIntegrationTest {

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
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void createGroupReturnsUnauthorizedWhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(post("/api/v1/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Feierabend"))))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Nicht authentifiziert"));
    }

    @Test
    void getGroupsReturnsUnauthorizedWhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(get("/api/v1/groups"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Nicht authentifiziert"));
    }

    @Test
    void getGroupByIdReturnsUnauthorizedWhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(get("/api/v1/groups/1"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Nicht authentifiziert"));
    }

    @Test
    void getGroupSettingsReturnsUnauthorizedWhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(get("/api/v1/groups/1/settings"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Nicht authentifiziert"));
    }

    @Test
    void getGroupMembersReturnsUnauthorizedWhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(get("/api/v1/groups/1/members"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Nicht authentifiziert"));
    }

    @Test
    void getOwnCounterReturnsUnauthorizedWhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(get("/api/v1/groups/1/me/counter"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Nicht authentifiziert"));
    }

    @Test
    void getOwnRoleReturnsUnauthorizedWhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(get("/api/v1/groups/1/me/role"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Nicht authentifiziert"));
    }

    @Test
    void incrementOwnCounterReturnsUnauthorizedWhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(post("/api/v1/groups/1/me/counter/increment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 1))))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Nicht authentifiziert"));
    }

    @Test
    void joinGroupReturnsUnauthorizedWhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(post("/api/v1/groups/1/join"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Nicht authentifiziert"));
    }

    @Test
    void leaveGroupReturnsUnauthorizedWhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(post("/api/v1/groups/1/leave"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Nicht authentifiziert"));
    }

    @Test
    void promoteGroupMemberReturnsUnauthorizedWhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(post("/api/v1/groups/1/roles/promote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("targetUserId", 1))))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Nicht authentifiziert"));
    }

    @Test
    void demoteGroupMemberReturnsUnauthorizedWhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(post("/api/v1/groups/1/roles/demote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("targetUserId", 1))))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Nicht authentifiziert"));
    }

    @Test
    void updateGroupSettingsReturnsUnauthorizedWhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(put("/api/v1/groups/1/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "pricePerStrich", 2.50,
                    "onlyWartsCanBookForOthers", false
                ))))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Nicht authentifiziert"));
    }

    @Test
    void getGroupsReturnsOnlyGroupsForAuthenticatedUser() throws Exception {
        User user = createUser("groups-user@example.com");
        User secondUser = createUser("groups-second@example.com");
        User thirdUser = createUser("groups-third@example.com");

        Group alphaGroup = createGroup("Alpha", user);
        Group betaGroup = createGroup("Beta", secondUser);
        Group gammaGroup = createGroup("Gamma", thirdUser);

        createMembership(alphaGroup, user, GroupRole.ADMIN);
        createMembership(betaGroup, user, GroupRole.MEMBER);
        createMembership(betaGroup, secondUser, GroupRole.ADMIN);
        createMembership(gammaGroup, thirdUser, GroupRole.ADMIN);

        String token = jwtTokenProvider.createAccessToken(user);

        mockMvc.perform(get("/api/v1/groups")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("Alpha"))
            .andExpect(jsonPath("$[0].memberCount").doesNotExist())
            .andExpect(jsonPath("$[1].name").value("Beta"))
            .andExpect(jsonPath("$[1].memberCount").doesNotExist());
    }

    @Test
    void getGroupByIdReturnsGroupDetailsForMember() throws Exception {
        User member = createUser("group-member@example.com");
        User creator = createUser("group-creator-details@example.com");
        Group group = createGroup("Details Gruppe", creator);

        createMembership(group, member, GroupRole.MEMBER);

        String token = jwtTokenProvider.createAccessToken(member);

        mockMvc.perform(get("/api/v1/groups/" + group.getId())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(group.getId()))
            .andExpect(jsonPath("$.name").value("Details Gruppe"))
            .andExpect(jsonPath("$.createdByUserId").value(creator.getId()))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.pricePerStrich").value(1))
            .andExpect(jsonPath("$.onlyWartsCanBookForOthers").value(true));
    }

    @Test
    void getGroupSettingsReturnsPersistedSettingsForMember() throws Exception {
        User admin = createUser("group-settings-admin@example.com");
        User member = createUser("group-settings-member@example.com");
        Group group = createGroup("Settings Gruppe", admin);
        group.setPricePerStrich(new BigDecimal("2.50"));
        group.setOnlyWartsCanBookForOthers(false);
        groupRepository.save(group);

        createMembership(group, admin, GroupRole.ADMIN);
        createMembership(group, member, GroupRole.MEMBER);

        String token = jwtTokenProvider.createAccessToken(member);

        mockMvc.perform(get("/api/v1/groups/" + group.getId() + "/settings")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.pricePerStrich").value(2.5))
            .andExpect(jsonPath("$.onlyWartsCanBookForOthers").value(false));
    }

    @Test
    void getGroupByIdReturnsNotFoundForNonMember() throws Exception {
        User member = createUser("group-member-only@example.com");
        User nonMember = createUser("group-non-member@example.com");
        Group group = createGroup("Private Gruppe", member);

        createMembership(group, member, GroupRole.ADMIN);

        String token = jwtTokenProvider.createAccessToken(nonMember);

        mockMvc.perform(get("/api/v1/groups/" + group.getId())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Gruppe nicht gefunden"));
    }

    @Test
    void getGroupMembersReturnsSortedMembersForGroupMember() throws Exception {
        User requester = createUser("requester@example.com", "Mona");
        User anna = createUser("anna@example.com", "Anna");
        User zora = createUser("zora@example.com", "Zora");
        Group group = createGroup("Team Gruppe", requester);

        GroupMember zoraMembership = createMembership(group, zora, GroupRole.MEMBER);
        GroupMember requesterMembership = createMembership(group, requester, GroupRole.ADMIN);
        GroupMember annaMembership = createMembership(group, anna, GroupRole.MEMBER);
        zoraMembership.setStrichCount(7);
        requesterMembership.setStrichCount(3);
        annaMembership.setStrichCount(1);
        groupMemberRepository.save(zoraMembership);
        groupMemberRepository.save(requesterMembership);
        groupMemberRepository.save(annaMembership);

        String token = jwtTokenProvider.createAccessToken(requester);

        mockMvc.perform(get("/api/v1/groups/" + group.getId() + "/members")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].username").value("Anna"))
            .andExpect(jsonPath("$[0].userId").value(anna.getId()))
            .andExpect(jsonPath("$[0].joinedAt").isNotEmpty())
            .andExpect(jsonPath("$[0].role").value("MEMBER"))
            .andExpect(jsonPath("$[0].strichCount").value(1))
            .andExpect(jsonPath("$[1].username").value("Mona"))
            .andExpect(jsonPath("$[1].role").value("ADMIN"))
            .andExpect(jsonPath("$[1].strichCount").value(3))
            .andExpect(jsonPath("$[2].username").value("Zora"))
            .andExpect(jsonPath("$[2].role").value("MEMBER"))
            .andExpect(jsonPath("$[2].strichCount").value(7));
    }

    @Test
    void getGroupMembersUsesStableSortingForDuplicateUsernames() throws Exception {
        User requester = createUser("duplicate-requester@example.com", "Requester");
        User firstChris = createUser("duplicate-first@example.com", "Chris");
        User secondChris = createUser("duplicate-second@example.com", "Chris");
        Group group = createGroup("Duplicate Namen", requester);

        createMembership(group, requester, GroupRole.ADMIN);
        createMembership(group, firstChris, GroupRole.MEMBER);
        createMembership(group, secondChris, GroupRole.MEMBER);

        String token = jwtTokenProvider.createAccessToken(requester);

        mockMvc.perform(get("/api/v1/groups/" + group.getId() + "/members")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].userId").value(firstChris.getId()))
            .andExpect(jsonPath("$[0].username").value("Chris"))
            .andExpect(jsonPath("$[1].userId").value(secondChris.getId()))
            .andExpect(jsonPath("$[1].username").value("Chris"))
            .andExpect(jsonPath("$[2].userId").value(requester.getId()))
            .andExpect(jsonPath("$[2].username").value("Requester"));
    }

    @Test
    void getGroupMembersReturnsNotFoundForNonMember() throws Exception {
        User member = createUser("members-visible@example.com", "Visible");
        User nonMember = createUser("members-hidden@example.com", "Hidden");
        Group group = createGroup("Nur Mitglieder", member);

        createMembership(group, member, GroupRole.ADMIN);

        String token = jwtTokenProvider.createAccessToken(nonMember);

        mockMvc.perform(get("/api/v1/groups/" + group.getId() + "/members")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Gruppe nicht gefunden"));
    }

    @Test
    void getGroupMembersReturnsNotFoundWhenGroupDoesNotExist() throws Exception {
        User user = createUser("members-missing-group@example.com", "MissingGroupUser");
        String token = jwtTokenProvider.createAccessToken(user);

        mockMvc.perform(get("/api/v1/groups/999999/members")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Gruppe nicht gefunden"));
    }

    @Test
    void getOwnCounterReturnsPersistedStrichCountForMember() throws Exception {
        User member = createUser("counter-member@example.com", "CounterMember");
        User creator = createUser("counter-creator@example.com", "CounterCreator");
        Group group = createGroup("Counter Gruppe", creator);

        GroupMember membership = createMembership(group, member, GroupRole.MEMBER);
        membership.setStrichCount(9);
        groupMemberRepository.save(membership);

        String token = jwtTokenProvider.createAccessToken(member);

        mockMvc.perform(get("/api/v1/groups/" + group.getId() + "/me/counter")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.count").value(9));
    }

    @Test
    void getOwnCounterReturnsNotFoundForNonMember() throws Exception {
        User member = createUser("counter-group-member@example.com", "Member");
        User nonMember = createUser("counter-group-non-member@example.com", "NonMember");
        Group group = createGroup("Private Counter Gruppe", member);

        createMembership(group, member, GroupRole.ADMIN);

        String token = jwtTokenProvider.createAccessToken(nonMember);

        mockMvc.perform(get("/api/v1/groups/" + group.getId() + "/me/counter")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Gruppe nicht gefunden"));
    }

    @Test
    void getOwnRoleReturnsMembershipRoleForMember() throws Exception {
        User member = createUser("role-member@example.com", "RoleMember");
        User creator = createUser("role-creator@example.com", "RoleCreator");
        Group group = createGroup("Role Gruppe", creator);

        createMembership(group, member, GroupRole.MEMBER);

        String token = jwtTokenProvider.createAccessToken(member);

        mockMvc.perform(get("/api/v1/groups/" + group.getId() + "/me/role")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void getOwnRoleReturnsNotFoundForNonMember() throws Exception {
        User member = createUser("role-group-member@example.com", "Member");
        User nonMember = createUser("role-group-non-member@example.com", "NonMember");
        Group group = createGroup("Private Role Gruppe", member);

        createMembership(group, member, GroupRole.ADMIN);

        String token = jwtTokenProvider.createAccessToken(nonMember);

        mockMvc.perform(get("/api/v1/groups/" + group.getId() + "/me/role")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Gruppe nicht gefunden"));
    }

    @Test
    void incrementOwnCounterIncreasesCountByOne() throws Exception {
        User member = createUser("increment-one@example.com", "IncrementOne");
        User creator = createUser("increment-one-creator@example.com", "Creator");
        Group group = createGroup("Increment Eins", creator);

        GroupMember membership = createMembership(group, member, GroupRole.MEMBER);
        membership.setStrichCount(2);
        groupMemberRepository.save(membership);

        String token = jwtTokenProvider.createAccessToken(member);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/me/counter/increment")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 1))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.count").value(3));

        int updatedCount = groupMemberRepository.findStrichCountByGroup_IdAndUser_Id(group.getId(), member.getId()).orElseThrow();
        assertThat(updatedCount).isEqualTo(3);
    }

    @Test
    void incrementOwnCounterIncreasesCountByRequestedAmount() throws Exception {
        User member = createUser("increment-many@example.com", "IncrementMany");
        User creator = createUser("increment-many-creator@example.com", "CreatorMany");
        Group group = createGroup("Increment Mehrfach", creator);

        GroupMember membership = createMembership(group, member, GroupRole.MEMBER);
        membership.setStrichCount(5);
        groupMemberRepository.save(membership);

        String token = jwtTokenProvider.createAccessToken(member);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/me/counter/increment")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 7))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.count").value(12));

        int updatedCount = groupMemberRepository.findStrichCountByGroup_IdAndUser_Id(group.getId(), member.getId()).orElseThrow();
        assertThat(updatedCount).isEqualTo(12);
    }

    @Test
    void incrementOwnCounterReturnsNotFoundForNonMember() throws Exception {
        User member = createUser("increment-member@example.com", "IncrementMember");
        User nonMember = createUser("increment-non-member@example.com", "IncrementNonMember");
        Group group = createGroup("Increment Privat", member);

        createMembership(group, member, GroupRole.ADMIN);

        String token = jwtTokenProvider.createAccessToken(nonMember);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/me/counter/increment")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 1))))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Gruppe nicht gefunden"));
    }

    @Test
    void incrementOwnCounterReturnsBadRequestWhenAmountIsZeroOrNegative() throws Exception {
        User member = createUser("increment-validation@example.com", "IncrementValidation");
        User creator = createUser("increment-validation-creator@example.com", "IncrementValidationCreator");
        Group group = createGroup("Increment Validation", creator);

        createMembership(group, member, GroupRole.MEMBER);

        String token = jwtTokenProvider.createAccessToken(member);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/me/counter/increment")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 0))))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.amount").exists());
    }

    @Test
    void joinGroupCreatesMembership() throws Exception {
        User joiningUser = createUser("join-user@example.com", "JoinUser");
        User creator = createUser("join-creator@example.com", "Creator");
        Group group = createGroup("Join Gruppe", creator);

        String token = jwtTokenProvider.createAccessToken(joiningUser);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/join")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("Mitgliedschaft aktiv"));

        var membership = groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), joiningUser.getId()).orElseThrow();
        assertThat(membership.getRole()).isEqualTo(GroupRole.MEMBER);
        assertThat(membership.getStrichCount()).isZero();
    }

    @Test
    void joinGroupIsIdempotentForExistingMembership() throws Exception {
        User joiningUser = createUser("join-idempotent@example.com", "Idempotent");
        User creator = createUser("join-owner@example.com", "Owner");
        Group group = createGroup("Join Idempotent", creator);

        String token = jwtTokenProvider.createAccessToken(joiningUser);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/join")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Mitgliedschaft aktiv"));

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/join")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Mitgliedschaft aktiv"));

        long memberCountForUser = groupMemberRepository.findAllByGroup_Id(group.getId())
            .stream()
            .filter(member -> member.getUser().getId().equals(joiningUser.getId()))
            .count();

        assertThat(memberCountForUser).isEqualTo(1);
    }

    @Test
    void joinGroupReturnsNotFoundWhenGroupDoesNotExist() throws Exception {
        User joiningUser = createUser("join-missing@example.com", "MissingGroup");
        String token = jwtTokenProvider.createAccessToken(joiningUser);

        mockMvc.perform(post("/api/v1/groups/999999/join")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Gruppe nicht gefunden"));
    }

    @Test
    void leaveGroupRemovesMembership() throws Exception {
        User leavingUser = createUser("leave-user@example.com", "LeavingUser");
        User creator = createUser("leave-creator@example.com", "LeaveCreator");
        Group group = createGroup("Leave Gruppe", creator);

        createMembership(group, leavingUser, GroupRole.MEMBER);

        String token = jwtTokenProvider.createAccessToken(leavingUser);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/leave")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("Gruppe verlassen"));

        assertThat(groupMemberRepository.existsByGroup_IdAndUser_Id(group.getId(), leavingUser.getId())).isFalse();
    }

    @Test
    void leaveGroupReturnsNotFoundWhenUserIsNotMember() throws Exception {
        User member = createUser("leave-member@example.com", "LeaveMember");
        User nonMember = createUser("leave-non-member@example.com", "LeaveNonMember");
        Group group = createGroup("Leave Private", member);

        createMembership(group, member, GroupRole.ADMIN);

        String token = jwtTokenProvider.createAccessToken(nonMember);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/leave")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Gruppe nicht gefunden"));
    }

    @Test
    void promoteGroupMemberPromotesMemberWhenCallerIsAdmin() throws Exception {
        User admin = createUser("promote-admin@example.com", "PromoteAdmin");
        User member = createUser("promote-member@example.com", "PromoteMember");
        Group group = createGroup("Promotion Gruppe", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        createMembership(group, member, GroupRole.MEMBER);

        String token = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/roles/promote")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("targetUserId", member.getId()))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.userId").value(member.getId()))
            .andExpect(jsonPath("$.username").value("PromoteMember"))
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.joinedAt").isNotEmpty())
            .andExpect(jsonPath("$.strichCount").value(0));

        GroupMember promotedMembership = groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), member.getId()).orElseThrow();
        assertThat(promotedMembership.getRole()).isEqualTo(GroupRole.ADMIN);
    }

    @Test
    void promoteGroupMemberReturnsForbiddenWhenCallerIsNotAdmin() throws Exception {
        User admin = createUser("promote-rights-admin@example.com", "Admin");
        User member = createUser("promote-rights-member@example.com", "Member");
        User target = createUser("promote-rights-target@example.com", "Target");
        Group group = createGroup("Promotion Rechte", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        createMembership(group, member, GroupRole.MEMBER);
        createMembership(group, target, GroupRole.MEMBER);

        String token = jwtTokenProvider.createAccessToken(member);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/roles/promote")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("targetUserId", target.getId()))))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Wart-Rechte erforderlich"));

        GroupMember targetMembership = groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), target.getId()).orElseThrow();
        assertThat(targetMembership.getRole()).isEqualTo(GroupRole.MEMBER);
    }

    @Test
    void promoteGroupMemberReturnsNotFoundWhenTargetIsNotMember() throws Exception {
        User admin = createUser("promote-missing-admin@example.com", "Admin");
        User outsider = createUser("promote-missing-outsider@example.com", "Outsider");
        Group group = createGroup("Promotion Missing", admin);

        createMembership(group, admin, GroupRole.ADMIN);

        String token = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/roles/promote")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("targetUserId", outsider.getId()))))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Gruppenmitglied nicht gefunden"));
    }

    @Test
    void promoteGroupMemberReturnsNotFoundWhenTargetUserDoesNotExist() throws Exception {
        User admin = createUser("promote-missing-user-admin@example.com", "Admin");
        Group group = createGroup("Promotion Missing User", admin);

        createMembership(group, admin, GroupRole.ADMIN);

        String token = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/roles/promote")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("targetUserId", 999999L))))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("User nicht gefunden"));
    }

    @Test
    void promoteGroupMemberIsIdempotentWhenTargetIsAlreadyAdmin() throws Exception {
        User admin = createUser("promote-idempotent-admin@example.com", "Admin");
        User target = createUser("promote-idempotent-target@example.com", "Target");
        Group group = createGroup("Promotion Idempotent", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        createMembership(group, target, GroupRole.ADMIN);

        String token = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/roles/promote")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("targetUserId", target.getId()))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.userId").value(target.getId()))
            .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void demoteGroupMemberDemotesAdminWhenAnotherAdminRemains() throws Exception {
        User caller = createUser("demote-caller@example.com", "CallerAdmin");
        User target = createUser("demote-target@example.com", "TargetAdmin");
        Group group = createGroup("Demotion Gruppe", caller);

        createMembership(group, caller, GroupRole.ADMIN);
        createMembership(group, target, GroupRole.ADMIN);

        String token = jwtTokenProvider.createAccessToken(caller);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/roles/demote")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("targetUserId", target.getId()))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.userId").value(target.getId()))
            .andExpect(jsonPath("$.username").value("TargetAdmin"))
            .andExpect(jsonPath("$.role").value("MEMBER"))
            .andExpect(jsonPath("$.joinedAt").isNotEmpty())
            .andExpect(jsonPath("$.strichCount").value(0));

        GroupMember demotedMembership = groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), target.getId()).orElseThrow();
        assertThat(demotedMembership.getRole()).isEqualTo(GroupRole.MEMBER);
    }

    @Test
    void demoteGroupMemberReturnsForbiddenWhenCallerIsNotAdmin() throws Exception {
        User admin = createUser("demote-rights-admin@example.com", "Admin");
        User member = createUser("demote-rights-member@example.com", "Member");
        User target = createUser("demote-rights-target@example.com", "Target");
        Group group = createGroup("Demotion Rechte", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        createMembership(group, member, GroupRole.MEMBER);
        createMembership(group, target, GroupRole.ADMIN);

        String token = jwtTokenProvider.createAccessToken(member);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/roles/demote")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("targetUserId", target.getId()))))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Wart-Rechte erforderlich"));

        GroupMember targetMembership = groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), target.getId()).orElseThrow();
        assertThat(targetMembership.getRole()).isEqualTo(GroupRole.ADMIN);
    }

    @Test
    void demoteGroupMemberReturnsNotFoundWhenTargetIsNotMember() throws Exception {
        User admin = createUser("demote-missing-admin@example.com", "Admin");
        User outsider = createUser("demote-missing-outsider@example.com", "Outsider");
        Group group = createGroup("Demotion Missing", admin);

        createMembership(group, admin, GroupRole.ADMIN);

        String token = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/roles/demote")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("targetUserId", outsider.getId()))))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Gruppenmitglied nicht gefunden"));
    }

    @Test
    void demoteGroupMemberReturnsNotFoundWhenTargetUserDoesNotExist() throws Exception {
        User admin = createUser("demote-missing-user-admin@example.com", "Admin");
        Group group = createGroup("Demotion Missing User", admin);

        createMembership(group, admin, GroupRole.ADMIN);

        String token = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/roles/demote")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("targetUserId", 999999L))))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("User nicht gefunden"));
    }

    @Test
    void demoteGroupMemberBlocksLastRemainingAdmin() throws Exception {
        User admin = createUser("demote-last-admin@example.com", "LastAdmin");
        Group group = createGroup("Demotion Last Admin", admin);

        createMembership(group, admin, GroupRole.ADMIN);

        String token = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/roles/demote")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("targetUserId", admin.getId()))))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Mindestens ein Wart muss in der Gruppe verbleiben"));

        GroupMember adminMembership = groupMemberRepository.findByGroup_IdAndUser_Id(group.getId(), admin.getId()).orElseThrow();
        assertThat(adminMembership.getRole()).isEqualTo(GroupRole.ADMIN);
    }

    @Test
    void demoteGroupMemberIsIdempotentWhenTargetIsAlreadyMember() throws Exception {
        User admin = createUser("demote-idempotent-admin@example.com", "Admin");
        User target = createUser("demote-idempotent-target@example.com", "Target");
        Group group = createGroup("Demotion Idempotent", admin);

        createMembership(group, admin, GroupRole.ADMIN);
        createMembership(group, target, GroupRole.MEMBER);

        String token = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/groups/" + group.getId() + "/roles/demote")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("targetUserId", target.getId()))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.userId").value(target.getId()))
            .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void createGroupReturnsValidationErrorWhenNameIsBlank() throws Exception {
        String token = createAccessTokenForUser("valid-user@example.com");

        mockMvc.perform(post("/api/v1/groups")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", " "))))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name").exists());
    }

    @Test
    void createGroupCreatesGroupAndMembership() throws Exception {
        User creator = createUser("group-creator@example.com");
        String token = jwtTokenProvider.createAccessToken(creator);

        mockMvc.perform(post("/api/v1/groups")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Neue Gruppe"))))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.name").value("Neue Gruppe"))
            .andExpect(jsonPath("$.createdByUserId").value(creator.getId()))
            .andExpect(jsonPath("$.pricePerStrich").value(1))
            .andExpect(jsonPath("$.onlyWartsCanBookForOthers").value(true));

        Long groupId = groupRepository.findAll().getFirst().getId();
        Group persistedGroup = groupRepository.findById(groupId).orElseThrow();

        var membership = groupMemberRepository.findByGroup_IdAndUser_Id(groupId, creator.getId()).orElseThrow();
        assertThat(persistedGroup.getPricePerStrich()).isEqualByComparingTo("1.00");
        assertThat(persistedGroup.isOnlyWartsCanBookForOthers()).isTrue();
        assertThat(membership.getRole()).isEqualTo(GroupRole.ADMIN);
        assertThat(membership.getStrichCount()).isZero();
    }

    @Test
    void updateGroupSettingsUpdatesOnlySettingsForAdmin() throws Exception {
        User admin = createUser("group-update-settings-admin@example.com");
        Group group = createGroup("Konstante Gruppe", admin);
        createMembership(group, admin, GroupRole.ADMIN);

        String token = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(put("/api/v1/groups/" + group.getId() + "/settings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "pricePerStrich", 2.75,
                    "onlyWartsCanBookForOthers", false
                ))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.pricePerStrich").value(2.75))
            .andExpect(jsonPath("$.onlyWartsCanBookForOthers").value(false));

        Group updatedGroup = groupRepository.findById(group.getId()).orElseThrow();
        assertThat(updatedGroup.getName()).isEqualTo("Konstante Gruppe");
        assertThat(updatedGroup.getCreatedByUserId()).isEqualTo(admin.getId());
        assertThat(updatedGroup.getPricePerStrich()).isEqualByComparingTo("2.75");
        assertThat(updatedGroup.isOnlyWartsCanBookForOthers()).isFalse();
    }

    @Test
    void updateGroupSettingsReturnsForbiddenWhenCallerIsNotAdmin() throws Exception {
        User admin = createUser("group-update-settings-owner@example.com");
        User member = createUser("group-update-settings-member@example.com");
        Group group = createGroup("Geschuetzte Settings", admin);
        createMembership(group, admin, GroupRole.ADMIN);
        createMembership(group, member, GroupRole.MEMBER);

        String token = jwtTokenProvider.createAccessToken(member);

        mockMvc.perform(put("/api/v1/groups/" + group.getId() + "/settings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "pricePerStrich", 3.10,
                    "onlyWartsCanBookForOthers", false
                ))))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Wart-Rechte erforderlich"));

        Group unchangedGroup = groupRepository.findById(group.getId()).orElseThrow();
        assertThat(unchangedGroup.getPricePerStrich()).isEqualByComparingTo("1.00");
        assertThat(unchangedGroup.isOnlyWartsCanBookForOthers()).isTrue();
    }

    private String createAccessTokenForUser(String email) {
        User user = createUser(email);
        return jwtTokenProvider.createAccessToken(user);
    }

    private User createUser(String email) {
        return createUser(email, email);
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
