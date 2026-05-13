package com.kazka.auth;

import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuth2SuccessHandlerTest {

    private final UserRepository users = mock(UserRepository.class);
    private final AuthProperties props = new AuthProperties(
            "http://localhost", "from@example.com",
            new AuthProperties.TokenTtl(java.time.Duration.ofHours(24), java.time.Duration.ofHours(1)),
            new AuthProperties.Admin("", ""),
            null, null, null);
    private final OAuth2SuccessHandler handler = new OAuth2SuccessHandler(users, props);

    @Test
    void should_returnExistingUser_when_googleSubjectFound() {
        User existing = userOf("a@example.com", "sub-1", null);
        when(users.findByGoogleSubject("sub-1")).thenReturn(Optional.of(existing));

        User resolved = handler.resolveOrCreateUser("sub-1", "a@example.com", "Alice");

        assertThat(resolved).isSameAs(existing);
    }

    @Test
    void should_linkAndVerify_when_emailFoundWithoutGoogleSubject() {
        User existing = userOf("b@example.com", null, null);
        existing.setEmailVerified(false);
        when(users.findByGoogleSubject("sub-2")).thenReturn(Optional.empty());
        when(users.findByEmail("b@example.com")).thenReturn(Optional.of(existing));
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User resolved = handler.resolveOrCreateUser("sub-2", "b@example.com", "Bob");

        assertThat(resolved.getGoogleSubject()).isEqualTo("sub-2");
        assertThat(resolved.isEmailVerified()).isTrue();
    }

    @Test
    void should_createUser_when_neitherSubjectNorEmailFound() {
        when(users.findByGoogleSubject("sub-3")).thenReturn(Optional.empty());
        when(users.findByEmail("c@example.com")).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User resolved = handler.resolveOrCreateUser("sub-3", "c@example.com", "Carol");

        assertThat(resolved.getEmail()).isEqualTo("c@example.com");
        assertThat(resolved.getGoogleSubject()).isEqualTo("sub-3");
        assertThat(resolved.isEmailVerified()).isTrue();
        assertThat(resolved.getRole()).isEqualTo(UserRole.USER);
        assertThat(resolved.getDisplayName()).isEqualTo("Carol");
    }

    private User userOf(String email, String sub, String hash) {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail(email);
        u.setGoogleSubject(sub);
        u.setPasswordHash(hash);
        u.setDisplayName(email);
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        return u;
    }
}
