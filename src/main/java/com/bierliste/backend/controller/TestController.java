package com.bierliste.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bierliste.backend.model.User;
import com.bierliste.backend.service.EmailService;

@RestController
@RequestMapping("/api/v1/email")
public class TestController {

    private final EmailService emailService;

    public TestController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping
    public String testEmail() {
        User user = new User();
        user.setUsername("TestUsername");
        user.setEmail("linus.koelker@gmx.de");
        emailService.sendVerificationEmail(user, "839461", false);
        return "OK";
    }
}