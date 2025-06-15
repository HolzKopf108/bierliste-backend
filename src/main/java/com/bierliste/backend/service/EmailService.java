package com.bierliste.backend.service;

import com.bierliste.backend.model.User;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final BrevoEmailService brevoEmailService;

    public EmailService(BrevoEmailService brevoEmailService) {
        this.brevoEmailService = brevoEmailService;
    }

    public void sendVerificationEmail(User user, String code) {
        brevoEmailService.sendVerificationEmailBrevo(user, code);
    }

    public void sendCustomEmail(String to, String subject, String plainText, String htmlContent) {
        brevoEmailService.sendEmail(to, subject, plainText, htmlContent);
    }
}
