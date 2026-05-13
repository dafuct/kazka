package com.kazka.billing;

import com.kazka.story.exception.PaywallRequiredException;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class FreeTierGate {

    private final EntitlementResolver entitlements;
    private final BillingProperties props;
    private final UserRepository users;

    public FreeTierGate(EntitlementResolver entitlements, BillingProperties props, UserRepository users) {
        this.entitlements = entitlements;
        this.props = props;
        this.users = users;
    }

    /** Throws PaywallRequiredException when a free-tier user has hit the monthly limit. No-op for Pro. */
    public void assertAllowed(User user) {
        if (entitlements.isPro(user.getId())) return;
        int limit = limit();
        if (user.getStoriesThisMonth() >= limit) {
            throw new PaywallRequiredException(
                    "Free tier limited to " + limit + " stories per month");
        }
    }

    /** Records a successful story creation against the free tier counter. No-op for Pro. */
    public void recordUsage(String userId) {
        if (entitlements.isPro(userId)) return;
        User u = users.findById(userId).orElseThrow();
        u.setStoriesThisMonth(u.getStoriesThisMonth() + 1);
        users.save(u);
    }

    private int limit() {
        return props.freeMonthlyStoryLimit() == null ? 3 : props.freeMonthlyStoryLimit();
    }
}
