package com.bierliste.backend.controller;

import com.bierliste.backend.dto.GroupSummaryDto;
import com.bierliste.backend.model.User;
import com.bierliste.backend.service.GroupInviteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invites")
public class InviteController {

    private final GroupInviteService groupInviteService;

    public InviteController(GroupInviteService groupInviteService) {
        this.groupInviteService = groupInviteService;
    }

    @PostMapping("/{token}/join")
    public ResponseEntity<GroupSummaryDto> joinGroup(@PathVariable String token, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(groupInviteService.joinGroupByToken(token, user));
    }
}
