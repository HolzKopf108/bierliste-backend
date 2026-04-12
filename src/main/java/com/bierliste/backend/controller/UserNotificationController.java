package com.bierliste.backend.controller;

import com.bierliste.backend.dto.AndroidPushTokenDto;
import com.bierliste.backend.model.User;
import com.bierliste.backend.service.AndroidPushTokenService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/notifications/android")
public class UserNotificationController {

    private final AndroidPushTokenService androidPushTokenService;

    public UserNotificationController(AndroidPushTokenService androidPushTokenService) {
        this.androidPushTokenService = androidPushTokenService;
    }

    @PutMapping
    public ResponseEntity<Map<String, String>> registerAndroidPushToken(
        @Valid @RequestBody AndroidPushTokenDto dto,
        @AuthenticationPrincipal User user
    ) {
        androidPushTokenService.registerToken(user, dto);
        return ResponseEntity.ok(Map.of("message", "Android-Benachrichtigungstoken gespeichert"));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> unregisterAndroidPushToken(
        @Valid @RequestBody AndroidPushTokenDto dto,
        @AuthenticationPrincipal User user
    ) {
        androidPushTokenService.unregisterToken(user, dto);
        return ResponseEntity.ok(Map.of("message", "Android-Benachrichtigungstoken entfernt"));
    }
}
