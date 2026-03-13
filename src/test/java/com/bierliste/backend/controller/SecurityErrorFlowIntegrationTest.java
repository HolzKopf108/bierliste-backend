package com.bierliste.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bierliste.backend.model.RefreshToken;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.RefreshTokenRepository;
import com.bierliste.backend.repository.UserRepository;
import com.bierliste.backend.security.JwtTokenProvider;
import com.bierliste.backend.service.RefreshTokenService;
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
class SecurityErrorFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void unknownProtectedRouteReturnsNotFoundAndKeepsRefreshToken() throws Exception {
        User user = createUser("unknown-route@example.com");
        RefreshToken refreshToken = refreshTokenService.create(user);

        mockMvc.perform(get("/api/v1/groups/1/unknown"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Endpoint nicht gefunden"));

        assertRefreshTokenExists(refreshToken.getToken());
    }

    @Test
    void unsupportedMethodReturnsMethodNotAllowedAndKeepsRefreshToken() throws Exception {
        User user = createUser("method-not-allowed@example.com");
        RefreshToken refreshToken = refreshTokenService.create(user);

        mockMvc.perform(delete("/api/v1/groups"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Methode nicht erlaubt"));

        assertRefreshTokenExists(refreshToken.getToken());
    }

    @Test
    void deletedUserAccessTokenReturnsUnauthorizedInsteadOfServerError() throws Exception {
        User user = createUser("deleted-user@example.com");
        String accessToken = jwtTokenProvider.createAccessToken(user);

        userRepository.delete(user);
        userRepository.flush();

        mockMvc.perform(get("/api/v1/groups")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Nicht authentifiziert"));
    }

    private User createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(email);
        user.setPasswordHash("hashed");
        return userRepository.save(user);
    }

    private void assertRefreshTokenExists(String token) {
        refreshTokenRepository.findByToken(token).orElseThrow();
    }
}
