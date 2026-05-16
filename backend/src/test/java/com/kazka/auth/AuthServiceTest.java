package com.kazka.auth;

import com.kazka.auth.exception.EmailAlreadyExistsException;
import com.kazka.auth.token.RefreshTokenService;
import com.kazka.auth.token.TokenIssuer;
import com.kazka.user.EmailVerificationTokenRepository;
import com.kazka.user.PasswordResetTokenRepository;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock UserRepository users;
    @Mock EmailVerificationTokenRepository emailTokens;
    @Mock PasswordResetTokenRepository resetTokens;
    @Mock PasswordEncoder passwordEncoder;
    @Mock TokenService tokenService;
    @Mock MailService mailService;
    @Mock SessionInvalidator sessionInvalidator;
    @Mock AuthProperties props;
    @Mock TokenIssuer tokenIssuer;
    @Mock RefreshTokenService refreshTokens;

    @InjectMocks AuthService authService;

    @Test
    void should_returnSignupResultWithTokens_when_validSignup() {
        when(users.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("pw12345678")).thenReturn("HASH");
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenService.generate()).thenReturn("verify-token");
        when(tokenIssuer.issueAccessToken(anyString(), eq(UserRole.USER)))
                .thenReturn("ACCESS");
        when(refreshTokens.issue(anyString())).thenReturn(Mono.just("REFRESH"));
        when(props.jwt()).thenReturn(new AuthProperties.Jwt(
                "secretsecretsecretsecretsecretsecret",
                Duration.ofHours(1), Duration.ofDays(30), "kazka"));
        when(props.tokenTtl()).thenReturn(new AuthProperties.TokenTtl(
                Duration.ofHours(24), Duration.ofHours(1)));

        AuthService.SignupResult result = authService.signup(
                "user@example.com", "pw12345678", "Display Name");

        assertThat(result.user().email()).isEqualTo("user@example.com");
        assertThat(result.user().displayName()).isEqualTo("Display Name");
        assertThat(result.user().role()).isEqualTo(UserRole.USER);
        assertThat(result.tokens().accessToken()).isEqualTo("ACCESS");
        assertThat(result.tokens().refreshToken()).isEqualTo("REFRESH");
        assertThat(result.tokens().accessExpiresInSeconds()).isEqualTo(3600L);
        verify(mailService).sendVerificationEmail(eq("user@example.com"), any(), eq("verify-token"));
    }

    @Test
    void should_throw_when_emailAlreadyExists() {
        when(users.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup("dup@example.com", "pw12345678", "Dup"))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(users, never()).save(any(User.class));
        verify(tokenIssuer, never()).issueAccessToken(anyString(), any());
    }
}
