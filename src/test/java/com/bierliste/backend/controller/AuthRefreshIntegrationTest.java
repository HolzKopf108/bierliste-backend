package com.bierliste.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bierliste.backend.model.RefreshToken;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.RefreshTokenRepository;
import com.bierliste.backend.repository.UserRepository;
import com.bierliste.backend.service.RefreshTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
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
class AuthRefreshIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Test
    void refreshWithValidTokenReturnsAllFieldsAndRotatesRefreshToken() throws Exception {
        User user = createUser("refresh-success@example.com");
        RefreshToken oldRefreshToken = refreshTokenService.create(user);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", oldRefreshToken.getToken()))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.userEmail").value(user.getEmail()))
            .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        String newRefreshToken = response.get("refreshToken").asText();

        assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken.getToken());
        assertThat(refreshTokenRepository.findByToken(oldRefreshToken.getToken())).isEmpty();
        assertThat(refreshTokenRepository.findByToken(newRefreshToken)).isPresent();
    }

    @Test
    void refreshWithInvalidTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", "invalid-refresh-token"))))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Ungültiges Refresh-Token"));
    }

    @Test
    void refreshWithExpiredTokenReturnsUnauthorizedAndDeletesToken() throws Exception {
        User user = createUser("refresh-expired@example.com");
        RefreshToken expiredToken = refreshTokenService.create(user);
        expiredToken.setExpiryDate(Instant.now().minusSeconds(60));
        refreshTokenRepository.save(expiredToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", expiredToken.getToken()))))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Refresh-Token abgelaufen"));

        assertThat(refreshTokenRepository.findByToken(expiredToken.getToken())).isEmpty();
    }

    @Test
    void refreshWithBlankTokenReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", ""))))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.refreshToken").exists());
    }

    private User createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(email);
        user.setPasswordHash("hashed");
        user.setEmailVerified(true);
        return userRepository.save(user);
    }
}
