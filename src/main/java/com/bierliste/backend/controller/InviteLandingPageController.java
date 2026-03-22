package com.bierliste.backend.controller;

import com.bierliste.backend.service.InviteLandingPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/invites")
public class InviteLandingPageController {

    private final InviteLandingPageService inviteLandingPageService;

    public InviteLandingPageController(InviteLandingPageService inviteLandingPageService) {
        this.inviteLandingPageService = inviteLandingPageService;
    }

    @Operation(summary = "Öffentliche Invite-Landing-Page")
    @ApiResponse(
        responseCode = "200",
        description = "HTML-Landing-Page, die versucht die App ueber das Custom URL Scheme zu oeffnen.",
        content = @Content(
            mediaType = MediaType.TEXT_HTML_VALUE,
            schema = @Schema(type = "string")
        )
    )
    @GetMapping(value = "/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getInviteLandingPage(@PathVariable String token) {
        return ResponseEntity.ok()
            .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
            .body(inviteLandingPageService.buildLandingPage(token));
    }
}
