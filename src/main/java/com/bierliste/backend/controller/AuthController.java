package com.bierliste.backend.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.validation.Valid;
import java.util.Map;

import com.bierliste.backend.dto.GoogleLoginDto;
import com.bierliste.backend.dto.LoginDto;
import com.bierliste.backend.dto.RegisterDto;
import com.bierliste.backend.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String,String>> register(@Valid @RequestBody RegisterDto dto) {
        authService.register(dto);
        return ResponseEntity.ok(Map.of("message", "Verifizierungscode gesendet"));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String,String>> verify(@RequestBody Map<String,String> body) {
        var resp = authService.verify(body.get("email"), body.get("code"));
        return ResponseEntity.ok(Map.of(
            "accessToken", resp.accessToken(),
            "refreshToken", resp.refreshToken()
        ));
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resend(@RequestBody Map<String, String> body) {
        authService.resend(body.get("email"));
        return ResponseEntity.ok(Map.of("message", "Code erneut gesendet"));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String,String>> login(@Valid @RequestBody LoginDto dto) {
        var resp = authService.login(dto);
        return ResponseEntity.ok(Map.of(
            "accessToken", resp.accessToken(),
            "refreshToken", resp.refreshToken()
        ));
    }

    @PostMapping("/google")
    public ResponseEntity<Map<String,String>> loginWithGoogle(@RequestBody GoogleLoginDto dto) {
        var resp = authService.loginGoogle(dto.getIdToken());
        return ResponseEntity.ok(Map.of(
            "accessToken", resp.accessToken(),
            "refreshToken", resp.refreshToken()
        ));
    }


    @PostMapping("/refresh")
    public ResponseEntity<Map<String,String>> refresh(@RequestBody Map<String,String> body) {
        var resp = authService.refresh(body.get("refreshToken"));
        return ResponseEntity.ok(Map.of(
            "accessToken", resp.accessToken(),
            "refreshToken", resp.refreshToken()
        ));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody Map<String, String> body) {
        String token = body.get("refreshToken");
        authService.logout(token);
        return ResponseEntity.ok(Map.of("message", "Logout erfolgreich"));
    }
}