package com.bierliste.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bierliste.backend.dto.UserDto;
import com.bierliste.backend.dto.UserPasswordDto;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private final UserRepository userRepo;
    private final PasswordEncoder pwdEnc;
    private final RefreshTokenService refreshService;

    public UserService(
        UserRepository userRepo, 
        PasswordEncoder pwdEnc,
        RefreshTokenService refreshService
    ) {
        this.userRepo = userRepo;
        this.pwdEnc = pwdEnc;
        this.refreshService = refreshService;
    }

    public UserDto getUser(User user) {
        User backendUser = userRepo.findByEmail(user.getEmail())
            .orElseThrow(() -> new UsernameNotFoundException("Benutzer nicht gefunden"));

        return UserDto.fromEntity(backendUser);
    }

    @Transactional
    public UserDto updateUsername(UserDto dto) {
        User user = userRepo.findByEmail(dto.getEmail())
            .orElseThrow(() -> new UsernameNotFoundException("Benutzer nicht gefunden"));

        if(dto.getLastUpdated().isAfter(user.getLastUpdated())) {
            user.setUsername(dto.getUsername());
            userRepo.save(user);
        }

        return UserDto.fromEntity(user);
    }

    @Transactional
    public void updatePassword(UserPasswordDto dto) {
        User user = userRepo.findByEmail(dto.getEmail())
            .orElseThrow(() -> new UsernameNotFoundException("Benutzer nicht gefunden"));

        if(dto.getLastUpdated().isAfter(user.getLastUpdated())) {
            user.setPasswordHash(pwdEnc.encode(dto.getPassword()));
            userRepo.save(user);
        }
        else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwort konnte nicht geändert werden. Versuche es erneut!");
        }
    }
    
    @Transactional
    public void logout(String refreshToken) {
        refreshService.delete(refreshToken);
    }
}
