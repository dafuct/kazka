package com.kazka.auth.google;

import com.kazka.user.User;
import com.kazka.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleOAuthService {

    private final UserRepository users;

    public GoogleOAuthService(UserRepository users) {
        this.users = users;
    }

    @Transactional
    public User linkOrCreate(String googleSubject, String email, boolean emailVerified, String displayName) {
        var bySubject = users.findByGoogleSubject(googleSubject);
        if (bySubject.isPresent()) return bySubject.get();

        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        if (normalizedEmail != null) {
            var byEmail = users.findByEmail(normalizedEmail);
            if (byEmail.isPresent()) {
                User u = byEmail.get();
                u.setGoogleSubject(googleSubject);
                u.setEmailVerified(emailVerified || u.isEmailVerified());
                return users.save(u);
            }
        }

        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("Google id_token had no email claim");
        }

        User u = new User();
        u.setId(java.util.UUID.randomUUID().toString());
        u.setEmail(normalizedEmail);
        u.setGoogleSubject(googleSubject);
        u.setDisplayName(displayName == null || displayName.isBlank()
                ? "Google user"
                : displayName.trim());
        u.setRole(com.kazka.user.UserRole.USER);
        u.setEmailVerified(emailVerified);
        return users.save(u);
    }
}
