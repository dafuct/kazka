package com.kazka.child;

import com.kazka.AbstractIT;
import com.kazka.child.dto.CreateChildProfileRequest;
import com.kazka.story.exception.PaywallRequiredException;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Locks in the headline atomicity guarantee of {@link ChildProfileService#createBatch}: if any
 * single creation in the batch fails (here, by exceeding the entitlement limit), the whole
 * {@code @Transactional} batch rolls back and persists nothing.
 *
 * <p>The {@link ChildEntitlementResolver} is mocked to a limit of 1, so the second {@code create()}
 * inside a 2-element batch throws {@link PaywallRequiredException} after the first has already been
 * inserted within the same transaction — exercising the rollback. This is deliberately a separate
 * class from {@code ChildProfileControllerIT} so the class-level mock does not affect that class's
 * "unlimited children" tests.
 */
@Tag("integration")
class ChildProfileBatchAtomicityIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired ChildProfileRepository profiles;
    @Autowired ChildProfileService service;
    @MockitoBean ChildEntitlementResolver tier;

    @Test
    void should_rollback_entire_batch_when_one_child_exceeds_limit() {
        when(tier.maxChildProfiles(any())).thenReturn(1);
        String userId = seedUser();

        List<CreateChildProfileRequest> batch = List.of(
                new CreateChildProfileRequest("First", null, null, "uk", List.of()),
                new CreateChildProfileRequest("Second", null, null, "uk", List.of()));

        // The second create() exceeds the mocked limit of 1 and throws, rolling back the batch.
        assertThatThrownBy(() -> service.createBatch(userId, batch))
                .isInstanceOf(PaywallRequiredException.class);

        // Atomicity: nothing from the failed batch survived for this user.
        assertThat(profiles.countByUserIdAndArchivedAtIsNull(userId)).isZero();
        assertThat(service.listActive(userId)).isEmpty();
    }

    private String seedUser() {
        String id = UUID.randomUUID().toString();
        User user = new User();
        user.setId(id);
        user.setEmail(id + "@test.example");
        user.setDisplayName("Tester");
        user.setPasswordHash("x");
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        users.save(user);
        return id;
    }
}
