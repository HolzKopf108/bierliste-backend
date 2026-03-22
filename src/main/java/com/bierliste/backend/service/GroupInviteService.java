package com.bierliste.backend.service;

import com.bierliste.backend.dto.GroupInviteResponseDto;
import com.bierliste.backend.dto.GroupSummaryDto;
import com.bierliste.backend.model.Group;
import com.bierliste.backend.model.GroupInvite;
import com.bierliste.backend.model.GroupMember;
import com.bierliste.backend.model.GroupRole;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.GroupInviteRepository;
import com.bierliste.backend.repository.GroupMemberRepository;
import jakarta.transaction.Transactional;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GroupInviteService {

    private static final int MAX_TOKEN_GENERATION_ATTEMPTS = 10;
    private static final String INVITE_NOT_FOUND_MESSAGE = "Einladung nicht gefunden";
    private static final String INVITE_EXPIRED_MESSAGE = "Einladung abgelaufen";

    private final GroupInviteRepository groupInviteRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupAuthorizationService groupAuthorizationService;
    private final GroupInviteTokenService groupInviteTokenService;
    private final ActivityService activityService;
    private final String deepLinkScheme;

    public GroupInviteService(
        GroupInviteRepository groupInviteRepository,
        GroupMemberRepository groupMemberRepository,
        GroupAuthorizationService groupAuthorizationService,
        GroupInviteTokenService groupInviteTokenService,
        ActivityService activityService,
        @Value("${app.deep-link-scheme:bierliste}") String deepLinkScheme
    ) {
        this.groupInviteRepository = groupInviteRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupAuthorizationService = groupAuthorizationService;
        this.groupInviteTokenService = groupInviteTokenService;
        this.activityService = activityService;
        this.deepLinkScheme = deepLinkScheme;
    }

    @Transactional
    public GroupInviteResponseDto createInvite(Long groupId, User user) {
        GroupMember membership = groupAuthorizationService.requireInviteCreationPermission(groupId, user);
        GroupInvite invite = persistInvite(membership.getGroup(), user.getId());

        return new GroupInviteResponseDto(
            invite.getId(),
            invite.getToken(),
            buildJoinUrl(invite.getToken()),
            invite.getExpiresAt()
        );
    }

    @Transactional
    public GroupSummaryDto joinGroupByToken(String token, User user) {
        Long userId = groupAuthorizationService.requireAuthenticatedUserId(user);
        GroupInvite invite = groupInviteRepository.findByToken(token)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, INVITE_NOT_FOUND_MESSAGE));

        if (!Instant.now().isBefore(invite.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.GONE, INVITE_EXPIRED_MESSAGE);
        }

        Group group = invite.getGroup();
        if (groupMemberRepository.existsByGroup_IdAndUser_Id(group.getId(), userId)) {
            return new GroupSummaryDto(group.getId(), group.getName());
        }

        GroupMember membership = new GroupMember();
        membership.setGroup(group);
        membership.setUser(user);
        membership.setRole(GroupRole.MEMBER);

        try {
            groupMemberRepository.save(membership);
            activityService.logUserJoinedGroup(group.getId(), user, "INVITE");
        } catch (DataIntegrityViolationException ex) {
            if (!groupMemberRepository.existsByGroup_IdAndUser_Id(group.getId(), userId)) {
                throw ex;
            }
        }

        return new GroupSummaryDto(group.getId(), group.getName());
    }

    private GroupInvite persistInvite(Group group, Long userId) {
        for (int attempt = 0; attempt < MAX_TOKEN_GENERATION_ATTEMPTS; attempt++) {
            String token = groupInviteTokenService.generateToken();
            if (groupInviteRepository.existsByToken(token)) {
                continue;
            }

            GroupInvite invite = new GroupInvite();
            invite.setGroup(group);
            invite.setToken(token);
            invite.setCreatedByUserId(userId);

            try {
                return groupInviteRepository.save(invite);
            } catch (DataIntegrityViolationException ex) {
                if (!groupInviteRepository.existsByToken(token)) {
                    throw ex;
                }
            }
        }

        throw new IllegalStateException("Invite-Token konnte nicht erzeugt werden");
    }

    private String buildJoinUrl(String token) {
        return UriComponentsBuilder.newInstance()
            .scheme(deepLinkScheme)
            .host("join")
            .queryParam("token", token)
            .build()
            .encode()
            .toUriString();
    }
}
