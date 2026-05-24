package com.kazka.child;

import com.kazka.billing.EntitlementResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChildEntitlementResolverTest {

    @Mock EntitlementResolver entitlements;
    @InjectMocks ChildEntitlementResolver resolver;

    @Test
    void should_returnOne_when_freeTier_for_maxChildProfiles() {
        when(entitlements.isPro("u")).thenReturn(false);
        assertThat(resolver.maxChildProfiles("u")).isEqualTo(1);
    }

    @Test
    void should_returnUnlimited_when_proTier_for_maxChildProfiles() {
        when(entitlements.isPro("u")).thenReturn(true);
        assertThat(resolver.maxChildProfiles("u")).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void should_returnZero_when_freeTier_for_maxSavedCharacters() {
        when(entitlements.isPro("u")).thenReturn(false);
        assertThat(resolver.maxSavedCharacters("u")).isEqualTo(0);
    }

    @Test
    void should_returnUnlimited_when_proTier_for_maxSavedCharacters() {
        when(entitlements.isPro("u")).thenReturn(true);
        assertThat(resolver.maxSavedCharacters("u")).isEqualTo(Integer.MAX_VALUE);
    }
}
