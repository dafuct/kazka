package com.kazka.moderation;

import com.kazka.auth.AuthProperties;
import com.kazka.auth.MailService;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SuspensionServiceTest {

    private UserRepository users;
    private FlaggedAttemptRepository flags;
    private MailService mail;
    private AuthProperties authProps;
    private ModerationProperties modProps;
    private SuspensionService service;
    private User user;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        flags = mock(FlaggedAttemptRepository.class);
        mail = mock(MailService.class);
        authProps = new AuthProperties("http://localhost", "no-reply@kazka.local",
                new AuthProperties.TokenTtl(java.time.Duration.ofHours(24), java.time.Duration.ofHours(1)),
                new AuthProperties.Admin("admin@kazka.local", "x"));
        modProps = new ModerationProperties();
        service = new SuspensionService(users, flags, mail, authProps, modProps);

        user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail("user@example.com");
        user.setDisplayName("Test User");
        when(users.lockById(user.getId())).thenReturn(Optional.of(user));
    }

    @Test
    void should_throwAccountSuspended_when_userIsAlreadySuspended() {
        user.setSuspendedAt(Instant.now());
        assertThatThrownBy(() -> service.assertNotSuspended(user))
                .isInstanceOf(AccountSuspendedException.class);
    }

    @Test
    void should_doNothing_when_userIsNotSuspended() {
        service.assertNotSuspended(user);                 // does not throw
    }

    @Test
    void should_notSuspend_when_underThreshold() {
        when(flags.countCountableInWindow(eq(user.getId()), any(Instant.class))).thenReturn(2L);
        service.recordAndMaybeSuspend(user.getId(), ModerationPipeline.TEXT_INPUT,
                ModerationCategory.SEXUAL, "uk", "x", null, "guard");
        assertThat(user.getSuspendedAt()).isNull();
        verify(users, never()).save(user);                // user row not modified
        verify(mail, never()).sendAccountSuspendedEmail(anyString(), anyString());
    }

    @Test
    void should_suspendAndEmail_when_thresholdReached() {
        when(flags.countCountableInWindow(eq(user.getId()), any(Instant.class))).thenReturn(3L);
        service.recordAndMaybeSuspend(user.getId(), ModerationPipeline.TEXT_INPUT,
                ModerationCategory.SEXUAL, "uk", "x", null, "guard");
        assertThat(user.getSuspendedAt()).isNotNull();
        assertThat(user.getSuspendedReason()).isEqualTo("CONTENT_POLICY");
        assertThat(user.getSuspendedBy()).isNull();        // null = auto
        verify(users).save(user);
        verify(mail).sendAccountSuspendedEmail("user@example.com", "Test User");
        verify(mail).sendAdminSuspensionNotice("admin@kazka.local", "user@example.com");
    }

    @Test
    void should_recordImageSceneFlag_when_pipelineIsImage() {
        when(flags.countCountableInWindow(eq(user.getId()), any(Instant.class))).thenReturn(0L);
        service.recordAndMaybeSuspend(user.getId(), ModerationPipeline.IMAGE_SCENE,
                ModerationCategory.VIOLENCE, "uk", "scene text", null, "guard");
        verify(flags).save(any(FlaggedAttempt.class));     // attempt persisted
        // image-scene rows are excluded — no lock acquired, no user write, no email sent
        verify(users, never()).lockById(anyString());
        verify(users, never()).save(any(User.class));
        verify(mail, never()).sendAccountSuspendedEmail(anyString(), anyString());
    }

    @Test
    void should_suspendWithoutAdminEmail_when_adminConfigUnset() {
        AuthProperties propsNoAdmin = new AuthProperties("http://localhost", "no-reply@kazka.local",
                new AuthProperties.TokenTtl(java.time.Duration.ofHours(24), java.time.Duration.ofHours(1)),
                null);
        SuspensionService svc = new SuspensionService(users, flags, mail, propsNoAdmin, modProps);
        when(flags.countCountableInWindow(eq(user.getId()), any(Instant.class))).thenReturn(3L);

        svc.recordAndMaybeSuspend(user.getId(), ModerationPipeline.TEXT_INPUT,
                ModerationCategory.SEXUAL, "uk", "x", null, "guard");

        assertThat(user.getSuspendedAt()).isNotNull();
        verify(mail).sendAccountSuspendedEmail("user@example.com", "Test User");
        verify(mail, never()).sendAdminSuspensionNotice(anyString(), anyString());
    }

    @Test
    void should_suspendWithoutAdminEmail_when_adminEmailIsBlank() {
        AuthProperties propsBlankAdmin = new AuthProperties("http://localhost", "no-reply@kazka.local",
                new AuthProperties.TokenTtl(java.time.Duration.ofHours(24), java.time.Duration.ofHours(1)),
                new AuthProperties.Admin("  ", "x"));
        SuspensionService svc = new SuspensionService(users, flags, mail, propsBlankAdmin, modProps);
        when(flags.countCountableInWindow(eq(user.getId()), any(Instant.class))).thenReturn(3L);

        svc.recordAndMaybeSuspend(user.getId(), ModerationPipeline.TEXT_INPUT,
                ModerationCategory.SEXUAL, "uk", "x", null, "guard");

        assertThat(user.getSuspendedAt()).isNotNull();
        verify(mail, never()).sendAdminSuspensionNotice(anyString(), anyString());
    }

    @Test
    void should_skipSuspension_when_categoryIsJudgeUnavailable() {
        when(flags.countCountableInWindow(eq(user.getId()), any(Instant.class))).thenReturn(99L);
        service.recordAndMaybeSuspend(user.getId(), ModerationPipeline.TEXT_INPUT,
                ModerationCategory.JUDGE_UNAVAILABLE, "uk", "x", null, "guard");
        assertThat(user.getSuspendedAt()).isNull();
        verify(users, never()).save(user);
    }

    @Test
    void should_useFromAddress_when_sendingAdminNotice() {
        when(flags.countCountableInWindow(eq(user.getId()), any(Instant.class))).thenReturn(3L);
        service.recordAndMaybeSuspend(user.getId(), ModerationPipeline.TEXT_INPUT,
                ModerationCategory.SEXUAL, "uk", "x", null, "guard");
        verify(mail).sendAdminSuspensionNotice(eq("admin@kazka.local"), eq("user@example.com"));
    }
}
