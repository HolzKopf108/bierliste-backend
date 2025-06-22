package com.bierliste.backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public void logout(String refreshToken) {
        refreshService.delete(refreshToken);
    }
    
    @Transactional
    public void resetPasswordSet(User user, String password) {
        user.setPasswordHash(pwdEnc.encode(password));
        userRepo.save(user);
    }
}
