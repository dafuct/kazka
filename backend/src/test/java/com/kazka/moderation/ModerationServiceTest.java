package com.kazka.moderation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ModerationServiceTest {

    private ModerationJudgeClient guard;
    private ReactiveStringRedisTemplate redis;
    private ReactiveValueOperations<String, String> ops;
    private ModerationProperties props;
    private ModerationService service;

    @BeforeEach
    void setUp() {
        guard = mock(ModerationJudgeClient.class);
        redis = mock(ReactiveStringRedisTemplate.class);
        ops = mock(ReactiveValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn(Mono.empty());                              // default: cache miss
        when(ops.set(anyString(), anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        props = new ModerationProperties();
        service = new ModerationService(guard, redis, props);
    }

    @Test
    void should_returnAllowed_when_judgeReturnsAllowed() {
        when(guard.classify(anyString(), anyString(), any())).thenReturn(ModerationResult.Allowed.INSTANCE);
        ModerationResult r = service.checkInput("uk", "happy bears", List.of("Sofia"));
        assertThat(r).isInstanceOf(ModerationResult.Allowed.class);
    }

    @Test
    void should_returnRefused_when_judgeReturnsRefused() {
        when(guard.classify(anyString(), anyString(), any()))
                .thenReturn(ModerationResult.Refused.of(ModerationCategory.SEXUAL));
        ModerationResult r = service.checkInput("uk", "naked princess", List.of());
        assertThat(((ModerationResult.Refused) r).category()).isEqualTo(ModerationCategory.SEXUAL);
    }

    @Test
    void should_useCachedResult_when_sameNormalizedPromptSeenAgain() {
        when(ops.get(anyString())).thenReturn(Mono.just("ALLOWED"));
        ModerationResult r = service.checkInput("uk", " Happy   Bears ", List.of("Sofia"));
        assertThat(r).isInstanceOf(ModerationResult.Allowed.class);
        verifyNoInteractions(guard);
    }

    @Test
    void should_writeToCache_when_judgeResolvesFreshResult() {
        when(guard.classify(anyString(), anyString(), any())).thenReturn(ModerationResult.Allowed.INSTANCE);
        service.checkInput("uk", "x", List.of());
        verify(ops, times(1)).set(anyString(), eq("ALLOWED"), eq(props.getCacheTtl()));
    }

    @Test
    void should_returnJudgeUnavailable_when_clientThrows() {
        when(guard.classify(anyString(), anyString(), any())).thenThrow(new RuntimeException("boom"));
        ModerationResult r = service.checkInput("uk", "x", List.of());
        assertThat(((ModerationResult.Refused) r).category()).isEqualTo(ModerationCategory.JUDGE_UNAVAILABLE);
    }

    @Test
    void should_keyCacheByLanguage_when_samePromptInDifferentLocales() {
        when(guard.classify(eq("uk"), anyString(), any())).thenReturn(ModerationResult.Allowed.INSTANCE);
        when(guard.classify(eq("en"), anyString(), any()))
                .thenReturn(ModerationResult.Refused.of(ModerationCategory.SEXUAL));
        ModerationResult uk = service.checkInput("uk", "x", List.of());
        ModerationResult en = service.checkInput("en", "x", List.of());
        assertThat(uk).isInstanceOf(ModerationResult.Allowed.class);
        assertThat(((ModerationResult.Refused) en).category()).isEqualTo(ModerationCategory.SEXUAL);
    }
}
