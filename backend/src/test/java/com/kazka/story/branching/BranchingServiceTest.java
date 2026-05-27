package com.kazka.story.branching;

import com.kazka.billing.EntitlementResolver;
import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileService;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.story.branching.dto.BranchingChoice;
import com.kazka.story.branching.dto.BranchingResponse;
import com.kazka.story.branching.dto.BranchingStartRequest;
import com.kazka.story.exception.PaywallRequiredException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchingServiceTest {

    @Mock StoryRepository stories;
    @Mock ChildProfileService childProfiles;
    @Mock com.kazka.child.CharacterRepository characters;
    @Mock EntitlementResolver entitlements;
    @Mock com.kazka.hf.HuggingFaceClient hfClient;
    @Mock BranchingPromptBuilder promptBuilder;
    @Mock com.kazka.child.CharacterExtractionWorker extractionWorker;
    @Mock com.kazka.story.PromptBuilder systemPromptBuilder;
    @InjectMocks BranchingService svc;

    private ChildProfile profile() {
        ChildProfile p = new ChildProfile();
        p.setId("p1"); p.setUserId("u1"); p.setName("Лія"); p.setPreferredLanguage("uk");
        return p;
    }

    private com.kazka.auth.CurrentUserResolver.CurrentUser user(String id) {
        return new com.kazka.auth.CurrentUserResolver.CurrentUser(id, com.kazka.user.UserRole.USER);
    }

    @Test
    void start_throws_PaywallRequired_when_free_tier() {
        when(childProfiles.requireOwned("p1", "u1")).thenReturn(profile());
        when(entitlements.isPro("u1")).thenReturn(false);

        BranchingStartRequest req = new BranchingStartRequest(
                "адвенчер", List.of("дракон"), "6-8", "short", "uk", "p1", List.of());

        assertThatThrownBy(() -> svc.start(req, user("u1")))
                .isInstanceOf(PaywallRequiredException.class);
    }

    @Test
    void start_creates_story_in_awaiting_choice_1_state() {
        when(childProfiles.requireOwned("p1", "u1")).thenReturn(profile());
        when(entitlements.isPro("u1")).thenReturn(true);
        when(systemPromptBuilder.buildStorySystem(anyString())).thenReturn("system prompt");
        when(promptBuilder.buildOpeningUserMessage(any(), any(), any())).thenReturn("opening prompt");
        when(hfClient.streamText(anyString(), anyString())).thenReturn(Flux.just(
                "Opening body.\n\n---\n\nCHOICE_A: Option A\nCHOICE_B: Option B"));
        when(stories.save(any(Story.class))).thenAnswer(i -> i.getArgument(0));

        BranchingResponse resp = svc.start(
                new BranchingStartRequest("адвенчер", List.of("дракон"), "6-8", "short", "uk", "p1", List.of()),
                user("u1")).block();

        assertThat(resp.segmentNumber()).isEqualTo(1);
        assertThat(resp.branchingState()).isEqualTo("awaiting_choice_1");
        assertThat(resp.isFinal()).isFalse();
        assertThat(resp.choices()).hasSize(2);
        assertThat(resp.content()).isEqualTo("Opening body.");
    }

    @Test
    void choose_rejects_unknown_choice_id_with_400() {
        Story s = new Story();
        s.setId("s1"); s.setUserId("u1");
        s.setBranching(true);
        s.setBranchingState("awaiting_choice_1");
        s.setPendingChoices(List.of(
                new BranchingChoice("A", "Option A"),
                new BranchingChoice("B", "Option B")));
        when(stories.findById("s1")).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> svc.choose("s1", "C", user("u1")).block())
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void choose_rejects_when_story_already_complete_with_400() {
        Story s = new Story();
        s.setId("s1"); s.setUserId("u1");
        s.setBranching(true);
        s.setBranchingState("complete");
        when(stories.findById("s1")).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> svc.choose("s1", "A", user("u1")).block())
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void choose_returns_404_when_story_not_owned() {
        Story s = new Story();
        s.setId("s1"); s.setUserId("other-user");
        s.setBranching(true);
        s.setBranchingState("awaiting_choice_1");
        when(stories.findById("s1")).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> svc.choose("s1", "A", user("u1")).block())
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
