package com.kazka.auth.apple;

import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class AppleOAuthService {

    private final UserRepository users;

    @Transactional
    public User linkOrCreate(String appleSubject, String email, String displayName) {
        var bySubject = users.findByAppleSubject(appleSubject);
        if (bySubject.isPresent()) return bySubject.get();

        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        if (normalizedEmail != null) {
            var byEmail = users.findByEmail(normalizedEmail);
            if (byEmail.isPresent()) {
                User user = byEmail.get();
                user.setAppleSubject(appleSubject);
                user.setAppleEmailRelay(normalizedEmail);
                user.setEmailVerified(true);
                return users.save(user);
            }
        }

        String storedEmail = normalizedEmail != null
                ? normalizedEmail
                : appleSubject + "@privaterelay.appleid.invalid";

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(storedEmail);
        user.setAppleSubject(appleSubject);
        user.setAppleEmailRelay(normalizedEmail);  // null if Apple omitted email
        user.setDisplayName(displayName == null || displayName.isBlank()
                ? "Apple user"
                : displayName.trim());
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        return users.save(user);
    }
}
