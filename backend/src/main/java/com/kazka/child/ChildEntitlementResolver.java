package com.kazka.child;

import com.kazka.billing.EntitlementResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ChildEntitlementResolver {

    static final int FREE_MAX_CHILD_PROFILES = 1;
    static final int FREE_MAX_SAVED_CHARACTERS_PER_PROFILE = 0;

    private final EntitlementResolver entitlements;

    public int maxChildProfiles(String userId) {
        return entitlements.isPro(userId) ? Integer.MAX_VALUE : FREE_MAX_CHILD_PROFILES;
    }

    public int maxSavedCharacters(String userId) {
        return entitlements.isPro(userId) ? Integer.MAX_VALUE : FREE_MAX_SAVED_CHARACTERS_PER_PROFILE;
    }

    public boolean canIncludeCharacters(String userId) {
        return entitlements.isPro(userId);
    }
}
