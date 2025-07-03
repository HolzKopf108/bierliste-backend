package com.bierliste.backend.controller;

import com.bierliste.backend.model.User;
import com.bierliste.backend.service.UserSettingsService;
import com.bierliste.backend.dto.UserSettingsDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user/settings")
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    public UserSettingsController(UserSettingsService userSettingsService) {
        this.userSettingsService = userSettingsService;
    }

    @PutMapping
    public ResponseEntity<UserSettingsDto> updateSettings(@AuthenticationPrincipal User user,
                                                          @RequestBody UserSettingsDto dto) {
        return ResponseEntity.ok(userSettingsService.updateSettingsForUser(user, dto));
    }

    @PostMapping("/verifyPassword")
    public ResponseEntity<Map<String, Boolean>> verifyPassword(@RequestBody Map<String, String> body,
                                                               @AuthenticationPrincipal User user) {
        boolean valid = userSettingsService.verifyPassword(user, body.get("password"));
        return ResponseEntity.ok(Map.of("valid", valid));
    }

}
