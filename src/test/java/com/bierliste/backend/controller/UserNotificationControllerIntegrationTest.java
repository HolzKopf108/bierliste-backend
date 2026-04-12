package com.bierliste.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.AndroidPushTokenRepository;
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
class UserNotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AndroidPushTokenRepository androidPushTokenRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void registerAndroidPushTokenPersistsTokenForAuthenticatedUser() throws Exception {
        User user = createUser("android-token-user@example.com", "AndroidTokenUser");
        String accessToken = jwtTokenProvider.createAccessToken(user);

        mockMvc.perform(put("/api/v1/user/notifications/android")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", "android-token-123"))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("Android-Benachrichtigungstoken gespeichert"));

        var persistedToken = androidPushTokenRepository.findByToken("android-token-123").orElseThrow();
        assertThat(persistedToken.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void unregisterAndroidPushTokenRemovesTokenForAuthenticatedUser() throws Exception {
        User user = createUser("android-token-remove@example.com", "AndroidRemoveUser");
        String accessToken = jwtTokenProvider.createAccessToken(user);

        mockMvc.perform(put("/api/v1/user/notifications/android")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", "android-token-remove"))))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/user/notifications/android")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", "android-token-remove"))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("Android-Benachrichtigungstoken entfernt"));

        assertThat(androidPushTokenRepository.findByToken("android-token-remove")).isEmpty();
    }

    private User createUser(String email, String username) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash("hashed");
        return userRepository.save(user);
    }
}
