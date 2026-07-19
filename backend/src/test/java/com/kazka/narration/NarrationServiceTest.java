package com.kazka.narration;

import com.kazka.auth.CurrentUserResolver.CurrentUser;
import com.kazka.story.NarrationStatus;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.time.Duration.ofSeconds;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NarrationServiceTest {

    @Mock StoryRepository repository;
    @Mock TtsClient ttsClient;
    @Mock AudioStorage audioStorage;

    NarrationService service;

    private static final CurrentUser USER = new CurrentUser("user-1", UserRole.USER);
    private static final CurrentUser ADMIN = new CurrentUser("admin-1", UserRole.ADMIN);

    @BeforeEach
    void init() {
        service = new NarrationService(repository, ttsClient, audioStorage);
    }

    private Story story(String id, String ownerId, NarrationStatus status, String key) {
        Story s = new Story();
        s.setId(id);
        s.setUserId(ownerId);
        s.setContent("Жила собі лисичка.");
        s.setLanguage("uk");
        s.setNarrationStatus(status);
        s.setNarrationKey(key);
        return s;
    }

    private static TtsAudio mp3(byte... bytes) {
        return new TtsAudio(bytes, "audio/mpeg", "mp3");
    }

    @Test
    void should_returnReadyUrlWithoutCallingTts_when_statusReady() {
        Story s = story("s1", "user-1", NarrationStatus.READY, "narration/s1.mp3");
        when(repository.findByIdAndUserId("s1", "user-1")).thenReturn(Optional.of(s));
        when(audioStorage.urlFor("narration/s1.mp3")).thenReturn("/uploads/s1.mp3");

        NarrationResponse resp = service.requestNarration("s1", USER).block();

        assertThat(resp.status()).isEqualTo("READY");
        assertThat(resp.url()).isEqualTo("/uploads/s1.mp3");
        verifyNoInteractions(ttsClient);
    }

    @Test
    void should_synthesizeOnceAndMarkReady_when_statusNone() {
        Story s = story("s2", "user-1", NarrationStatus.NONE, null);
        when(repository.findByIdAndUserId("s2", "user-1")).thenReturn(Optional.of(s));
        when(repository.claimNarration("s2")).thenReturn(1);
        when(ttsClient.synthesize(any(), eq("uk"))).thenReturn(Mono.just(mp3((byte) 9, (byte) 9)));
        when(audioStorage.storeNarration(eq("s2"), any(), eq("audio/mpeg"), eq("mp3")))
                .thenReturn("narration/s2.mp3");
        when(repository.markNarrationReady("s2", "narration/s2.mp3")).thenReturn(1);

        NarrationResponse resp = service.requestNarration("s2", USER).block();

        assertThat(resp.status()).isEqualTo("GENERATING");
        await().atMost(ofSeconds(3)).untilAsserted(() ->
                verify(repository).markNarrationReady("s2", "narration/s2.mp3"));
        verify(ttsClient, times(1)).synthesize(any(), eq("uk"));
    }

    @Test
    void should_notTriggerTts_when_claimLost() {
        Story s = story("s3", "user-1", NarrationStatus.GENERATING, null);
        when(repository.findByIdAndUserId("s3", "user-1")).thenReturn(Optional.of(s));
        when(repository.claimNarration("s3")).thenReturn(0);

        NarrationResponse resp = service.requestNarration("s3", USER).block();

        assertThat(resp.status()).isEqualTo("GENERATING");
        verifyNoInteractions(ttsClient);
    }

    @Test
    void should_useFindByIdForAdmin_when_adminOpensOthersTale() {
        Story s = story("s4", "someone-else", NarrationStatus.NONE, null);
        when(repository.findById("s4")).thenReturn(Optional.of(s));
        when(repository.claimNarration("s4")).thenReturn(1);
        when(ttsClient.synthesize(any(), any())).thenReturn(Mono.just(mp3((byte) 2)));
        when(audioStorage.storeNarration(any(), any(), any(), any())).thenReturn("narration/s4.mp3");
        when(repository.markNarrationReady(any(), any())).thenReturn(1);

        NarrationResponse resp = service.requestNarration("s4", ADMIN).block();

        assertThat(resp.status()).isEqualTo("GENERATING");
        verify(repository).findById("s4");
        verify(repository, never()).findByIdAndUserId(any(), any());
        // Synthesis is fire-and-forget on boundedElastic; await its completion so the
        // synthesize/store/markReady stubs are consumed before strict-stubbing verification.
        await().atMost(ofSeconds(3)).untilAsserted(() ->
                verify(repository).markNarrationReady("s4", "narration/s4.mp3"));
    }

    @Test
    void should_passStoryLanguageToClient_when_englishTale() {
        Story s = story("s6", "user-1", NarrationStatus.NONE, null);
        s.setLanguage("en");
        when(repository.findByIdAndUserId("s6", "user-1")).thenReturn(Optional.of(s));
        when(repository.claimNarration("s6")).thenReturn(1);
        when(ttsClient.synthesize(any(), eq("en"))).thenReturn(Mono.just(mp3((byte) 7)));
        when(audioStorage.storeNarration(any(), any(), any(), any())).thenReturn("narration/s6.mp3");
        when(repository.markNarrationReady(any(), any())).thenReturn(1);

        service.requestNarration("s6", USER).block();

        await().atMost(ofSeconds(3)).untilAsserted(() -> verify(ttsClient).synthesize(any(), eq("en")));
    }

    @Test
    void should_markFailed_when_ttsThrows() {
        Story s = story("s5", "user-1", NarrationStatus.NONE, null);
        when(repository.findByIdAndUserId("s5", "user-1")).thenReturn(Optional.of(s));
        when(repository.claimNarration("s5")).thenReturn(1);
        when(ttsClient.synthesize(any(), any())).thenReturn(Mono.error(new RuntimeException("boom")));

        service.requestNarration("s5", USER).block();

        await().atMost(ofSeconds(3)).untilAsserted(() -> verify(repository).markNarrationFailed("s5"));
        verify(repository, never()).markNarrationReady(any(), any());
    }

    @Test
    void should_returnReadyUrl_when_getNarrationAndReady() {
        Story s = story("g1", "user-1", NarrationStatus.READY, "narration/g1.mp3");
        when(repository.findByIdAndUserId("g1", "user-1")).thenReturn(Optional.of(s));
        when(audioStorage.urlFor("narration/g1.mp3")).thenReturn("/uploads/g1.mp3");

        NarrationResponse resp = service.getNarration("g1", USER).block();

        assertThat(resp.status()).isEqualTo("READY");
        assertThat(resp.url()).isEqualTo("/uploads/g1.mp3");
    }

    @Test
    void should_returnNullUrl_when_getNarrationAndGenerating() {
        Story s = story("g2", "user-1", NarrationStatus.GENERATING, null);
        when(repository.findByIdAndUserId("g2", "user-1")).thenReturn(Optional.of(s));

        NarrationResponse resp = service.getNarration("g2", USER).block();

        assertThat(resp.status()).isEqualTo("GENERATING");
        assertThat(resp.url()).isNull();
        verifyNoInteractions(audioStorage);
    }
}
