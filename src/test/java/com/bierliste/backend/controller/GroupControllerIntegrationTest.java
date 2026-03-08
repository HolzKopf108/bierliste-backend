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
    void getGroupsReturnsOnlyGroupsForAuthenticatedUser() throws Exception {
        User user = createUser("groups-user@example.com");
        User secondUser = createUser("groups-second@example.com");
        User thirdUser = createUser("groups-third@example.com");

        Group alphaGroup = createGroup("Alpha", user.getId());
        Group betaGroup = createGroup("Beta", secondUser.getId());
        Group gammaGroup = createGroup("Gamma", thirdUser.getId());

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
        Group group = createGroup("Details Gruppe", creator.getId());

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
        Group group = createGroup("Private Gruppe", member.getId());

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
        Group group = createGroup("Team Gruppe", requester.getId());

        createMembership(group, zora, GroupRole.MEMBER);
        createMembership(group, requester, GroupRole.ADMIN);
        createMembership(group, anna, GroupRole.MEMBER);

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
            .andExpect(jsonPath("$[1].username").value("Mona"))
            .andExpect(jsonPath("$[1].role").value("ADMIN"))
            .andExpect(jsonPath("$[2].username").value("Zora"))
            .andExpect(jsonPath("$[2].role").value("MEMBER"));
    }

    @Test
    void getGroupMembersReturnsNotFoundForNonMember() throws Exception {
        User member = createUser("members-visible@example.com", "Visible");
        User nonMember = createUser("members-hidden@example.com", "Hidden");
        Group group = createGroup("Nur Mitglieder", member.getId());

        createMembership(group, member, GroupRole.ADMIN);

        String token = jwtTokenProvider.createAccessToken(nonMember);

        mockMvc.perform(get("/api/v1/groups/" + group.getId() + "/members")
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

    private Group createGroup(String name, Long createdByUserId) {
        Group group = new Group();
        group.setName(name);
        group.setCreatedByUserId(createdByUserId);
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
