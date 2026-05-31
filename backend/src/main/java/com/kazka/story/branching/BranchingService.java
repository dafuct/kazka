package com.kazka.story.branching;

import com.kazka.auth.CurrentUserResolver.CurrentUser;
import com.kazka.billing.EntitlementResolver;
import com.kazka.child.Character;
import com.kazka.child.CharacterExtractionWorker;
import com.kazka.child.CharacterRepository;
import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileService;
import com.kazka.ai.AiClient;
import com.kazka.story.Story;
import com.kazka.story.IllustrationStatus;
import com.kazka.story.StoryRepository;
import com.kazka.story.branching.dto.BranchingChoice;
import com.kazka.story.branching.dto.BranchingResponse;
import com.kazka.story.branching.dto.BranchingStartRequest;
import com.kazka.story.dto.GenerationRequest;
import com.kazka.story.exception.PaywallRequiredException;
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
    private final EntitlementResolver entitlements;
    private final AiClient aiClient;
    private final BranchingPromptBuilder promptBuilder;
    private final BranchingResponseParser parser = new BranchingResponseParser();
    private final CharacterExtractionWorker extractionWorker;
    private final com.kazka.story.PromptBuilder systemPromptBuilder;

    public Mono<BranchingResponse> start(BranchingStartRequest req, CurrentUser cu) {
        ChildProfile child = childProfiles.requireOwned(req.childProfileId(), cu.userId());
        if (!entitlements.isPro(cu.userId())) {
            throw new PaywallRequiredException("Branching tales require a paid plan");
        }

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
                    Story s = new Story();
                    s.setId(UUID.randomUUID().toString());
                    s.setUserId(cu.userId());
                    s.setTitle("");
                    s.setTheme(req.theme());
                    s.setCharacters(req.characters());
                    s.setAgeGroup(req.ageGroup());
                    s.setLength(req.length());
                    s.setLanguage(req.language());
                    s.setContent(parsed.body());
                    s.setIllustrationStatus(IllustrationStatus.PENDING);
                    s.setChildProfileId(child.getId());
                    s.setExtractionStatus(com.kazka.child.ExtractionStatus.PENDING);
                    s.setBranching(true);
                    s.setBranchingState("awaiting_choice_1");
                    s.setPendingChoices(parsed.choices());
                    stories.save(s);
                    return new BranchingResponse(s.getId(), 1, s.getContent(),
                            s.getPendingChoices(), s.getBranchingState(), false);
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    @Transactional
    public Mono<BranchingResponse> choose(String storyId, String choiceId, CurrentUser cu) {
        return Mono.fromCallable(() -> {
            Story s = stories.findById(storyId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (!s.getUserId().equals(cu.userId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            if (!s.isBranching() || "complete".equals(s.getBranchingState())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_state");
            }
            List<BranchingChoice> pending = s.getPendingChoices();
            if (pending == null || pending.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_state");
            }
            BranchingChoice chosen = pending.stream()
                    .filter(c -> c.id().equals(choiceId))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_choice"));
            return new Object[]{s, chosen};
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(arr -> {
            Story s = (Story) ((Object[]) arr)[0];
            BranchingChoice chosen = (BranchingChoice) ((Object[]) arr)[1];
            ChildProfile child = childProfiles.requireOwned(s.getChildProfileId(), cu.userId());

            // Append transition line to existing content
            String contentWithTransition = s.getContent() + promptBuilder.transitionLine(child, chosen.text());

            boolean isLastSegment = "awaiting_choice_2".equals(s.getBranchingState());
            String storySystem = systemPromptBuilder.buildStorySystem(s.getLanguage());
            String userMessage = isLastSegment
                    ? promptBuilder.buildClosingUserMessage(contentWithTransition, chosen.text())
                    : promptBuilder.buildMiddleUserMessage(contentWithTransition, chosen.text());

            return aiClient.streamText(storySystem, userMessage)
                    .reduce("", String::concat)
                    .map(raw -> isLastSegment ? parser.parseFinal(raw) : parser.parse(raw))
                    .flatMap(parsed -> Mono.fromCallable(() -> {
                        s.setContent(contentWithTransition + parsed.body());
                        if (isLastSegment) {
                            s.setBranchingState("complete");
                            s.setPendingChoices(null);
                            // Derive a title from the first line if it looks like one
                            String first = s.getContent().split("\n", 2)[0].strip();
                            s.setTitle(first.length() <= 60 && !first.endsWith(".") ? first : s.getTheme());
                            stories.save(s);
                            extractionWorker.enqueue(s.getId(), s.getChildProfileId(), s.getUserId());
                            return new BranchingResponse(s.getId(), 3, s.getContent(),
                                    null, "complete", true);
                        } else {
                            s.setBranchingState("awaiting_choice_2");
                            s.setPendingChoices(parsed.choices());
                            stories.save(s);
                            return new BranchingResponse(s.getId(), 2, s.getContent(),
                                    parsed.choices(), s.getBranchingState(), false);
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
                .filter(c -> c.getChildProfileId().equals(child.getId()))
                .toList();
    }
}
