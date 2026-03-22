package com.bierliste.backend.service;

import com.bierliste.backend.dto.GroupActivitiesResponseDto;
import com.bierliste.backend.dto.GroupActivityDto;
import com.bierliste.backend.dto.GroupActivityUserDto;
import com.bierliste.backend.dto.GroupSettingsChangedGroupActivityDto;
import com.bierliste.backend.dto.GroupSettingsSnapshotDto;
import com.bierliste.backend.dto.InviteCreatedGroupActivityDto;
import com.bierliste.backend.dto.InviteUsedGroupActivityDto;
import com.bierliste.backend.dto.MoneyDeductedGroupActivityDto;
import com.bierliste.backend.dto.RoleGrantedWartGroupActivityDto;
import com.bierliste.backend.dto.RoleRevokedWartGroupActivityDto;
import com.bierliste.backend.dto.StricheDeductedGroupActivityDto;
import com.bierliste.backend.dto.StrichIncrementedGroupActivityDto;
import com.bierliste.backend.dto.UserJoinedGroupActivityDto;
import com.bierliste.backend.dto.UserLeftGroupActivityDto;
import com.bierliste.backend.dto.UserRemovedFromGroupActivityDto;
import com.bierliste.backend.model.ActivityType;
import com.bierliste.backend.model.GroupActivity;
import com.bierliste.backend.model.GroupActivityBookingMode;
import com.bierliste.backend.model.GroupInvitePermission;
import com.bierliste.backend.model.GroupRole;
import com.bierliste.backend.model.GroupSettingsChangedField;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.GroupActivityRepository;
import com.bierliste.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ActivityService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;
    private static final int META_VERSION = 1;

    private final GroupActivityRepository groupActivityRepository;
    private final UserRepository userRepository;
    private final GroupAuthorizationService groupAuthorizationService;

    public ActivityService(
        GroupActivityRepository groupActivityRepository,
        UserRepository userRepository,
        GroupAuthorizationService groupAuthorizationService
    ) {
        this.groupActivityRepository = groupActivityRepository;
        this.userRepository = userRepository;
        this.groupAuthorizationService = groupAuthorizationService;
    }

    @Transactional
    public void log(Long groupId, ActivityType type, Long actorUserId, Long targetUserId, Map<String, Object> meta) {
        User actor = userRepository.findById(actorUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User nicht gefunden"));
        User target = targetUserId == null
            ? null
            : userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User nicht gefunden"));

        saveActivity(groupId, type, actor, target, meta);
    }

    @Transactional
    public void log(Long groupId, ActivityType type, User actor, User target, Map<String, Object> meta) {
        saveActivity(groupId, type, actor, target, meta);
    }

    public void logUserJoinedGroup(Long groupId, User user) {
        logUserJoinedGroup(groupId, user, null);
    }

    public void logUserJoinedGroup(Long groupId, User user, String via) {
        if (via == null || via.isBlank()) {
            log(groupId, ActivityType.USER_JOINED_GROUP, user, user, Map.of());
            return;
        }

        log(groupId, ActivityType.USER_JOINED_GROUP, user, user, Map.of("via", via));
    }

    public void logUserLeftGroup(Long groupId, User user) {
        log(groupId, ActivityType.USER_LEFT_GROUP, user, user, Map.of());
    }

    public void logStrichIncremented(Long groupId, User actor, User target, int amount) {
        LinkedHashMap<String, Object> meta = new LinkedHashMap<>();
        meta.put("amount", amount);
        meta.put("mode", requireUserId(actor).equals(requireUserId(target))
            ? GroupActivityBookingMode.SELF.name()
            : GroupActivityBookingMode.OTHER.name());
        log(groupId, ActivityType.STRICH_INCREMENTED, actor, target, meta);
    }

    public void logMoneyDeducted(Long groupId, User actor, User target, BigDecimal amountMoney, BigDecimal pricePerStrich) {
        if (amountMoney == null || amountMoney.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        LinkedHashMap<String, Object> meta = new LinkedHashMap<>();
        meta.put("amountMoney", amountMoney);
        if (pricePerStrich != null) {
            meta.put("pricePerStrich", pricePerStrich);
        }

        log(groupId, ActivityType.MONEY_DEDUCTED, actor, target, meta);
    }

    public void logStricheDeducted(Long groupId, User actor, User target, int amountStriche) {
        if (amountStriche <= 0) {
            return;
        }

        LinkedHashMap<String, Object> meta = new LinkedHashMap<>();
        meta.put("amountStriche", amountStriche);
        log(groupId, ActivityType.STRICHE_DEDUCTED, actor, target, meta);
    }

    public void logRoleGrantedWart(Long groupId, User actor, User target, GroupRole previousRole, GroupRole newRole) {
        if (previousRole == newRole) {
            return;
        }

        LinkedHashMap<String, Object> meta = new LinkedHashMap<>();
        meta.put("previousRole", previousRole.name());
        meta.put("newRole", newRole.name());
        log(groupId, ActivityType.ROLE_GRANTED_WART, actor, target, meta);
    }

    public void logRoleRevokedWart(Long groupId, User actor, User target, GroupRole previousRole, GroupRole newRole) {
        if (previousRole == newRole) {
            return;
        }

        LinkedHashMap<String, Object> meta = new LinkedHashMap<>();
        meta.put("previousRole", previousRole.name());
        meta.put("newRole", newRole.name());
        log(groupId, ActivityType.ROLE_REVOKED_WART, actor, target, meta);
    }

    public void logGroupSettingsChanged(
        Long groupId,
        User actor,
        List<String> changedFields,
        Map<String, Object> oldValues,
        Map<String, Object> newValues
    ) {
        if (changedFields == null || changedFields.isEmpty()) {
            return;
        }

        LinkedHashMap<String, Object> meta = new LinkedHashMap<>();
        meta.put("changedFields", new ArrayList<>(changedFields));
        meta.put("old", new LinkedHashMap<>(oldValues));
        meta.put("new", new LinkedHashMap<>(newValues));
        log(groupId, ActivityType.GROUP_SETTINGS_CHANGED, actor, null, meta);
    }

    public GroupActivitiesResponseDto getActivities(Long groupId, String cursor, Integer limit, User user) {
        groupAuthorizationService.requireMember(groupId, user);

        int resolvedLimit = resolveLimit(limit);
        ActivityCursor activityCursor = decodeCursor(cursor);
        PageRequest pageRequest = PageRequest.of(0, resolvedLimit + 1);

        List<GroupActivity> activities = activityCursor.hasCursor()
            ? groupActivityRepository.findPageByGroupIdBeforeCursor(
                groupId,
                activityCursor.timestamp(),
                activityCursor.id(),
                pageRequest
            )
            : groupActivityRepository.findByGroupIdOrderByTimestampDescIdDesc(groupId, pageRequest);

        boolean hasMore = activities.size() > resolvedLimit;
        List<GroupActivity> pageItems = hasMore ? activities.subList(0, resolvedLimit) : activities;
        String nextCursor = hasMore ? encodeCursor(pageItems.getLast()) : null;

        return new GroupActivitiesResponseDto(
            pageItems.stream().map(this::toDto).toList(),
            nextCursor
        );
    }

    private void saveActivity(Long groupId, ActivityType type, User actor, User target, Map<String, Object> meta) {
        GroupActivity activity = new GroupActivity();
        activity.setGroupId(groupId);
        activity.setType(type);
        activity.setActorUserId(requireUserId(actor));
        activity.setActorUsernameSnapshot(actor.getUsername());
        activity.setTargetUserId(target != null ? requireUserId(target) : null);
        activity.setTargetUsernameSnapshot(target != null ? target.getUsername() : null);
        activity.setMetaVersion(META_VERSION);
        activity.setMeta(meta);
        groupActivityRepository.save(activity);
    }

    private Long requireUserId(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User muss persistiert sein");
        }
        return user.getId();
    }

    private int resolveLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }

        if (limit < 1 || limit > MAX_LIMIT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit muss zwischen 1 und 100 liegen");
        }

        return limit;
    }

    private ActivityCursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return new ActivityCursor(null, null);
        }

        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Cursor ist ungueltig");
            }

            Instant timestamp = Instant.parse(parts[0]);
            long id = Long.parseLong(parts[1]);
            if (id <= 0) {
                throw new IllegalArgumentException("Cursor ist ungueltig");
            }

            return new ActivityCursor(timestamp, id);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ungueltiger Cursor");
        }
    }

    private String encodeCursor(GroupActivity activity) {
        String rawCursor = activity.getTimestamp() + "|" + activity.getId();
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(rawCursor.getBytes(StandardCharsets.UTF_8));
    }

    private GroupActivityDto toDto(GroupActivity activity) {
        return switch (activity.getType()) {
            case STRICH_INCREMENTED -> toStrichIncrementedDto(activity);
            case STRICHE_DEDUCTED -> toStricheDeductedDto(activity);
            case MONEY_DEDUCTED -> toMoneyDeductedDto(activity);
            case USER_JOINED_GROUP -> applyBaseFields(activity, new UserJoinedGroupActivityDto());
            case USER_LEFT_GROUP -> applyBaseFields(activity, new UserLeftGroupActivityDto());
            case ROLE_GRANTED_WART -> toRoleGrantedWartDto(activity);
            case ROLE_REVOKED_WART -> toRoleRevokedWartDto(activity);
            case GROUP_SETTINGS_CHANGED -> toGroupSettingsChangedDto(activity);
            case USER_REMOVED_FROM_GROUP -> applyBaseFields(activity, new UserRemovedFromGroupActivityDto());
            case INVITE_CREATED -> applyBaseFields(activity, new InviteCreatedGroupActivityDto());
            case INVITE_USED -> applyBaseFields(activity, new InviteUsedGroupActivityDto());
        };
    }

    private StrichIncrementedGroupActivityDto toStrichIncrementedDto(GroupActivity activity) {
        Map<String, Object> meta = activity.getMeta();
        StrichIncrementedGroupActivityDto dto = applyBaseFields(activity, new StrichIncrementedGroupActivityDto());
        dto.setAmount(readRequiredInteger(meta, "amount", activity.getType()));
        dto.setMode(readRequiredBookingMode(meta, "mode", activity.getType()));
        return dto;
    }

    private StricheDeductedGroupActivityDto toStricheDeductedDto(GroupActivity activity) {
        Map<String, Object> meta = activity.getMeta();
        StricheDeductedGroupActivityDto dto = applyBaseFields(activity, new StricheDeductedGroupActivityDto());
        dto.setAmountStriche(readRequiredInteger(meta, "amountStriche", activity.getType()));
        return dto;
    }

    private MoneyDeductedGroupActivityDto toMoneyDeductedDto(GroupActivity activity) {
        Map<String, Object> meta = activity.getMeta();
        MoneyDeductedGroupActivityDto dto = applyBaseFields(activity, new MoneyDeductedGroupActivityDto());
        dto.setAmountMoney(readRequiredBigDecimal(meta, "amountMoney", activity.getType()));
        dto.setPricePerStrich(readOptionalBigDecimal(meta, "pricePerStrich"));
        return dto;
    }

    private RoleGrantedWartGroupActivityDto toRoleGrantedWartDto(GroupActivity activity) {
        Map<String, Object> meta = activity.getMeta();
        RoleGrantedWartGroupActivityDto dto = applyBaseFields(activity, new RoleGrantedWartGroupActivityDto());
        dto.setPreviousRole(readRequiredGroupRole(meta, "previousRole", activity.getType()));
        dto.setNewRole(readRequiredGroupRole(meta, "newRole", activity.getType()));
        return dto;
    }

    private RoleRevokedWartGroupActivityDto toRoleRevokedWartDto(GroupActivity activity) {
        Map<String, Object> meta = activity.getMeta();
        RoleRevokedWartGroupActivityDto dto = applyBaseFields(activity, new RoleRevokedWartGroupActivityDto());
        dto.setPreviousRole(readRequiredGroupRole(meta, "previousRole", activity.getType()));
        dto.setNewRole(readRequiredGroupRole(meta, "newRole", activity.getType()));
        return dto;
    }

    private GroupSettingsChangedGroupActivityDto toGroupSettingsChangedDto(GroupActivity activity) {
        Map<String, Object> meta = activity.getMeta();
        GroupSettingsChangedGroupActivityDto dto = applyBaseFields(activity, new GroupSettingsChangedGroupActivityDto());
        dto.setChangedFields(readChangedFields(meta, activity.getType()));
        dto.setOldSettings(toGroupSettingsSnapshotDto(readMap(meta, "old")));
        dto.setNewSettings(toGroupSettingsSnapshotDto(readMap(meta, "new")));
        return dto;
    }

    private <T extends GroupActivityDto> T applyBaseFields(GroupActivity activity, T dto) {
        dto.setId(activity.getId());
        dto.setTimestamp(activity.getTimestamp());
        dto.setType(activity.getType());
        dto.setActor(new GroupActivityUserDto(activity.getActorUserId(), activity.getActorUsernameSnapshot()));
        dto.setTarget(activity.getTargetUserId() == null
            ? null
            : new GroupActivityUserDto(activity.getTargetUserId(), activity.getTargetUsernameSnapshot()));
        return dto;
    }

    private int readRequiredInteger(Map<String, Object> meta, String key, ActivityType activityType) {
        Object value = meta.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw invalidActivityPayload(activityType, key);
    }

    private BigDecimal readRequiredBigDecimal(Map<String, Object> meta, String key, ActivityType activityType) {
        BigDecimal value = readOptionalBigDecimal(meta, key);
        if (value != null) {
            return value;
        }
        throw invalidActivityPayload(activityType, key);
    }

    private BigDecimal readOptionalBigDecimal(Map<String, Object> meta, String key) {
        Object value = meta.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value instanceof String stringValue) {
            return new BigDecimal(stringValue);
        }
        throw new IllegalStateException("Ungueltiger numerischer Activity-Wert fuer " + key);
    }

    private GroupRole readRequiredGroupRole(Map<String, Object> meta, String key, ActivityType activityType) {
        Object value = meta.get(key);
        if (value instanceof GroupRole groupRole) {
            return groupRole;
        }
        if (value instanceof String stringValue) {
            return GroupRole.valueOf(stringValue);
        }
        throw invalidActivityPayload(activityType, key);
    }

    private GroupActivityBookingMode readRequiredBookingMode(Map<String, Object> meta, String key, ActivityType activityType) {
        Object value = meta.get(key);
        if (value instanceof GroupActivityBookingMode bookingMode) {
            return bookingMode;
        }
        if (value instanceof String stringValue) {
            return GroupActivityBookingMode.valueOf(stringValue);
        }
        throw invalidActivityPayload(activityType, key);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(Map<String, Object> meta, String key) {
        Object value = meta.get(key);
        if (value == null) {
            return Collections.emptyMap();
        }
        if (value instanceof Map<?, ?> mapValue) {
            return (Map<String, Object>) mapValue;
        }
        throw new IllegalStateException("Ungueltiges Activity-Objekt fuer " + key);
    }

    @SuppressWarnings("unchecked")
    private List<GroupSettingsChangedField> readChangedFields(Map<String, Object> meta, ActivityType activityType) {
        Object value = meta.get("changedFields");
        if (!(value instanceof List<?> rawList)) {
            throw invalidActivityPayload(activityType, "changedFields");
        }

        List<GroupSettingsChangedField> changedFields = new ArrayList<>();
        for (Object item : rawList) {
            if (item instanceof GroupSettingsChangedField changedField) {
                changedFields.add(changedField);
                continue;
            }
            if (item instanceof String stringValue) {
                changedFields.add(toGroupSettingsChangedField(stringValue));
                continue;
            }
            throw invalidActivityPayload(activityType, "changedFields");
        }
        return changedFields;
    }

    private GroupSettingsSnapshotDto toGroupSettingsSnapshotDto(Map<String, Object> values) {
        GroupSettingsSnapshotDto dto = new GroupSettingsSnapshotDto();
        dto.setName(readOptionalString(values, "name"));
        dto.setPricePerStrich(readOptionalBigDecimal(values, "pricePerStrich"));
        dto.setOnlyWartsCanBookForOthers(readOptionalBoolean(values, "onlyWartsCanBookForOthers"));
        dto.setAllowArbitraryMoneySettlements(readOptionalBoolean(values, "allowArbitraryMoneySettlements"));
        dto.setInvitePermission(readOptionalGroupInvitePermission(values, "invitePermission"));
        return dto;
    }

    private String readOptionalString(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String stringValue) {
            return stringValue;
        }
        throw new IllegalStateException("Ungueltiger String-Activity-Wert fuer " + key);
    }

    private Boolean readOptionalBoolean(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        throw new IllegalStateException("Ungueltiger Boolean-Activity-Wert fuer " + key);
    }

    private GroupInvitePermission readOptionalGroupInvitePermission(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof GroupInvitePermission invitePermission) {
            return invitePermission;
        }
        if (value instanceof String stringValue) {
            return GroupInvitePermission.valueOf(stringValue);
        }
        throw new IllegalStateException("Ungültiger InvitePermission-Activity-Wert für " + key);
    }

    private GroupSettingsChangedField toGroupSettingsChangedField(String value) {
        return switch (value) {
            case "name", "NAME" -> GroupSettingsChangedField.NAME;
            case "pricePerStrich", "PRICE_PER_STRICH" -> GroupSettingsChangedField.PRICE_PER_STRICH;
            case "onlyWartsCanBookForOthers", "ONLY_WARTS_CAN_BOOK_FOR_OTHERS" ->
                GroupSettingsChangedField.ONLY_WARTS_CAN_BOOK_FOR_OTHERS;
            case "allowArbitraryMoneySettlements", "ALLOW_ARBITRARY_MONEY_SETTLEMENTS" ->
                GroupSettingsChangedField.ALLOW_ARBITRARY_MONEY_SETTLEMENTS;
            case "invitePermission", "INVITE_PERMISSION" -> GroupSettingsChangedField.INVITE_PERMISSION;
            default -> throw new IllegalStateException("Unbekanntes GroupSettings-Feld: " + value);
        };
    }

    private IllegalStateException invalidActivityPayload(ActivityType activityType, String key) {
        return new IllegalStateException("Ungueltiges Activity-Payload fuer " + activityType + ": " + key);
    }

    public List<String> determineChangedFields(Map<String, Object> oldValues, Map<String, Object> newValues) {
        List<String> changedFields = new ArrayList<>();
        for (Map.Entry<String, Object> entry : newValues.entrySet()) {
            if (!areEqual(oldValues.get(entry.getKey()), entry.getValue())) {
                changedFields.add(entry.getKey());
            }
        }
        return changedFields;
    }

    private boolean areEqual(Object left, Object right) {
        if (left instanceof BigDecimal leftDecimal && right instanceof BigDecimal rightDecimal) {
            return leftDecimal.compareTo(rightDecimal) == 0;
        }
        return Objects.equals(left, right);
    }

    private record ActivityCursor(Instant timestamp, Long id) {

        private boolean hasCursor() {
            return timestamp != null && id != null;
        }
    }
}
