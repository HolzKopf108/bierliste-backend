package com.bierliste.backend.service;

import com.bierliste.backend.model.User;

public record ActivityUserRef(Long userId, String usernameSnapshot) {

    public ActivityUserRef {
        if (userId == null) {
            throw new IllegalArgumentException("User-ID darf nicht null sein");
        }
        if (usernameSnapshot == null || usernameSnapshot.isBlank()) {
            throw new IllegalArgumentException("Username-Snapshot darf nicht leer sein");
        }
    }

    public static ActivityUserRef from(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User muss persistiert sein");
        }

        return new ActivityUserRef(user.getId(), user.getUsername());
    }
}
