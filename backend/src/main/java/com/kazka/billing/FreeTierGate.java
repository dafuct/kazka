package com.kazka.billing;

import com.kazka.story.exception.PaywallRequiredException;
import com.kazka.user.User;
import com.kazka.user.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class FreeTierGate {

    private final EntitlementResolver entitlements;
    private final BillingProperties props;
    private final UserRepository users;

    /** Throws PaywallRequiredException when a free-tier user has hit the monthly limit. No-op for Pro. */
    public void assertAllowed(User user) {
        if (entitlements.isPro(user.getId())) return;
        int limit = limit();
        if (user.getStoriesThisMonth() >= limit) {
            throw new PaywallRequiredException(
                    "Free tier limited to " + limit + " stories per month");
        }
    }

    /** Records a successful story creation against the free tier counter. No-op for Pro.
     *  Uses an atomic UPDATE to avoid lost-update races between concurrent generations. */
    public void recordUsage(String userId) {
        if (entitlements.isPro(userId)) return;
        users.incrementStoriesThisMonth(userId);
    }

    private int limit() {
        return props.freeMonthlyStoryLimit() == null ? 5 : props.freeMonthlyStoryLimit();
    }
}
