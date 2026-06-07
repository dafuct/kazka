package com.kazka.billing;

import com.kazka.story.exception.PaywallRequiredException;
import com.kazka.usage.UsageProperties;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Enforces the monthly tale cap for every user. Admins are unlimited. */
@RequiredArgsConstructor
@Component
public class FreeTierGate {

    private final UsageProperties usage;
    private final UserRepository users;

    /** Throws PaywallRequiredException when a non-admin user has hit the monthly limit. */
    public void assertAllowed(User user) {
        if (user.getRole() == UserRole.ADMIN) return;
        int limit = usage.limitOrDefault();
        if (user.getStoriesThisMonth() >= limit) {
            throw new PaywallRequiredException(
                    "Monthly limit reached (" + limit + " tales per month)");
        }
    }

    /** Records a successful tale against the monthly counter. */
    public void recordUsage(String userId) {
        users.incrementStoriesThisMonth(userId);
    }
}
