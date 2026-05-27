package com.kazka.story.translation;

import com.kazka.auth.CurrentUserResolver.CurrentUser;
import com.kazka.billing.EntitlementResolver;
import com.kazka.hf.HuggingFaceClient;
import com.kazka.story.PromptBuilder;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.story.dto.StoryDto;
import com.kazka.story.exception.PaywallRequiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class TranslationService {

    private static final Logger log = LoggerFactory.getLogger(TranslationService.class);

    private final StoryRepository stories;
    private final EntitlementResolver entitlements;
    private final HuggingFaceClient hfClient;
    private final TranslationPromptBuilder promptBuilder;
    private final PromptBuilder systemPromptBuilder;

    public TranslationService(StoryRepository stories,
                              EntitlementResolver entitlements,
                              HuggingFaceClient hfClient,
                              TranslationPromptBuilder promptBuilder,
                              PromptBuilder systemPromptBuilder) {
        this.stories = stories;
        this.entitlements = entitlements;
        this.hfClient = hfClient;
        this.promptBuilder = promptBuilder;
        this.systemPromptBuilder = systemPromptBuilder;
    }

    @Transactional
    public Mono<StoryDto> translate(String storyId, String targetLanguage, CurrentUser cu) {
        Story story = stories.findById(storyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!story.getUserId().equals(cu.userId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!entitlements.isPro(cu.userId())) {
            throw new PaywallRequiredException("Translation requires a paid plan");
        }
        if (story.isBranching() && !"complete".equals(story.getBranchingState())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "story_in_progress");
        }
        if (targetLanguage.equals(story.getLanguage())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "same_language");
        }
        if (targetLanguage.equals(story.getTranslatedLanguage()) && story.getTranslatedContent() != null) {
            return Mono.just(StoryDto.from(story));
        }

        String systemPrompt = systemPromptBuilder.buildStorySystem(targetLanguage);
        String userMessage = promptBuilder.buildUserMessage(story.getLanguage(), targetLanguage, story.getContent());

        return hfClient.streamText(systemPrompt, userMessage)
                .reduce("", String::concat)
                .flatMap(translated -> Mono.fromCallable(() -> {
                    story.setTranslatedContent(translated.strip());
                    story.setTranslatedLanguage(targetLanguage);
                    stories.save(story);
                    return StoryDto.from(story);
                }).subscribeOn(Schedulers.boundedElastic()));
    }
}
