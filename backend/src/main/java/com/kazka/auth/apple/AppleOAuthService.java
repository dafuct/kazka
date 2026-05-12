package com.kazka.auth.apple;

import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AppleOAuthService {

    private final UserRepository users;

    public AppleOAuthService(UserRepository users) {
        this.users = users;
    }

    @Transactional
    public User linkOrCreate(String appleSubject, String email, String displayName) {
        var bySubject = users.findByAppleSubject(appleSubject);
        if (bySubject.isPresent()) return bySubject.get();

        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        if (normalizedEmail != null) {
            var byEmail = users.findByEmail(normalizedEmail);
            if (byEmail.isPresent()) {
                User u = byEmail.get();
                u.setAppleSubject(appleSubject);
                u.setAppleEmailRelay(normalizedEmail);
                u.setEmailVerified(true);
                return users.save(u);
            }
        }

        String storedEmail = normalizedEmail != null
                ? normalizedEmail
                : appleSubject + "@privaterelay.appleid.invalid";

        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail(storedEmail);
        u.setAppleSubject(appleSubject);
        u.setAppleEmailRelay(normalizedEmail);  // null if Apple omitted email
        u.setDisplayName(displayName == null || displayName.isBlank()
                ? "Apple user"
                : displayName.trim());
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        return users.save(u);
    }
}
