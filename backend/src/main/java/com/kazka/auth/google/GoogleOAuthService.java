package com.kazka.auth.google;

import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class GoogleOAuthService {

    private final UserRepository users;

    @Transactional
    public User linkOrCreate(String googleSubject, String email, boolean emailVerified, String displayName) {
        var bySubject = users.findByGoogleSubject(googleSubject);
        if (bySubject.isPresent()) return bySubject.get();

        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        if (normalizedEmail != null) {
            var byEmail = users.findByEmail(normalizedEmail);
            if (byEmail.isPresent()) {
                User existingUser = byEmail.get();
                existingUser.setGoogleSubject(googleSubject);
                existingUser.setEmailVerified(emailVerified || existingUser.isEmailVerified());
                return users.save(existingUser);
            }
        }

        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("Google id_token had no email claim");
        }

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(normalizedEmail);
        user.setGoogleSubject(googleSubject);
        user.setDisplayName(displayName == null || displayName.isBlank()
                ? "Google user"
                : displayName.trim());
        user.setRole(UserRole.USER);
        user.setEmailVerified(emailVerified);
        return users.save(user);
    }
}
