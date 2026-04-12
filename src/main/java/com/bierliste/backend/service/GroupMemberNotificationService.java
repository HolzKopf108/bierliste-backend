package com.bierliste.backend.service;

import com.bierliste.backend.dto.GroupMemberNotificationCreateDto;
import com.bierliste.backend.dto.GroupMemberNotificationDto;
import com.bierliste.backend.model.AndroidPushToken;
import com.bierliste.backend.model.Group;
import com.bierliste.backend.model.GroupMember;
import com.bierliste.backend.model.GroupMemberNotification;
import com.bierliste.backend.model.GroupRole;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.AndroidPushTokenRepository;
import com.bierliste.backend.repository.GroupMemberNotificationRepository;
import com.bierliste.backend.repository.GroupMemberRepository;
import com.bierliste.backend.repository.UserRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GroupMemberNotificationService {

    private static final Logger log = LoggerFactory.getLogger(GroupMemberNotificationService.class);
    private static final String TARGET_NOT_FOUND_MESSAGE = "Gruppenmitglied nicht gefunden";
    private static final String TARGET_NO_NOTIFICATIONS_MESSAGE = "Benachrichtigungen fuer dieses Mitglied sind nicht verfuegbar";
    private static final String NOTIFICATION_NOT_FOUND_MESSAGE = "Benachrichtigung nicht gefunden";

    private final GroupAuthorizationService groupAuthorizationService;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMemberNotificationRepository groupMemberNotificationRepository;
    private final AndroidPushTokenRepository androidPushTokenRepository;
    private final AndroidPushSender androidPushSender;
    private final UserRepository userRepository;

    public GroupMemberNotificationService(
        GroupAuthorizationService groupAuthorizationService,
        GroupMemberRepository groupMemberRepository,
        GroupMemberNotificationRepository groupMemberNotificationRepository,
        AndroidPushTokenRepository androidPushTokenRepository,
        AndroidPushSender androidPushSender,
        UserRepository userRepository
    ) {
        this.groupAuthorizationService = groupAuthorizationService;
        this.groupMemberRepository = groupMemberRepository;
        this.groupMemberNotificationRepository = groupMemberNotificationRepository;
        this.androidPushTokenRepository = androidPushTokenRepository;
        this.androidPushSender = androidPushSender;
        this.userRepository = userRepository;
    }

    @Transactional
    public GroupMemberNotificationDto sendNotification(
        Long groupId,
        Long targetUserId,
        GroupMemberNotificationCreateDto dto,
        User actor
    ) {
        GroupMember actorMembership = groupAuthorizationService.requireMemberEntity(groupId, actor);
        if (actorMembership.getRole() != GroupRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wart-Rechte erforderlich");
        }

        GroupMember targetMembership = requireTargetMembership(groupId, targetUserId);
        List<AndroidPushToken> targetTokens = androidPushTokenRepository.findAllByUser_Id(targetUserId);
        if (targetTokens.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, TARGET_NO_NOTIFICATIONS_MESSAGE);
        }

        GroupMemberNotification notification = new GroupMemberNotification();
        notification.setGroupId(groupId);
        notification.setActorUserId(actorMembership.getUser().getId());
        notification.setActorUsernameSnapshot(actorMembership.getUser().getUsername());
        notification.setTargetUserId(targetMembership.getUser().getId());
        notification.setMessage(dto.getMessage().trim());

        GroupMemberNotification savedNotification = groupMemberNotificationRepository.save(notification);
        Group group = actorMembership.getGroup();
        AndroidPushSender.PushDispatchResult dispatchResult = androidPushSender.sendGroupMemberNotification(
            group,
            savedNotification,
            targetTokens.stream().map(AndroidPushToken::getToken).toList()
        );

        if (!dispatchResult.invalidTokens().isEmpty()) {
            androidPushTokenRepository.deleteAllByTokenIn(dispatchResult.invalidTokens());
        }

        if (!dispatchResult.configured()) {
            log.warn(
                "Notification {} for groupId={} targetUserId={} was stored but FCM is not configured",
                savedNotification.getId(),
                groupId,
                targetUserId
            );
        } else if (dispatchResult.successCount() == 0) {
            log.warn(
                "Notification {} for groupId={} targetUserId={} was stored but no FCM delivery succeeded",
                savedNotification.getId(),
                groupId,
                targetUserId
            );
        } else {
            log.info(
                "Notification {} for groupId={} targetUserId={} stored and delivered to {} Android token(s)",
                savedNotification.getId(),
                groupId,
                targetUserId,
                dispatchResult.successCount()
            );
        }

        return toDto(savedNotification);
    }

    public List<GroupMemberNotificationDto> getPendingNotificationsForUser(Long groupId, User user) {
        Long userId = groupAuthorizationService.requireAuthenticatedUserId(user);
        groupAuthorizationService.requireMember(groupId, userId);

        return groupMemberNotificationRepository
            .findAllByGroupIdAndTargetUserIdAndConfirmedAtIsNullOrderByCreatedAtDescIdDesc(groupId, userId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public GroupMemberNotificationDto confirmNotification(Long groupId, Long notificationId, User user) {
        Long userId = groupAuthorizationService.requireAuthenticatedUserId(user);
        groupAuthorizationService.requireMember(groupId, userId);

        GroupMemberNotification notification = groupMemberNotificationRepository.findByIdAndGroupIdForUpdate(notificationId, groupId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, NOTIFICATION_NOT_FOUND_MESSAGE));

        if (!notification.getTargetUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, NOTIFICATION_NOT_FOUND_MESSAGE);
        }

        if (notification.getConfirmedAt() == null) {
            notification.setConfirmedAt(Instant.now());
        }

        return toDto(notification);
    }

    public Map<Long, NotificationStatus> getLatestStatusByTargetUserId(Long groupId, Collection<Long> targetUserIds) {
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, NotificationStatus> statuses = new LinkedHashMap<>();
        for (Object[] row : groupMemberNotificationRepository.findRawStatusesByGroupIdAndTargetUserIds(groupId, targetUserIds)) {
            Long targetUserId = toLong(row[0]);
            boolean canReceiveNotification = toBoolean(row[1]);
            Instant lastNotificationSentAt = toInstant(row[2]);
            Instant lastNotificationConfirmedAt = toInstant(row[3]);
            statuses.put(
                targetUserId,
                new NotificationStatus(
                    canReceiveNotification,
                    lastNotificationSentAt != null && lastNotificationConfirmedAt == null,
                    lastNotificationSentAt,
                    lastNotificationConfirmedAt
                )
            );
        }

        return statuses;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private boolean toBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toInstant(ZoneOffset.UTC);
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant();
        }
        throw new IllegalArgumentException("Unbekannter Timestamp-Typ: " + value.getClass().getName());
    }

    private GroupMember requireTargetMembership(Long groupId, Long targetUserId) {
        if (!userRepository.existsById(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User nicht gefunden");
        }

        return groupMemberRepository.findByGroup_IdAndUser_IdAndActiveTrue(groupId, targetUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, TARGET_NOT_FOUND_MESSAGE));
    }

    private GroupMemberNotificationDto toDto(GroupMemberNotification notification) {
        return new GroupMemberNotificationDto(
            notification.getId(),
            notification.getGroupId(),
            notification.getActorUserId(),
            notification.getActorUsernameSnapshot(),
            notification.getTargetUserId(),
            notification.getMessage(),
            notification.getCreatedAt(),
            notification.getConfirmedAt()
        );
    }

    public record NotificationStatus(
        boolean canReceiveNotification,
        boolean hasPendingNotification,
        Instant lastNotificationSentAt,
        Instant lastNotificationConfirmedAt
    ) {
    }
}
