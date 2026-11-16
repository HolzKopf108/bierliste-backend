package com.bierliste.backend.controller;

import com.bierliste.backend.dto.SyncEventsRequestDto;
import com.bierliste.backend.dto.SyncEventsResponseDto;
import com.bierliste.backend.model.User;
import com.bierliste.backend.service.SyncService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/events")
    public ResponseEntity<SyncEventsResponseDto> syncEvents(
        @Valid @RequestBody SyncEventsRequestDto dto,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(syncService.processEvents(dto, user));
    }
}
