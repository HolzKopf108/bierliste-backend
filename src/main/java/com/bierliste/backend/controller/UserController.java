package com.bierliste.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.bierliste.backend.dto.UserDto;
import com.bierliste.backend.dto.UserPasswordDto;
import com.bierliste.backend.model.User;
import com.bierliste.backend.service.UserService;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<UserDto> getUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getUser(user));
    }

    @PutMapping
    public ResponseEntity<UserDto> updateUsername(@AuthenticationPrincipal User user,
                                        @RequestBody UserDto dto) {
        return ResponseEntity.ok(userService.updateUsername(dto));
    }

    @PostMapping("/updatePassword")
    public ResponseEntity<Map<String,String>> updatePassword(@AuthenticationPrincipal User user, @RequestBody UserPasswordDto dto) {
        userService.updatePassword(dto);
        return ResponseEntity.ok(Map.of("message", "Passwort erfolgreich geändert"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody Map<String, String> body) {
        String token = body.get("refreshToken");
        userService.logout(token);
        return ResponseEntity.ok(Map.of("message", "Logout erfolgreich"));
    }

    @DeleteMapping("/delete/account")
    public ResponseEntity<Map<String,String>> deleteAccount(@AuthenticationPrincipal User user) {
        userService.deleteAccount(user);
        return ResponseEntity.ok(Map.of("message", "Konto erfolgreich gelöscht"));
    }
}
