package com.kazka.billing;

import com.kazka.story.exception.PaywallRequiredException;
import com.kazka.usage.UsageProperties;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FreeTierGateTest {

    @Mock UserRepository users;

    private User user(UserRole role, int count) {
        User u = new User();
        u.setRole(role);
        u.setStoriesThisMonth(count);
        return u;
    }

    @Test
    void should_block_nonAdmin_when_atLimit() {
        FreeTierGate gate = new FreeTierGate(new UsageProperties(30), users);
        assertThatThrownBy(() -> gate.assertAllowed(user(UserRole.USER, 30)))
                .isInstanceOf(PaywallRequiredException.class);
    }

    @Test
    void should_allow_nonAdmin_when_belowLimit() {
        FreeTierGate gate = new FreeTierGate(new UsageProperties(30), users);
        assertThatCode(() -> gate.assertAllowed(user(UserRole.USER, 29)))
                .doesNotThrowAnyException();
    }

    @Test
    void should_allow_admin_when_overLimit() {
        FreeTierGate gate = new FreeTierGate(new UsageProperties(30), users);
        assertThatCode(() -> gate.assertAllowed(user(UserRole.ADMIN, 999)))
                .doesNotThrowAnyException();
    }

    @Test
    void should_useDefaultLimit_when_propertyNull() {
        FreeTierGate gate = new FreeTierGate(new UsageProperties(null), users);
        assertThatThrownBy(() -> gate.assertAllowed(user(UserRole.USER, 30)))
                .isInstanceOf(PaywallRequiredException.class);
        assertThatCode(() -> gate.assertAllowed(user(UserRole.USER, 29)))
                .doesNotThrowAnyException();
    }

    @Test
    void should_increment_counter_when_recording_usage() {
        FreeTierGate gate = new FreeTierGate(new UsageProperties(30), users);
        gate.recordUsage("u1");
        verify(users).incrementStoriesThisMonth("u1");
    }
}
