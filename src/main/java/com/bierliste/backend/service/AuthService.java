package com.bierliste.backend.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.bierliste.backend.dto.LoginDto;
import com.bierliste.backend.dto.RegisterDto;
import com.bierliste.backend.model.RefreshToken;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.UserRepository;
import com.bierliste.backend.security.JwtTokenProvider;

@Service
public class AuthService {
    private final UserRepository userRepo;
    private final PasswordEncoder pwdEnc;
    private final JwtTokenProvider jwtProvider;
    private final RefreshTokenService refreshService;
    private final VerificationService verificationService;
    private final AuthenticationManager authManager;

    public AuthService(UserRepository userRepo,
                       PasswordEncoder pwdEnc,
                       JwtTokenProvider jwtProvider,
                       RefreshTokenService refreshService,
                       VerificationService verificationService,
                       AuthenticationManager authManager) {
        this.userRepo = userRepo;
        this.pwdEnc = pwdEnc;
        this.jwtProvider = jwtProvider;
        this.refreshService = refreshService;
        this.verificationService = verificationService;
        this.authManager = authManager;
    }

    @Transactional
    public void register(RegisterDto dto) {
        if (userRepo.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email bereits vergeben");
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setUsername(dto.getUsername());
        user.setPasswordHash(pwdEnc.encode(dto.getPassword()));
        userRepo.save(user);

        verificationService.createAndSend(user);
    }

    @Transactional
    public AuthResponse verify(String email, String code) {
        verificationService.verify(email, code);

        User user = userRepo.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User nicht gefunden"));

        String accessToken = jwtProvider.createAccessToken(user);
        RefreshToken refreshToken = refreshService.create(user);
        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    public void resend(String email) {
        User user = userRepo.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User nicht gefunden"));

        verificationService.createAndSend(user);
    }

    public AuthResponse login(LoginDto dto) {
        User user = userRepo.findByEmail(dto.getEmail())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User nicht gefunden"));

        authManager.authenticate(new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword()));
        
        if (!user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Email nicht verifiziert");
        }
            
        String accessToken = jwtProvider.createAccessToken(user);
        RefreshToken refreshToken = refreshService.create(user);
        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    public AuthResponse refresh(String tokenStr) {
        RefreshToken old = refreshService.verify(tokenStr);
        User user = old.getUser();

        String accessToken = jwtProvider.createAccessToken(user);
        RefreshToken newRefresh = refreshService.create(user);
        return new AuthResponse(accessToken, newRefresh.getToken());
    }

    public void logout(String refreshToken) {
        refreshService.delete(refreshToken);
    }

    public record AuthResponse(String accessToken, String refreshToken) {}
}