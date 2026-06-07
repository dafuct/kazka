package com.kazka.child;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChildEntitlementResolverTest {

    private final ChildEntitlementResolver resolver = new ChildEntitlementResolver();

    @Test
    void should_allow_unlimited_child_profiles_for_any_user() {
        assertThat(resolver.maxChildProfiles("any-user")).isEqualTo(Integer.MAX_VALUE);
    }
}
