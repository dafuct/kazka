package com.kazka.moderation;

import com.kazka.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Import(ModerationServiceCacheIT.MockConfig.class)
class ModerationServiceCacheIT extends AbstractIT {

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        ModerationJudgeClient mockGuard() {
            return mock(ModerationJudgeClient.class);
        }
    }

    @Autowired ModerationService service;
    @Autowired ReactiveStringRedisTemplate redis;
    @Autowired ModerationJudgeClient guard;

    @Test
    void should_consultJudgeOnlyOnce_when_samePromptCheckedTwice() {
        when(guard.classify(anyString(), anyString(), any())).thenReturn(ModerationResult.Allowed.INSTANCE);
        // clear any prior cache entries and reset mock call count
        redis.delete(redis.keys("kazka:moderation:*")).block();
        clearInvocations(guard);

        service.checkInput("uk", "пригоди трьох ведмежат", List.of("Sofia"));
        service.checkInput("uk", "пригоди трьох ведмежат", List.of("Sofia"));

        verify(guard, times(1)).classify(eq("uk"), anyString(), any());
    }
}
