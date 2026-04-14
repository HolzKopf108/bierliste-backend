package com.bierliste.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bierliste.backend.model.User;
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
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void getUserReturnsCurrentUserIncludingStableUserId() throws Exception {
        User user = createUser("current-user@example.com", "CurrentUser");
        String accessToken = jwtTokenProvider.createAccessToken(user);

        mockMvc.perform(get("/api/v1/user")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.userId").value(user.getId()))
            .andExpect(jsonPath("$.email").value(user.getEmail()))
            .andExpect(jsonPath("$.username").value(user.getUsername()))
            .andExpect(jsonPath("$.lastUpdated").isNotEmpty())
            .andExpect(jsonPath("$.googleUser").value(false));
    }

    @Test
    void updateUserAcceptsExistingRequestShapeAndReturnsUserId() throws Exception {
        User user = createUser("update-user@example.com", "OldName");
        String accessToken = jwtTokenProvider.createAccessToken(user);

        mockMvc.perform(put("/api/v1/user")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", user.getEmail(),
                    "username", "NewName",
                    "lastUpdated", user.getLastUpdated().plusSeconds(60).toString()
                ))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.userId").value(user.getId()))
            .andExpect(jsonPath("$.email").value(user.getEmail()))
            .andExpect(jsonPath("$.username").value("NewName"));

        User persistedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(persistedUser.getUsername()).isEqualTo("NewName");
    }

    @Test
    void openApiDocumentsUserDtoWithReadOnlyUserId() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.components.schemas.UserDto.required").value(hasItem("userId")))
            .andExpect(jsonPath("$.components.schemas.UserDto.properties.userId.type").value("integer"))
            .andExpect(jsonPath("$.components.schemas.UserDto.properties.userId.format").value("int64"))
            .andExpect(jsonPath("$.components.schemas.UserDto.properties.userId.readOnly").value(true));
    }

    private User createUser(String email, String username) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash("hashed");
        user.setEmailVerified(true);
        return userRepository.save(user);
    }
}
