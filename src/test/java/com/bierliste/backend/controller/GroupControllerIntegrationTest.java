package com.bierliste.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        User user = new User();
        user.setEmail(email);
        user.setUsername(email);
        user.setPasswordHash("hashed");
        return userRepository.save(user);
    }
}
