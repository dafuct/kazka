package com.kazka.auth.apple;

import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppleOAuthServiceTest {

    @Mock UserRepository users;
    @InjectMocks AppleOAuthService service;

    @Test
    void should_returnExistingUser_when_appleSubjectAlreadyLinked() {
        User existing = newUser();
        existing.setAppleSubject("apple-sub-1");
        when(users.findByAppleSubject("apple-sub-1")).thenReturn(Optional.of(existing));

        User result = service.linkOrCreate("apple-sub-1", "ignored@privaterelay.appleid.com", null);

        assertThat(result).isSameAs(existing);
    }

    @Test
    void should_linkAppleSubject_when_emailMatchesExistingUser() {
        User existing = newUser();
        existing.setEmail("alice@example.com");
        when(users.findByAppleSubject("apple-sub-1")).thenReturn(Optional.empty());
        when(users.findByEmail("alice@example.com")).thenReturn(Optional.of(existing));
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.linkOrCreate("apple-sub-1", "alice@example.com", "Alice");

        assertThat(result.getAppleSubject()).isEqualTo("apple-sub-1");
        assertThat(result.isEmailVerified()).isTrue();
        verify(users).save(existing);
    }

    @Test
    void should_createNewUser_when_neitherSubjectNorEmailMatch() {
        when(users.findByAppleSubject("apple-sub-1")).thenReturn(Optional.empty());
        when(users.findByEmail("new@privaterelay.appleid.com")).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.linkOrCreate("apple-sub-1", "new@privaterelay.appleid.com", "New User");

        assertThat(result.getId()).isNotBlank();
        assertThat(result.getAppleSubject()).isEqualTo("apple-sub-1");
        assertThat(result.getEmail()).isEqualTo("new@privaterelay.appleid.com");
        assertThat(result.getAppleEmailRelay()).isEqualTo("new@privaterelay.appleid.com");
        assertThat(result.getDisplayName()).isEqualTo("New User");
        assertThat(result.isEmailVerified()).isTrue();
    }

    @Test
    void should_fallBackToAppleUser_when_displayNameAbsent() {
        when(users.findByAppleSubject(any())).thenReturn(Optional.empty());
        when(users.findByEmail(any())).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.linkOrCreate("apple-sub-2", "u@privaterelay.appleid.com", null);

        assertThat(result.getDisplayName()).isEqualTo("Apple user");
    }

    private User newUser() {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setRole(UserRole.USER);
        u.setDisplayName("Test");
        return u;
    }

    @Test
    void should_synthesisePlaceholderEmail_when_appleOmitsEmail() {
        when(users.findByAppleSubject("apple-sub-3")).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.linkOrCreate("apple-sub-3", null, "Hidden User");

        assertThat(result.getEmail()).isEqualTo("apple-sub-3@privaterelay.appleid.invalid");
        assertThat(result.getAppleSubject()).isEqualTo("apple-sub-3");
        assertThat(result.getAppleEmailRelay()).isNull();
    }
}
