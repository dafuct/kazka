package com.kazka.story;

import com.kazka.ai.AiClient;
import com.kazka.auth.CurrentUserResolver.CurrentUser;
import com.kazka.comics.ComicsBuilder;
import com.kazka.comics.StoryPanelRepository;
import com.kazka.illustration.ImageUrlResolver;
import com.kazka.moderation.ModerationProperties;
import com.kazka.moderation.ModerationService;
import com.kazka.moderation.SuspensionService;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoryServiceRetryTest {

    @Mock StoryRepository repository;
    @Mock UserRepository users;
    @Mock AiClient aiClient;
    @Mock PromptBuilder promptBuilder;
    @Mock ModerationService moderationService;
    @Mock SuspensionService suspensionService;
    @Mock ModerationProperties moderationProperties;
    @Mock com.kazka.billing.FreeTierGate freeTier;
    @Mock com.kazka.child.ChildProfileService childProfiles;
    @Mock com.kazka.child.CharacterRepository characters;
    @Mock com.kazka.child.StoryCharacterRepository storyCharacters;
    @Mock com.kazka.child.ChildEntitlementResolver childTier;
    @Mock com.kazka.child.CharacterExtractionWorker extractionWorker;
    @Mock ImageUrlResolver images;
    @Mock ComicsBuilder comicsBuilder;
    @Mock StoryPanelRepository panelRepository;

    @InjectMocks StoryService service;

    private final CurrentUser user = new CurrentUser("u1", UserRole.USER);

    private void verifiedUser() {
        User u = new User();
        u.setId("u1");
        u.setEmailVerified(true);
        when(users.findById("u1")).thenReturn(Optional.of(u));
    }

    private Story storyWith(IllustrationStatus status) {
        Story s = new Story();
        s.setId("s1");
        s.setUserId("u1");
        s.setIllustrationStatus(status);
        return s;
    }

    @Test
    void should_retry_when_storyPending() {
        verifiedUser();
        when(repository.findByIdAndUserId("s1", "u1"))
                .thenReturn(Optional.of(storyWith(IllustrationStatus.PENDING)));
        when(comicsBuilder.retry("s1")).thenReturn(Mono.empty());

        StepVerifier.create(service.retry("s1", user)).verifyComplete();

        verify(comicsBuilder).retry("s1");
    }

    @Test
    void should_retry_when_storyFailed() {
        verifiedUser();
        when(repository.findByIdAndUserId("s1", "u1"))
                .thenReturn(Optional.of(storyWith(IllustrationStatus.FAILED)));
        when(comicsBuilder.retry("s1")).thenReturn(Mono.empty());

        StepVerifier.create(service.retry("s1", user)).verifyComplete();

        verify(comicsBuilder).retry("s1");
    }

    @Test
    void should_reject_when_storyAlreadyReady() {
        verifiedUser();
        when(repository.findByIdAndUserId("s1", "u1"))
                .thenReturn(Optional.of(storyWith(IllustrationStatus.READY)));
        lenient().when(comicsBuilder.retry(eq("s1"))).thenReturn(Mono.empty());

        StepVerifier.create(service.retry("s1", user))
                .expectError(ResponseStatusException.class)
                .verify();
    }
}
