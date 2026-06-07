package com.kazka.story.branching;

import com.kazka.auth.CurrentUserResolver.CurrentUser;
import com.kazka.child.Character;
import com.kazka.child.CharacterExtractionWorker;
import com.kazka.child.CharacterRepository;
import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileService;
import com.kazka.child.ExtractionStatus;
import com.kazka.ai.AiClient;
import com.kazka.story.IllustrationStatus;
import com.kazka.story.PromptBuilder;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.story.branching.dto.BranchingChoice;
import com.kazka.story.branching.dto.BranchingResponse;
import com.kazka.story.branching.dto.BranchingStartRequest;
import com.kazka.story.dto.GenerationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class BranchingService {

    private final StoryRepository stories;
    private final ChildProfileService childProfiles;
    private final CharacterRepository characters;
    private final AiClient aiClient;
    private final BranchingPromptBuilder promptBuilder;
    private final BranchingResponseParser parser = new BranchingResponseParser();
    private final CharacterExtractionWorker extractionWorker;
    private final PromptBuilder systemPromptBuilder;

    public Mono<BranchingResponse> start(BranchingStartRequest req, CurrentUser cu) {
        ChildProfile child = childProfiles.requireOwned(req.childProfileId(), cu.userId());

        List<Character> recurringCast = resolveRecurringCast(req.includeCharacterIds(), child);
        GenerationRequest genReq = new GenerationRequest(
                req.theme(), req.characters(), req.ageGroup(), req.length(), req.language(),
                req.childProfileId(), req.includeCharacterIds());

        String storySystem = systemPromptBuilder.buildStorySystem(req.language());
        String userMessage = promptBuilder.buildOpeningUserMessage(genReq, child, recurringCast);

        return aiClient.streamText(storySystem, userMessage)
                .reduce("", String::concat)
                .map(parser::parse)
                .flatMap(parsed -> Mono.fromCallable(() -> {
                    Story story = new Story();
                    story.setId(UUID.randomUUID().toString());
                    story.setUserId(cu.userId());
                    story.setTitle("");
                    story.setTheme(req.theme());
                    story.setCharacters(req.characters());
                    story.setAgeGroup(req.ageGroup());
                    story.setLength(req.length());
                    story.setLanguage(req.language());
                    story.setContent(parsed.body());
                    story.setIllustrationStatus(IllustrationStatus.PENDING);
                    story.setChildProfileId(child.getId());
                    story.setExtractionStatus(ExtractionStatus.PENDING);
                    story.setBranching(true);
                    story.setBranchingState("awaiting_choice_1");
                    story.setPendingChoices(parsed.choices());
                    stories.save(story);
                    return new BranchingResponse(story.getId(), 1, story.getContent(),
                            story.getPendingChoices(), story.getBranchingState(), false);
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    @Transactional
    public Mono<BranchingResponse> choose(String storyId, String choiceId, CurrentUser cu) {
        return Mono.fromCallable(() -> {
            Story story = stories.findById(storyId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (!story.getUserId().equals(cu.userId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            if (!story.isBranching() || "complete".equals(story.getBranchingState())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_state");
            }
            List<BranchingChoice> pending = story.getPendingChoices();
            if (pending == null || pending.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_state");
            }
            BranchingChoice chosen = pending.stream()
                    .filter(choice -> choice.id().equals(choiceId))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_choice"));
            return new Object[]{story, chosen};
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(arr -> {
            Story story = (Story) ((Object[]) arr)[0];
            BranchingChoice chosen = (BranchingChoice) ((Object[]) arr)[1];
            ChildProfile child = childProfiles.requireOwned(story.getChildProfileId(), cu.userId());

            // Append transition line to existing content
            String contentWithTransition = story.getContent() + promptBuilder.transitionLine(child, chosen.text());

            boolean isLastSegment = "awaiting_choice_2".equals(story.getBranchingState());
            String storySystem = systemPromptBuilder.buildStorySystem(story.getLanguage());
            String userMessage = isLastSegment
                    ? promptBuilder.buildClosingUserMessage(contentWithTransition, chosen.text())
                    : promptBuilder.buildMiddleUserMessage(contentWithTransition, chosen.text());

            return aiClient.streamText(storySystem, userMessage)
                    .reduce("", String::concat)
                    .map(raw -> isLastSegment ? parser.parseFinal(raw) : parser.parse(raw))
                    .flatMap(parsed -> Mono.fromCallable(() -> {
                        story.setContent(contentWithTransition + parsed.body());
                        if (isLastSegment) {
                            story.setBranchingState("complete");
                            story.setPendingChoices(null);
                            // Derive a title from the first line if it looks like one
                            String first = story.getContent().split("\n", 2)[0].strip();
                            story.setTitle(first.length() <= 60 && !first.endsWith(".") ? first : story.getTheme());
                            stories.save(story);
                            extractionWorker.enqueue(story.getId(), story.getChildProfileId(), story.getUserId());
                            return new BranchingResponse(story.getId(), 3, story.getContent(),
                                    null, "complete", true);
                        } else {
                            story.setBranchingState("awaiting_choice_2");
                            story.setPendingChoices(parsed.choices());
                            stories.save(story);
                            return new BranchingResponse(story.getId(), 2, story.getContent(),
                                    parsed.choices(), story.getBranchingState(), false);
                        }
                    }).subscribeOn(Schedulers.boundedElastic()));
        });
    }

    private List<Character> resolveRecurringCast(List<String> includeIds, ChildProfile child) {
        if (includeIds == null || includeIds.isEmpty()) return List.of();
        return includeIds.stream()
                .limit(3)
                .map(characters::findById)
                .filter(Optional::isPresent).map(Optional::get)
                .filter(character -> character.getChildProfileId().equals(child.getId()))
                .toList();
    }
}
