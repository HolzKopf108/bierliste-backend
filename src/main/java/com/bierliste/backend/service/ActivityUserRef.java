package com.bierliste.backend.service;

import com.bierliste.backend.model.User;

public record ActivityUserRef(Long userId, String displayNameSnapshot) {

    public ActivityUserRef {
        if (userId == null) {
            throw new IllegalArgumentException("User-ID darf nicht null sein");
        }
        if (displayNameSnapshot == null || displayNameSnapshot.isBlank()) {
            throw new IllegalArgumentException("DisplayName-Snapshot darf nicht leer sein");
        }
    }

    public static ActivityUserRef from(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User muss persistiert sein");
        }

        return new ActivityUserRef(user.getId(), user.getUsername());
    }
}
