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
    void should_allow_up_to_daily_limit_then_block_next() {
        String userId = "rl-user-" + System.nanoTime();
        for (int index = 0; index < ChildRateLimiter.DAILY_LIMIT; index++) {
            int call = index + 1;
            assertThatNoException()
                    .as("call " + call)
                    .isThrownBy(() -> limiter.assertAndIncrement(userId));
        }
        assertThatThrownBy(() -> limiter.assertAndIncrement(userId))
                .isInstanceOf(ChildRateLimiter.TooManyChildCreatesException.class);
    }

    @Test
    void should_reserve_batch_count_in_single_increment() {
        String userId = "rl-batch-" + System.nanoTime();

        assertThatNoException()
                .as("batch fitting within the limit")
                .isThrownBy(() -> limiter.assertAndIncrement(userId, ChildRateLimiter.DAILY_LIMIT));

        assertThatThrownBy(() -> limiter.assertAndIncrement(userId, 1))
                .isInstanceOf(ChildRateLimiter.TooManyChildCreatesException.class);
    }

    @Test
    void should_block_batch_that_exceeds_daily_limit() {
        String userId = "rl-over-" + System.nanoTime();

        assertThatThrownBy(() -> limiter.assertAndIncrement(userId, ChildRateLimiter.DAILY_LIMIT + 1))
                .isInstanceOf(ChildRateLimiter.TooManyChildCreatesException.class);

        // Fail-closed: the rejected batch still consumed the window (INCRBY runs before the throw),
        // so a subsequent single reservation is still over the ceiling and must also be blocked.
        assertThatThrownBy(() -> limiter.assertAndIncrement(userId, 1))
                .isInstanceOf(ChildRateLimiter.TooManyChildCreatesException.class);
    }
}
