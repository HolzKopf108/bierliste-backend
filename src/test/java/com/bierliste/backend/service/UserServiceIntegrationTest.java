package com.bierliste.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bierliste.backend.model.Group;
import com.bierliste.backend.model.GroupMember;
import com.bierliste.backend.model.GroupRole;
import com.bierliste.backend.model.RefreshToken;
import com.bierliste.backend.model.User;
import com.bierliste.backend.model.UserSettings;
import com.bierliste.backend.model.VerificationToken;
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
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Test
    void deleteAccountAnonymizesUserCleansTokensAndGroups() {
        User deletedUser = createVerifiedUser("delete-me@example.com", "DeleteMe");
        User remainingUser = createVerifiedUser("remaining@example.com", "Remaining");

        Group deletedGroup = createGroup("Wird geloescht", deletedUser);
        createMembership(deletedGroup, deletedUser, GroupRole.ADMIN);

        Group promotedGroup = createGroup("Bleibt erhalten", deletedUser);
        createMembership(promotedGroup, deletedUser, GroupRole.ADMIN);
        createMembership(promotedGroup, remainingUser, GroupRole.MEMBER);

        RefreshToken refreshToken = refreshTokenService.create(deletedUser);
        createVerificationToken(deletedUser, Instant.now().plusSeconds(3600));
        createUserSettings(deletedUser);

        userService.deleteAccount(deletedUser);

        User anonymizedUser = userRepository.findById(deletedUser.getId()).orElseThrow();
        assertThat(anonymizedUser.isDeleted()).isTrue();
        assertThat(anonymizedUser.getEmail()).isEqualTo("deleted-user-" + deletedUser.getId() + "@deleted.local");
        assertThat(anonymizedUser.getUsername()).isEqualTo("Gelöschter Benutzer");
        assertThat(anonymizedUser.isGoogleUser()).isFalse();

        assertThat(groupRepository.findById(deletedGroup.getId())).isEmpty();
        assertThat(groupRepository.findById(promotedGroup.getId())).isPresent();
        assertThat(groupMemberRepository.findByGroup_IdAndUser_Id(promotedGroup.getId(), deletedUser.getId())).isEmpty();
        GroupMember promotedMembership = groupMemberRepository.findByGroup_IdAndUser_Id(promotedGroup.getId(), remainingUser.getId()).orElseThrow();
        assertThat(promotedMembership.getRole()).isEqualTo(GroupRole.ADMIN);

        assertThat(refreshTokenRepository.findByToken(refreshToken.getToken())).isEmpty();
        assertThat(verificationTokenRepository.findByUserEmail("delete-me@example.com")).isEmpty();
        assertThat(userSettingsRepository.findByUser(anonymizedUser)).isEmpty();
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
}
