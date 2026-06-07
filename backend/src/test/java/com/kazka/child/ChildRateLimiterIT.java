package com.kazka.child;

import com.kazka.AbstractIT;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("integration")
class ChildRateLimiterIT extends AbstractIT {

    @Autowired ChildRateLimiter limiter;

    @Test
    void should_allow_up_to_five_creates_then_block_sixth() {
        String userId = "rl-user-" + System.nanoTime();
        for (int index = 0; index < 5; index++) {
            int call = index + 1;
            assertThatNoException()
                    .as("call " + call)
                    .isThrownBy(() -> limiter.assertAndIncrement(userId));
        }
        assertThatThrownBy(() -> limiter.assertAndIncrement(userId))
                .isInstanceOf(ChildRateLimiter.TooManyChildCreatesException.class);
    }
}
