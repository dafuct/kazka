package com.kazka.story.branching;

import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileService;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.story.branching.dto.BranchingChoice;
import com.kazka.story.branching.dto.BranchingResponse;
import com.kazka.story.branching.dto.BranchingStartRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchingServiceTest {

    @Mock StoryRepository stories;
    @Mock ChildProfileService childProfiles;
    @Mock com.kazka.child.CharacterRepository characters;
    @Mock com.kazka.ai.AiClient aiClient;
    @Mock BranchingPromptBuilder promptBuilder;
    @Mock com.kazka.child.CharacterExtractionWorker extractionWorker;
    @Mock com.kazka.comics.ComicsBuilder comicsBuilder;
    @InjectMocks BranchingService svc;

    private ChildProfile profile() {
        ChildProfile profile = new ChildProfile();
        profile.setId("p1"); profile.setUserId("u1"); profile.setName("Лія"); profile.setPreferredLanguage("uk");
        return profile;
    }

    private com.kazka.auth.CurrentUserResolver.CurrentUser user(String id) {
        return new com.kazka.auth.CurrentUserResolver.CurrentUser(id, com.kazka.user.UserRole.USER);
    }

    private com.kazka.auth.CurrentUserResolver.CurrentUser admin(String id) {
        return new com.kazka.auth.CurrentUserResolver.CurrentUser(id, com.kazka.user.UserRole.ADMIN);
    }

    @Test
    void start_creates_story_for_any_user() {
        when(childProfiles.requireOwned("p1", "u1")).thenReturn(profile());
        when(promptBuilder.buildBranchingSystem()).thenReturn("branching system");
        when(promptBuilder.buildOpeningUserMessage(any(), any(), any())).thenReturn("opening prompt");
        when(aiClient.generateStoryJson(anyString(), anyString())).thenReturn(Mono.just(
                "{\"segment\":\"Opening body.\",\"choiceA\":\"Option A\",\"choiceB\":\"Option B\"}"));
        when(stories.save(any(Story.class))).thenAnswer(i -> i.getArgument(0));

        BranchingResponse resp = svc.start(
                new BranchingStartRequest("адвенчер", List.of("дракон"), "6-8", "short", "uk", "p1", List.of()),
                user("u1")).block();

        assertThat(resp).isNotNull();
        assertThat(resp.segmentNumber()).isEqualTo(1);
        assertThat(resp.branchingState()).isEqualTo("awaiting_choice_1");
        assertThat(resp.isFinal()).isFalse();
        assertThat(resp.choices()).hasSize(2);
        assertThat(resp.content()).isEqualTo("Opening body.");
    }

    @Test
    void start_creates_story_in_awaiting_choice_1_state() {
        when(childProfiles.requireOwned("p1", "u1")).thenReturn(profile());
        when(promptBuilder.buildBranchingSystem()).thenReturn("branching system");
        when(promptBuilder.buildOpeningUserMessage(any(), any(), any())).thenReturn("opening prompt");
        when(aiClient.generateStoryJson(anyString(), anyString())).thenReturn(Mono.just(
                "{\"segment\":\"Opening body.\",\"choiceA\":\"Option A\",\"choiceB\":\"Option B\"}"));
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
    void start_takes_title_from_json_and_keeps_body_clean() {
        // Structured JSON: the title is its own field, the segment is pure prose — the title must
        // become the story title and never appear inside the tale body.
        when(childProfiles.requireOwned("p1", "u1")).thenReturn(profile());
        when(promptBuilder.buildBranchingSystem()).thenReturn("branching system");
        when(promptBuilder.buildOpeningUserMessage(any(), any(), any())).thenReturn("opening prompt");
        when(aiClient.generateStoryJson(anyString(), anyString())).thenReturn(Mono.just(
                "{\"title\":\"Матвійко та Червона Машинка\","
                        + "\"segment\":\"Жив-був Матвійко, що катав червону машинку.\","
                        + "\"choiceA\":\"Піти в сад\",\"choiceB\":\"Лишитися вдома\"}"));
        when(stories.save(any(Story.class))).thenAnswer(i -> i.getArgument(0));

        BranchingResponse resp = svc.start(
                new BranchingStartRequest("машинка", List.of("Матвійко"), "3-5", "short", "uk", "p1", List.of()),
                user("u1")).block();

        ArgumentCaptor<Story> saved = ArgumentCaptor.forClass(Story.class);
        verify(stories).save(saved.capture());
        assertThat(saved.getValue().getTitle()).isEqualTo("Матвійко та Червона Машинка");
        assertThat(resp.content())
                .isEqualTo("Жив-був Матвійко, що катав червону машинку.")
                .doesNotContain("Червона Машинка", "CHOICE_", "title", "segment");
    }

    @Test
    void start_scrubs_a_leaked_label_even_inside_the_json_segment() {
        // Belt-and-suspenders: even if the model sneaks a "Ukrainian:" label into the segment
        // field, cleanBody strips it so it never reaches the reader.
        when(childProfiles.requireOwned("p1", "u1")).thenReturn(profile());
        when(promptBuilder.buildBranchingSystem()).thenReturn("branching system");
        when(promptBuilder.buildOpeningUserMessage(any(), any(), any())).thenReturn("opening prompt");
        when(aiClient.generateStoryJson(anyString(), anyString())).thenReturn(Mono.just(
                "{\"title\":\"Матвій і машинка\",\"segment\":\"Ukrainian: Матвійко ступив на стежку.\","
                        + "\"choiceA\":\"Піти в ліс\",\"choiceB\":\"Лишитися\"}"));
        when(stories.save(any(Story.class))).thenAnswer(i -> i.getArgument(0));

        BranchingResponse resp = svc.start(
                new BranchingStartRequest("машинка", List.of("Матвійко"), "3-5", "short", "uk", "p1", List.of()),
                user("u1")).block();

        assertThat(resp.content()).isEqualTo("Матвійко ступив на стежку.").doesNotContain("Ukrainian");
    }

    @Test
    void choose_rejects_unknown_choice_id_with_400() {
        Story story = new Story();
        story.setId("s1"); story.setUserId("u1");
        story.setBranching(true);
        story.setBranchingState("awaiting_choice_1");
        story.setPendingChoices(List.of(
                new BranchingChoice("A", "Option A"),
                new BranchingChoice("B", "Option B")));
        when(stories.findByIdAndUserId("s1", "u1")).thenReturn(Optional.of(story));

        assertThatThrownBy(() -> svc.choose("s1", "C", user("u1")).block())
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void choose_rejects_when_story_already_complete_with_400() {
        Story story = new Story();
        story.setId("s1"); story.setUserId("u1");
        story.setBranching(true);
        story.setBranchingState("complete");
        when(stories.findByIdAndUserId("s1", "u1")).thenReturn(Optional.of(story));

        assertThatThrownBy(() -> svc.choose("s1", "A", user("u1")).block())
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void choose_returns_404_when_regular_user_does_not_own_story() {
        // A non-admin only ever sees their own rows: findByIdAndUserId yields nothing.
        when(stories.findByIdAndUserId("s1", "u1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.choose("s1", "A", user("u1")).block())
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void choose_allows_admin_to_advance_a_non_owned_story() {
        // Regression: an admin opening another user's in-progress branching tale
        // could read it (admin read-bypass) but choosing 404'd. Admins drive any tale.
        Story story = new Story();
        story.setId("s1"); story.setUserId("owner"); story.setChildProfileId("p1");
        story.setLanguage("uk"); story.setContent("Opening body.");
        story.setBranching(true);
        story.setBranchingState("awaiting_choice_1");
        story.setPendingChoices(List.of(
                new BranchingChoice("A", "Option A"),
                new BranchingChoice("B", "Option B")));
        when(stories.findById("s1")).thenReturn(Optional.of(story));
        // child is resolved by the STORY owner, not the admin caller
        when(childProfiles.requireOwned("p1", "owner")).thenReturn(profile());
        when(promptBuilder.buildBranchingSystem()).thenReturn("branching system");
        when(promptBuilder.buildMiddleUserMessage(anyString(), anyString(), anyString())).thenReturn("middle prompt");
        when(aiClient.generateStoryJson(anyString(), anyString())).thenReturn(Mono.just(
                "{\"segment\":\"Middle body.\",\"choiceA\":\"A2\",\"choiceB\":\"B2\"}"));
        when(stories.save(any(Story.class))).thenAnswer(i -> i.getArgument(0));

        BranchingResponse resp = svc.choose("s1", "A", admin("admin-1")).block();

        assertThat(resp).isNotNull();
        assertThat(resp.segmentNumber()).isEqualTo(2);
        assertThat(resp.branchingState()).isEqualTo("awaiting_choice_2");
        assertThat(resp.isFinal()).isFalse();
        assertThat(resp.choices()).hasSize(2);
    }

    @Test
    void choose_final_segment_triggers_comic_build_and_keeps_content_clean() {
        // Regression (2 bugs): completing an interactive tale must (a) trigger the comic build —
        // branching never did, so covers were stuck PENDING forever — and (b) NOT bake the chosen
        // option label into the persisted narrative.
        Story story = new Story();
        story.setId("s1"); story.setUserId("u1"); story.setChildProfileId("p1");
        story.setLanguage("uk"); story.setTheme("пригода");
        story.setContent("Opening body.\n\nMiddle body.");
        story.setBranching(true);
        story.setBranchingState("awaiting_choice_2");
        story.setPendingChoices(List.of(
                new BranchingChoice("A", "Піти ліворуч"),
                new BranchingChoice("B", "Піти праворуч")));
        when(stories.findByIdAndUserId("s1", "u1")).thenReturn(Optional.of(story));
        when(childProfiles.requireOwned("p1", "u1")).thenReturn(profile());
        when(promptBuilder.buildBranchingSystem()).thenReturn("branching system");
        when(promptBuilder.buildClosingUserMessage(anyString(), anyString(), anyString())).thenReturn("closing prompt");
        when(aiClient.generateStoryJson(anyString(), anyString()))
                .thenReturn(Mono.just("{\"segment\":\"Closing text. The tale ends happily.\"}"));
        when(stories.save(any(Story.class))).thenAnswer(i -> i.getArgument(0));
        when(comicsBuilder.build(anyString())).thenReturn(Mono.empty());

        BranchingResponse resp = svc.choose("s1", "A", user("u1")).block();

        assertThat(resp).isNotNull();
        assertThat(resp.segmentNumber()).isEqualTo(3);
        assertThat(resp.branchingState()).isEqualTo("complete");
        assertThat(resp.isFinal()).isTrue();
        assertThat(resp.choices()).isNull();
        // Clean narrative: no "chose:"/"обрал…" breadcrumb, no choice label, no CHOICE_ markers.
        assertThat(resp.content())
                .isEqualTo("Opening body.\n\nMiddle body.\n\nClosing text. The tale ends happily.")
                .doesNotContain("обрал", "chose", "Піти ліворуч", "CHOICE_");
        verify(comicsBuilder).build("s1");
        verify(extractionWorker).enqueue("s1", "p1", "u1");
    }
}
