package com.bierliste.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.bierliste.backend.model.VerificationToken;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.VerificationTokenRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class VerificationService {
    @Value("${app.verif-exp-minutes}")
    private long expMinutes;

    private final VerificationTokenRepository repo;
    private final EmailService emailService;

    public VerificationService(VerificationTokenRepository repo, EmailService emailService) {
        this.repo = repo;
        this.emailService = emailService;
    }

    @Transactional
    public void createAndSend(User user) {
        repo.deleteByUser(user);

        VerificationToken vt = new VerificationToken();
        vt.setUser(user);
        vt.setCode(String.format("%06d", new SecureRandom().nextInt(1_000_000)));
        vt.setExpiryDate(Instant.now().plus(expMinutes, ChronoUnit.MINUTES));
        repo.save(vt);

        emailService.sendVerificationEmail(user, vt.getCode());
    }

    @Transactional
    public void verify(String email, String code) {
        VerificationToken vt = repo.findByUserEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kein Verifizierungscode zur Email-Adresse gefunden"));

        if (vt.getExpiryDate().isBefore(Instant.now())) {
            repo.delete(vt);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code abgelaufen");
        }

        if (!vt.getCode().equals(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falscher Verifizierungscode");
        }

        User user = vt.getUser();
        user.setEmailVerified(true);
        repo.delete(vt);
    }
}