package com.kazka.auth.google;

import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthServiceTest {

    @Mock UserRepository users;
    @InjectMocks GoogleOAuthService service;

    @Test
    void should_returnExistingUser_when_googleSubjectMatches() {
        User existing = new User();
        existing.setId("u1");
        existing.setEmail("a@b.com");
        existing.setGoogleSubject("g-sub-1");
        when(users.findByGoogleSubject("g-sub-1")).thenReturn(Optional.of(existing));

        User result = service.linkOrCreate("g-sub-1", "a@b.com", "Name");

        assertThat(result).isSameAs(existing);
    }

    @Test
    void should_linkByEmail_when_subjectUnknownButEmailExists() {
        when(users.findByGoogleSubject("g-sub-2")).thenReturn(Optional.empty());
        User existing = new User();
        existing.setId("u2");
        existing.setEmail("a@b.com");
        when(users.findByEmail("a@b.com")).thenReturn(Optional.of(existing));
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.linkOrCreate("g-sub-2", "a@b.com", "Name");

        assertThat(result.getGoogleSubject()).isEqualTo("g-sub-2");
        assertThat(result.isEmailVerified()).isTrue();
    }

    @Test
    void should_createUser_when_emailUnknown() {
        when(users.findByGoogleSubject("g-sub-3")).thenReturn(Optional.empty());
        when(users.findByEmail("new@x.com")).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.linkOrCreate("g-sub-3", "new@x.com", "New Name");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(users).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getGoogleSubject()).isEqualTo("g-sub-3");
        assertThat(saved.getEmail()).isEqualTo("new@x.com");
        assertThat(saved.getDisplayName()).isEqualTo("New Name");
        assertThat(saved.getRole()).isEqualTo(UserRole.USER);
        assertThat(saved.isEmailVerified()).isTrue();
        assertThat(saved.getPasswordHash()).isNull();
    }
}
