package com.bierliste.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.bierliste.backend.model.User;
import com.bierliste.backend.service.UserService;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody Map<String, String> body) {
        String token = body.get("refreshToken");
        userService.logout(token);
        return ResponseEntity.ok(Map.of("message", "Logout erfolgreich"));
    }
    
    @PostMapping("/resetPasswordSet")
    public ResponseEntity<Map<String,String>> resetPasswordSet(@RequestBody Map<String, String> body, @AuthenticationPrincipal User user) {
        userService.resetPasswordSet(user, body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Passwort erfolgreich zurückgesetzt"));
    }
}
