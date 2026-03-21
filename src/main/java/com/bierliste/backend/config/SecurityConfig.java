package com.bierliste.backend.config;

import com.bierliste.backend.security.JwtAuthenticationFilter;
import com.bierliste.backend.security.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
public class SecurityConfig {
    private final JwtTokenProvider jwtProvider;

    public SecurityConfig(JwtTokenProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        PathPatternRequestMatcher.Builder mvc = PathPatternRequestMatcher.withDefaults();

        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"error\":\"Nicht authentifiziert\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"error\":\"Zugriff verweigert\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/auth/**",
                    "/api/v1/ping",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**"
                ).permitAll()
                .requestMatchers(
                    mvc.matcher(HttpMethod.GET, "/api/v1/email"),
                    mvc.matcher(HttpMethod.GET, "/api/v1/groups"),
                    mvc.matcher(HttpMethod.POST, "/api/v1/groups"),
                    mvc.matcher(HttpMethod.GET, "/api/v1/groups/{groupId}"),
                    mvc.matcher(HttpMethod.GET, "/api/v1/groups/{groupId}/settings"),
                    mvc.matcher(HttpMethod.GET, "/api/v1/groups/{groupId}/members"),
                    mvc.matcher(HttpMethod.GET, "/api/v1/groups/{groupId}/activities"),
                    mvc.matcher(HttpMethod.GET, "/api/v1/groups/{groupId}/me/counter"),
                    mvc.matcher(HttpMethod.GET, "/api/v1/groups/{groupId}/me/role"),
                    mvc.matcher(HttpMethod.PUT, "/api/v1/groups/{groupId}/settings"),
                    mvc.matcher(HttpMethod.POST, "/api/v1/groups/{groupId}/me/counter/increment"),
                    mvc.matcher(HttpMethod.POST, "/api/v1/groups/{groupId}/members/{targetUserId}/counter/increment"),
                    mvc.matcher(HttpMethod.POST, "/api/v1/groups/{groupId}/join"),
                    mvc.matcher(HttpMethod.POST, "/api/v1/groups/{groupId}/leave"),
                    mvc.matcher(HttpMethod.POST, "/api/v1/groups/{groupId}/roles/promote"),
                    mvc.matcher(HttpMethod.POST, "/api/v1/groups/{groupId}/roles/demote"),
                    mvc.matcher(HttpMethod.POST, "/api/v1/groups/{groupId}/members/{targetUserId}/settlements/money"),
                    mvc.matcher(HttpMethod.POST, "/api/v1/groups/{groupId}/members/{targetUserId}/settlements/striche"),
                    mvc.matcher(HttpMethod.GET, "/api/v1/user"),
                    mvc.matcher(HttpMethod.PUT, "/api/v1/user"),
                    mvc.matcher(HttpMethod.POST, "/api/v1/user/logout"),
                    mvc.matcher(HttpMethod.POST, "/api/v1/user/updatePassword"),
                    mvc.matcher(HttpMethod.DELETE, "/api/v1/user/delete/account"),
                    mvc.matcher(HttpMethod.GET, "/api/v1/user/settings"),
                    mvc.matcher(HttpMethod.PUT, "/api/v1/user/settings"),
                    mvc.matcher(HttpMethod.POST, "/api/v1/user/settings/verifyPassword")
                ).authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtProvider),
                UsernamePasswordAuthenticationFilter.class
            );
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
