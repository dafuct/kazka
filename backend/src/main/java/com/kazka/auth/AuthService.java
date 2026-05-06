package com.kazka.auth;

import com.kazka.auth.exception.EmailAlreadyExistsException;
import com.kazka.user.EmailVerificationToken;
import com.kazka.user.EmailVerificationTokenRepository;
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

    private final UserRepository users;
    private final EmailVerificationTokenRepository emailTokens;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final MailService mailService;
    private final AuthProperties props;

    public AuthService(UserRepository users,
                       EmailVerificationTokenRepository emailTokens,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       MailService mailService,
                       AuthProperties props) {
        this.users = users;
        this.emailTokens = emailTokens;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.mailService = mailService;
        this.props = props;
    }

    @Transactional
    public UserDto signup(String email, String password, String displayName) {
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
        return UserDto.from(user);
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
            log.warn("Verification email failed for {}; user can resend later", user.getEmail());
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
}
