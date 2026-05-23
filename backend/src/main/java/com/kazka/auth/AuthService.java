package com.kazka.auth;

import com.kazka.auth.exception.EmailAlreadyExistsException;
import com.kazka.auth.token.RefreshTokenService;
import com.kazka.auth.token.TokenIssuer;
import com.kazka.auth.token.dto.TokenResponse;
import com.kazka.user.EmailVerificationToken;
import com.kazka.user.EmailVerificationTokenRepository;
import com.kazka.user.PasswordResetToken;
import com.kazka.user.PasswordResetTokenRepository;
import com.kazka.user.User;
import com.kazka.user.UserDto;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    public record SignupResult(UserDto user, TokenResponse tokens) {}

    private final UserRepository users;
    private final EmailVerificationTokenRepository emailTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final MailService mailService;
    private final SessionInvalidator sessionInvalidator;
    private final AuthProperties props;
    private final TokenIssuer tokenIssuer;
    private final RefreshTokenService refreshTokens;

    public AuthService(UserRepository users,
                       EmailVerificationTokenRepository emailTokens,
                       PasswordResetTokenRepository resetTokens,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       MailService mailService,
                       SessionInvalidator sessionInvalidator,
                       AuthProperties props,
                       TokenIssuer tokenIssuer,
                       RefreshTokenService refreshTokens) {
        this.users = users;
        this.emailTokens = emailTokens;
        this.resetTokens = resetTokens;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.mailService = mailService;
        this.sessionInvalidator = sessionInvalidator;
        this.props = props;
        this.tokenIssuer = tokenIssuer;
        this.refreshTokens = refreshTokens;
    }

    @Transactional
    public SignupResult signup(String email, String password, String displayName) {
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmail(normalized)) {
            throw new EmailAlreadyExistsException();
        }

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(normalized);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(displayName.trim());
        user.setRole(UserRole.USER);
        user.setEmailVerified(false);
        users.save(user);

        sendVerification(user);

        String access = tokenIssuer.issueAccessToken(user.getId(), user.getRole());
        String refresh = refreshTokens.issue(user.getId()).block();
        TokenResponse tokens = new TokenResponse(
                access, refresh,
                props.jwt().accessTtl().toSeconds(),
                UserDto.from(user));
        return new SignupResult(UserDto.from(user), tokens);
    }

    private void sendVerification(User user) {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(tokenService.generate());
        token.setUserId(user.getId());
        token.setExpiresAt(Instant.now().plus(props.tokenTtl().emailVerification()));
        emailTokens.save(token);

        try {
            mailService.sendVerificationEmail(user.getEmail(), user.getDisplayName(), token.getToken());
        } catch (Exception e) {
            log.warn("Verification email failed for user={}; user can resend later", user.getId());
        }
    }

    @Transactional
    public boolean verifyEmail(String token) {
        var opt = emailTokens.findById(token);
        if (opt.isEmpty()) return false;
        EmailVerificationToken evt = opt.get();
        if (evt.getConsumedAt() != null) return false;
        if (evt.getExpiresAt().isBefore(Instant.now())) return false;

        User user = users.findById(evt.getUserId()).orElse(null);
        if (user == null) return false;

        user.setEmailVerified(true);
        users.save(user);

        evt.setConsumedAt(Instant.now());
        emailTokens.save(evt);
        return true;
    }

    @Transactional
    public void resendVerification(String userId) {
        User user = users.findById(userId).orElseThrow();
        if (user.isEmailVerified()) return;
        emailTokens.consumeAllByUserId(userId, Instant.now());
        sendVerification(user);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        String normalized = email.trim().toLowerCase();
        var userOpt = users.findByEmail(normalized);
        if (userOpt.isEmpty()) return;
        User user = userOpt.get();
        if (user.getPasswordHash() == null) return;

        PasswordResetToken token = new PasswordResetToken();
        token.setToken(tokenService.generate());
        token.setUserId(user.getId());
        token.setExpiresAt(Instant.now().plus(props.tokenTtl().passwordReset()));
        resetTokens.save(token);

        try {
            mailService.sendPasswordResetEmail(user.getEmail(), user.getDisplayName(), token.getToken());
        } catch (Exception e) {
            log.warn("Password reset email failed for user={}", user.getId());
        }
    }

    @Transactional
    public void confirmPasswordReset(String token, String newPassword) {
        var opt = resetTokens.findById(token);
        if (opt.isEmpty()) throw new com.kazka.auth.exception.InvalidTokenException();
        PasswordResetToken prt = opt.get();
        if (prt.getConsumedAt() != null) throw new com.kazka.auth.exception.InvalidTokenException();
        if (prt.getExpiresAt().isBefore(Instant.now())) throw new com.kazka.auth.exception.InvalidTokenException();

        User user = users.findById(prt.getUserId())
                .orElseThrow(com.kazka.auth.exception.InvalidTokenException::new);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);

        prt.setConsumedAt(Instant.now());
        resetTokens.save(prt);

        sessionInvalidator.invalidateAllForUser(user.getId());
    }

    @Transactional(readOnly = true)
    public UserDto findCurrent(String userId) {
        return users.findById(userId).map(UserDto::from).orElseThrow();
    }

    @Transactional
    public UserDto updateDisplayName(String userId, String displayName) {
        User user = users.findById(userId).orElseThrow();
        user.setDisplayName(displayName.trim());
        return UserDto.from(users.save(user));
    }
}
