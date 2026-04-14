package com.bierliste.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bierliste.backend.model.ActivityType;
import com.bierliste.backend.model.GroupActivity;
import com.bierliste.backend.model.Group;
import com.bierliste.backend.model.GroupMember;
import com.bierliste.backend.model.GroupRole;
import com.bierliste.backend.model.RefreshToken;
import com.bierliste.backend.model.User;
import com.bierliste.backend.model.UserSettings;
import com.bierliste.backend.model.VerificationToken;
import com.bierliste.backend.repository.GroupActivityRepository;
import com.bierliste.backend.repository.GroupMemberRepository;
import com.bierliste.backend.repository.GroupRepository;
import com.bierliste.backend.repository.RefreshTokenRepository;
import com.bierliste.backend.repository.UserRepository;
import com.bierliste.backend.repository.UserSettingsRepository;
import com.bierliste.backend.repository.VerificationTokenRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private GroupActivityRepository groupActivityRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Test
    void deleteAccountDeletesUserCleansRelationsReassignsGroupsAndAnonymizesActivities() {
        User deletedUser = createVerifiedUser("delete-me@example.com", "DeleteMe");
        User remainingUser = createVerifiedUser("remaining@example.com", "Remaining");
        User secondUser = createVerifiedUser("second@example.com", "Second");

        Group deletedGroup = createGroup("Wird geloescht", deletedUser);
        createMembership(deletedGroup, deletedUser, GroupRole.ADMIN);

        Group promotedGroup = createGroup("Bleibt erhalten", deletedUser);
        createMembership(promotedGroup, deletedUser, GroupRole.ADMIN);
        createMembership(promotedGroup, remainingUser, GroupRole.MEMBER);

        Group sharedGroup = createGroup("Andere Gruppe", remainingUser);
        createMembership(sharedGroup, remainingUser, GroupRole.ADMIN);
        createMembership(sharedGroup, deletedUser, GroupRole.MEMBER);
        createMembership(sharedGroup, secondUser, GroupRole.MEMBER);

        Group previouslyLeftGroup = createGroup("Frueher verlassen", deletedUser);
        GroupMember inactiveCreatorMembership = createMembership(previouslyLeftGroup, deletedUser, GroupRole.ADMIN);
        inactiveCreatorMembership.setActive(false);
        inactiveCreatorMembership.setLeftAt(Instant.parse("2026-03-01T10:00:00Z"));
        groupMemberRepository.save(inactiveCreatorMembership);
        createMembership(previouslyLeftGroup, remainingUser, GroupRole.ADMIN);

        createActivity(promotedGroup.getId(), ActivityType.USER_JOINED_GROUP, deletedUser, deletedUser, Instant.parse("2026-03-21T10:00:00Z"));
        createActivity(promotedGroup.getId(), ActivityType.STRICH_INCREMENTED, remainingUser, deletedUser, Instant.parse("2026-03-21T10:05:00Z"));
        createActivity(sharedGroup.getId(), ActivityType.STRICH_INCREMENTED, deletedUser, secondUser, Instant.parse("2026-03-22T10:00:00Z"));
        createActivity(previouslyLeftGroup.getId(), ActivityType.USER_REMOVED_FROM_GROUP, remainingUser, deletedUser, Instant.parse("2026-03-23T10:00:00Z"));

        RefreshToken refreshToken = refreshTokenService.create(deletedUser);
        createVerificationToken(deletedUser, Instant.now().plusSeconds(3600));
        createUserSettings(deletedUser);

        userService.deleteAccount(deletedUser);

        assertThat(userRepository.findById(deletedUser.getId())).isEmpty();

        assertThat(groupRepository.findById(deletedGroup.getId())).isEmpty();
        Group persistedPromotedGroup = groupRepository.findById(promotedGroup.getId()).orElseThrow();
        Group persistedSharedGroup = groupRepository.findById(sharedGroup.getId()).orElseThrow();
        Group persistedPreviouslyLeftGroup = groupRepository.findById(previouslyLeftGroup.getId()).orElseThrow();
        assertThat(groupMemberRepository.findByGroup_IdAndUser_Id(promotedGroup.getId(), remainingUser.getId())).isPresent();
        assertThat(persistedPromotedGroup.getCreatedByUserId()).isNull();
        assertThat(persistedSharedGroup.getCreatedByUserId()).isEqualTo(remainingUser.getId());
        assertThat(persistedPreviouslyLeftGroup.getCreatedByUserId()).isNull();
        assertThat(groupMemberRepository.findAllByUser_Id(deletedUser.getId())).isEmpty();

        assertThat(refreshTokenRepository.findByToken(refreshToken.getToken())).isEmpty();
        assertThat(verificationTokenRepository.findByUserEmail("delete-me@example.com")).isEmpty();
        assertThat(userSettingsRepository.findAll()).isEmpty();

        GroupActivity promotedJoinActivity = groupActivityRepository.findAllByGroupIdOrderByTimestampDescIdDesc(promotedGroup.getId())
            .stream()
            .filter(activity -> activity.getType() == ActivityType.USER_JOINED_GROUP)
            .findFirst()
            .orElseThrow();
        assertThat(promotedJoinActivity.getActorUserId()).isNull();
        assertThat(promotedJoinActivity.getTargetUserId()).isNull();
        assertThat(promotedJoinActivity.getActorDisplayNameSnapshot()).isEqualTo(ActivityService.FORMER_MEMBER_DISPLAY_NAME);
        assertThat(promotedJoinActivity.getTargetDisplayNameSnapshot()).isEqualTo(ActivityService.FORMER_MEMBER_DISPLAY_NAME);

        GroupActivity promotedIncrementActivity = groupActivityRepository.findAllByGroupIdOrderByTimestampDescIdDesc(promotedGroup.getId())
            .stream()
            .filter(activity -> activity.getType() == ActivityType.STRICH_INCREMENTED)
            .findFirst()
            .orElseThrow();
        assertThat(promotedIncrementActivity.getActorUserId()).isEqualTo(remainingUser.getId());
        assertThat(promotedIncrementActivity.getTargetUserId()).isNull();
        assertThat(promotedIncrementActivity.getTargetDisplayNameSnapshot()).isEqualTo(ActivityService.FORMER_MEMBER_DISPLAY_NAME);

        GroupActivity sharedGroupActivity = groupActivityRepository.findAllByGroupIdOrderByTimestampDescIdDesc(sharedGroup.getId())
            .stream()
            .filter(activity -> activity.getType() == ActivityType.STRICH_INCREMENTED)
            .findFirst()
            .orElseThrow();
        assertThat(sharedGroupActivity.getActorUserId()).isNull();
        assertThat(sharedGroupActivity.getActorDisplayNameSnapshot()).isEqualTo(ActivityService.FORMER_MEMBER_DISPLAY_NAME);
        assertThat(sharedGroupActivity.getTargetUserId()).isEqualTo(secondUser.getId());

        GroupActivity previouslyLeftGroupActivity = groupActivityRepository.findAllByGroupIdOrderByTimestampDescIdDesc(previouslyLeftGroup.getId())
            .getFirst();
        assertThat(previouslyLeftGroupActivity.getActorUserId()).isEqualTo(remainingUser.getId());
        assertThat(previouslyLeftGroupActivity.getTargetUserId()).isNull();
        assertThat(previouslyLeftGroupActivity.getTargetDisplayNameSnapshot()).isEqualTo(ActivityService.FORMER_MEMBER_DISPLAY_NAME);
    }

    private User createVerifiedUser(String email, String username) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash("hashed");
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private Group createGroup(String name, User createdByUser) {
        Group group = new Group();
        group.setName(name);
        group.setCreatedByUser(createdByUser);
        return groupRepository.save(group);
    }

    private GroupMember createMembership(Group group, User user, GroupRole role) {
        GroupMember membership = new GroupMember();
        membership.setGroup(group);
        membership.setUser(user);
        membership.setRole(role);
        return groupMemberRepository.save(membership);
    }

    private VerificationToken createVerificationToken(User user, Instant expiryDate) {
        VerificationToken token = new VerificationToken();
        token.setUser(user);
        token.setCode("123456");
        token.setExpiryDate(expiryDate);
        return verificationTokenRepository.save(token);
    }

    private UserSettings createUserSettings(User user) {
        UserSettings settings = new UserSettings();
        settings.setUser(user);
        settings.setTheme("light");
        settings.setLastUpdated(Instant.now());
        return userSettingsRepository.save(settings);
    }

    private GroupActivity createActivity(Long groupId, ActivityType type, User actor, User target, Instant timestamp) {
        GroupActivity activity = new GroupActivity();
        activity.setGroupId(groupId);
        activity.setType(type);
        activity.setTimestamp(timestamp);
        activity.setActorUserId(actor.getId());
        activity.setActorDisplayNameSnapshot(actor.getUsername());
        activity.setTargetUserId(target != null ? target.getId() : null);
        activity.setTargetDisplayNameSnapshot(target != null ? target.getUsername() : null);
        return groupActivityRepository.save(activity);
    }
}
