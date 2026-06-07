package com.kazka.child;

import org.springframework.stereotype.Component;

@Component
public class ChildEntitlementResolver {

    public int maxChildProfiles(String userId) {
        return Integer.MAX_VALUE;
    }
}
