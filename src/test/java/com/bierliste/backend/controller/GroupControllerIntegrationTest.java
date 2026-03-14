package com.bierliste.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void getGroupMembersReturnsUnauthorizedWhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(get("/api/v1/groups/1/members"))
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
            .andExpect(jsonPath("$.createdAt").isNotEmpty());
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
            .andExpect(jsonPath("$.createdByUserId").value(creator.getId()));

        Long groupId = groupRepository.findAll().getFirst().getId();

        var membership = groupMemberRepository.findByGroup_IdAndUser_Id(groupId, creator.getId()).orElseThrow();
        assertThat(membership.getRole()).isEqualTo(GroupRole.ADMIN);
        assertThat(membership.getStrichCount()).isZero();
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
