package com.kazka.story;

import com.kazka.auth.CurrentUserResolver.CurrentUser;
import com.kazka.auth.exception.EmailNotVerifiedException;
import com.kazka.hf.HuggingFaceClient;
import com.kazka.illustration.IllustrationService;
import com.kazka.moderation.ModerationCategory;
import com.kazka.moderation.ModerationPipeline;
import com.kazka.moderation.ModerationProperties;
import com.kazka.moderation.ModerationResult;
import com.kazka.moderation.ModerationService;
import com.kazka.moderation.SuspensionService;
import com.kazka.story.dto.*;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Service
public class StoryService {

    private final StoryRepository repository;
    private final UserRepository users;
    private final HuggingFaceClient hfClient;
    private final PromptBuilder promptBuilder;
    private final IllustrationService illustrationService;
    private final ModerationService moderationService;
    private final SuspensionService suspensionService;
    private final ModerationProperties moderationProperties;
    private final com.kazka.billing.FreeTierGate freeTier;
    private final com.kazka.child.ChildProfileService childProfiles;
    private final com.kazka.child.CharacterRepository characters;
    private final com.kazka.child.StoryCharacterRepository storyCharacters;
    private final com.kazka.child.ChildEntitlementResolver childTier;
    private final com.kazka.child.CharacterExtractionWorker extractionWorker;

    public StoryService(StoryRepository repository, UserRepository users,
                        HuggingFaceClient hfClient, PromptBuilder promptBuilder,
                        IllustrationService illustrationService,
                        ModerationService moderationService,
                        SuspensionService suspensionService,
                        ModerationProperties moderationProperties,
                        com.kazka.billing.FreeTierGate freeTier,
                        com.kazka.child.ChildProfileService childProfiles,
                        com.kazka.child.CharacterRepository characters,
                        com.kazka.child.StoryCharacterRepository storyCharacters,
                        com.kazka.child.ChildEntitlementResolver childTier,
                        com.kazka.child.CharacterExtractionWorker extractionWorker) {
        this.repository = repository;
        this.users = users;
        this.hfClient = hfClient;
        this.promptBuilder = promptBuilder;
        this.illustrationService = illustrationService;
        this.moderationService = moderationService;
        this.suspensionService = suspensionService;
        this.moderationProperties = moderationProperties;
        this.freeTier = freeTier;
        this.childProfiles = childProfiles;
        this.characters = characters;
        this.storyCharacters = storyCharacters;
        this.childTier = childTier;
        this.extractionWorker = extractionWorker;
    }

    public Flux<SseEvent> generate(GenerationRequest req, CurrentUser currentUser) {
        String userId = currentUser.userId();
        return ensureVerified(currentUser)
                .then(loadUser(currentUser))
                .doOnNext(suspensionService::assertNotSuspended)
                .doOnNext(freeTier::assertAllowed)
                .thenMany(Flux.defer(() -> moderateThenGenerate(req, userId)));
    }

    private Mono<User> loadUser(CurrentUser currentUser) {
        return Mono.fromCallable(() -> users.findById(currentUser.userId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Flux<SseEvent> moderateThenGenerate(GenerationRequest req, String userId) {
        return Mono.fromCallable(() -> moderationService.checkInput(req.language(), req.theme(), req.characters()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(result -> {
                    if (result instanceof ModerationResult.Refused refused) {
                        return Mono.fromRunnable(() -> suspensionService.recordAndMaybeSuspend(
                                        userId,
                                        ModerationPipeline.TEXT_INPUT,
                                        refused.category(),
                                        req.language(),
                                        req.theme() + " | " + String.join(", ", req.characters()),
                                        refused.confidence(),
                                        moderationProperties.getJudgeModel()))
                                .subscribeOn(Schedulers.boundedElastic())
                                .thenMany(Flux.just(
                                        SseEvent.errorCode(refused.category() ==
                                                ModerationCategory.JUDGE_UNAVAILABLE
                                                ? "JUDGE_UNAVAILABLE" : "BLOCKED_INPUT")));
                    }
                    return generateInternal(req, userId);
                });
    }

    private Flux<SseEvent> generateInternal(GenerationRequest req, String userId) {
        com.kazka.child.ChildProfile child = childProfiles.requireOwned(req.childProfileId(), userId);
        String effectiveLang = promptBuilder.resolveLanguage(child, req.language());

        java.util.List<com.kazka.child.Character> recurringCast;
        if (!childTier.canIncludeCharacters(userId)) {
            recurringCast = java.util.List.of();   // silently strip for free tier
        } else if (req.includeCharacterIds() == null || req.includeCharacterIds().isEmpty()) {
            recurringCast = java.util.List.of();
        } else {
            recurringCast = req.includeCharacterIds().stream()
                    .limit(3)
                    .map(characters::findById)
                    .filter(java.util.Optional::isPresent).map(java.util.Optional::get)
                    .filter(c -> c.getChildProfileId().equals(child.getId()))
                    .toList();
        }

        String id = UUID.randomUUID().toString();
        String storySystem = promptBuilder.buildStorySystem(effectiveLang);
        String storyUser = promptBuilder.buildStoryUserMessage(req, child, recurringCast);
        String editorSystem = promptBuilder.buildEditorSystem(effectiveLang);

        Story story = new Story();
        story.setId(id);
        story.setUserId(userId);
        story.setTitle("");
        story.setTheme(req.theme());
        story.setCharacters(req.characters());
        story.setAgeGroup(req.ageGroup());
        story.setLength(req.length());
        story.setLanguage(effectiveLang);
        story.setContent("");
        story.setIllustrationStatus(IllustrationStatus.PENDING);
        story.setChildProfileId(child.getId());
        story.setExtractionStatus(com.kazka.child.ExtractionStatus.PENDING);

        return Mono.fromCallable(() -> repository.save(story))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(saved -> {
                    Flux<SseEvent> meta = Flux.just(SseEvent.meta(id));
                    StringBuilder rawBuffer = new StringBuilder();
                    Flux<SseEvent> tokens = hfClient.streamText(storySystem, storyUser)
                            .doOnNext(rawBuffer::append)
                            .map(SseEvent::token)
                            .concatWith(Mono.defer(() ->
                                hfClient.streamEdit(editorSystem, rawBuffer.toString())
                                        .reduce("", String::concat)
                                        .flatMap(corrected -> Mono.fromCallable(() -> {
                                            String[] lines = corrected.split("\n");
                                            String title = "";
                                            int storyStart = 0;
                                            for (int i = 0; i < lines.length; i++) {
                                                String l = lines[i].strip();
                                                if (l.isEmpty()) continue;
                                                if (looksLikeTitle(l)) {
                                                    title = l;
                                                    storyStart = i + 1;
                                                } else {
                                                    title = req.theme();
                                                    storyStart = i;
                                                }
                                                break;
                                            }
                                            while (storyStart < lines.length && lines[storyStart].strip().isEmpty()) storyStart++;
                                            String body = String.join("\n", java.util.Arrays.copyOfRange(lines, storyStart, lines.length));
                                            saved.setTitle(title);
                                            saved.setContent(body);
                                            repository.save(saved);
                                            for (com.kazka.child.Character cc : recurringCast) {
                                                storyCharacters.save(new com.kazka.child.StoryCharacter(
                                                        saved.getId(), cc.getId(), "companion"));
                                                cc.setUsageCount(cc.getUsageCount() + 1);
                                                cc.setLastUsedAt(java.time.Instant.now());
                                                characters.save(cc);
                                            }
                                            extractionWorker.enqueue(saved.getId(), child.getId(), userId);
                                            freeTier.recordUsage(userId);
                                            return SseEvent.done(id, title);
                                        }).subscribeOn(Schedulers.boundedElastic()))
                            ))
                            .onErrorResume(e -> Flux.just(SseEvent.error(e.getMessage())));
                    return meta.concatWith(tokens);
                });
    }

    private static boolean looksLikeTitle(String line) {
        if (line.length() > 60) return false;
        if (line.contains(". ") || line.contains("! ") || line.contains("? ")) return false;
        if (line.endsWith(".") || line.endsWith("!") || line.endsWith("?")) return false;
        return line.split("\\s+").length <= 6;
    }

    public Mono<Void> illustrate(String id, CurrentUser currentUser) {
        return ensureVerified(currentUser)
                .then(Mono.fromCallable(() -> findOwned(id, currentUser))
                        .subscribeOn(Schedulers.boundedElastic()))
                .then(illustrationService.generateAndStore(id)
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    public Mono<PageResponse<StoryDto>> list(int page, int size, String childProfileIdFilter, CurrentUser currentUser) {
        return Mono.fromCallable(() -> {
            org.springframework.data.domain.Page<Story> p;
            if (childProfileIdFilter == null) {
                p = currentUser.isAdmin()
                        ? repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                        : repository.findAllByUserIdOrderByCreatedAtDesc(currentUser.userId(), PageRequest.of(page, size));
            } else if ("none".equalsIgnoreCase(childProfileIdFilter)) {
                // "none" is a sentinel that returns stories with no child profile attached (legacy tales)
                p = repository.findAllByUserIdAndChildProfileIdIsNullOrderByCreatedAtDesc(
                        currentUser.userId(), PageRequest.of(page, size));
            } else {
                p = repository.findAllByUserIdAndChildProfileIdOrderByCreatedAtDesc(
                        currentUser.userId(), childProfileIdFilter, PageRequest.of(page, size));
            }
            return new PageResponse<>(
                    p.getContent().stream().map(StoryDto::from).toList(),
                    p.getNumber(), p.getSize(), p.getTotalElements());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<StoryDto> featured(CurrentUser currentUser) {
        return Mono.fromCallable(() ->
                repository.findByCursor(currentUser.userId(), null, null,
                                PageRequest.of(0, 1))
                        .stream().findFirst().map(StoryDto::from).orElse(null))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<CursorPageResponse<StoryDto>> listByCursor(String cursor, int limit, CurrentUser currentUser) {
        StoryCursor c = (cursor == null || cursor.isBlank()) ? null : StoryCursor.decode(cursor);
        return Mono.fromCallable(() -> {
            java.util.List<Story> rows = repository.findByCursor(
                    currentUser.userId(),
                    c == null ? null : c.createdAt(),
                    c == null ? null : c.id(),
                    PageRequest.of(0, limit + 1));
            boolean hasMore = rows.size() > limit;
            java.util.List<Story> page = hasMore ? rows.subList(0, limit) : rows;
            java.util.List<StoryDto> items = page.stream().map(StoryDto::from).toList();
            String next = hasMore
                    ? new StoryCursor(
                            page.get(page.size() - 1).getCreatedAt(),
                            page.get(page.size() - 1).getId()).encode()
                    : null;
            return new CursorPageResponse<>(items, next);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<StoryDto> findById(String id, CurrentUser currentUser) {
        return Mono.fromCallable(() -> findOwned(id, currentUser))
                .subscribeOn(Schedulers.boundedElastic())
                .map(StoryDto::from);
    }

    public Mono<StoryDto> update(String id, UpdateStoryRequest req, CurrentUser currentUser) {
        return Mono.fromCallable(() -> {
            Story story = findOwned(id, currentUser);
            story.setTitle(req.title());
            story.setContent(req.content());
            if (req.childProfileId() != null && !req.childProfileId().isBlank()) {
                // verify new profile is owned by this user before rebinding
                childProfiles.requireOwned(req.childProfileId(), currentUser.userId());
                story.setChildProfileId(req.childProfileId());
            }
            return repository.save(story);
        }).subscribeOn(Schedulers.boundedElastic()).map(StoryDto::from);
    }

    public Mono<Void> delete(String id, CurrentUser currentUser) {
        return Mono.fromCallable(() -> findOwned(id, currentUser))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(story -> Mono.fromRunnable(() -> {
                    if (story.getIllustrationPathLight() != null || story.getIllustrationPathDark() != null) {
                        illustrationService.deleteImage(id);
                    }
                    repository.deleteById(id);
                }).subscribeOn(Schedulers.boundedElastic()))
                .then();
    }

    public Mono<Void> triggerExtraction(String storyId, CurrentUser currentUser) {
        return Mono.fromCallable(() -> {
            Story story = findOwned(storyId, currentUser);
            if (story.getExtractionStatus() == com.kazka.child.ExtractionStatus.RUNNING) {
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            }
            return story;
        }).subscribeOn(Schedulers.boundedElastic())
                .flatMap(story -> Mono.fromRunnable(() ->
                        extractionWorker.enqueueAsync(story.getId())).then());
    }

    private Story findOwned(String id, CurrentUser currentUser) {
        var opt = currentUser.isAdmin()
                ? repository.findById(id)
                : repository.findByIdAndUserId(id, currentUser.userId());
        return opt.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private Mono<Void> ensureVerified(CurrentUser currentUser) {
        return Mono.fromCallable(() -> users.findById(currentUser.userId())
                        .map(User::isEmailVerified)
                        .orElse(false))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(verified -> verified
                        ? Mono.empty()
                        : Mono.error(new EmailNotVerifiedException()));
    }

    /**
     * Generates a story for the bedtime worker. Same persistence flow as the SSE path,
     * but returns the final Story (no token streaming, no SSE events).
     *
     * Theme fallback chain:
     *   1. schedule.themes (explicit per-bedtime preference)
     *   2. child.interests (general profile-level preference)
     *   3. language-aware default ("магічна казка" / "a magical bedtime tale")
     *
     * Does NOT call freeTier.recordUsage — bedtime is paid-only and doesn't count
     * against the free-tier monthly limit.
     */
    public Mono<Story> generateForBedtime(com.kazka.child.ChildProfile child,
                                          com.kazka.child.bedtime.BedtimeSchedule schedule,
                                          User user,
                                          String themeOverride) {
        String themeText;
        if (themeOverride != null && !themeOverride.isBlank()) {
            themeText = themeOverride;
        } else {
            java.util.List<String> themes = !schedule.getThemes().isEmpty()
                    ? schedule.getThemes()
                    : (child.getInterests() == null || child.getInterests().isEmpty()
                            ? java.util.List.of()
                            : child.getInterests());
            themeText = themes.isEmpty()
                    ? ("uk".equals(child.getPreferredLanguage()) ? "магічна казка" : "a magical bedtime tale")
                    : String.join(", ", themes);
        }

        String effectiveLang = promptBuilder.resolveLanguage(child, "uk");

        com.kazka.story.dto.GenerationRequest req = new com.kazka.story.dto.GenerationRequest(
                themeText,
                java.util.List.of(child.getName()),
                "6-8",
                "short",
                effectiveLang,
                child.getId(),
                java.util.List.of()
        );

        String storySystem = promptBuilder.buildStorySystem(effectiveLang);
        String storyUser = promptBuilder.buildStoryUserMessage(req, child, java.util.List.of());
        String editorSystem = promptBuilder.buildEditorSystem(effectiveLang);

        Story story = new Story();
        story.setId(java.util.UUID.randomUUID().toString());
        story.setUserId(user.getId());
        story.setTitle("");
        story.setTheme(req.theme());
        story.setCharacters(req.characters());
        story.setAgeGroup(req.ageGroup());
        story.setLength(req.length());
        story.setLanguage(req.language());
        story.setContent("");
        story.setIllustrationStatus(IllustrationStatus.PENDING);
        story.setChildProfileId(child.getId());
        story.setExtractionStatus(com.kazka.child.ExtractionStatus.PENDING);

        return Mono.fromCallable(() -> repository.save(story))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(saved -> hfClient.streamText(storySystem, storyUser)
                        .reduce("", String::concat)
                        .flatMap(raw -> hfClient.streamEdit(editorSystem, raw)
                                .reduce("", String::concat))
                        .flatMap(corrected -> Mono.fromCallable(() -> {
                            String[] lines = corrected.split("\n");
                            int firstNonEmpty = 0;
                            while (firstNonEmpty < lines.length && lines[firstNonEmpty].strip().isEmpty()) firstNonEmpty++;
                            String title;
                            int storyStart;
                            if (firstNonEmpty < lines.length && looksLikeTitle(lines[firstNonEmpty].strip())) {
                                title = lines[firstNonEmpty].strip();
                                storyStart = firstNonEmpty + 1;
                            } else {
                                title = req.theme();
                                storyStart = firstNonEmpty;
                            }
                            while (storyStart < lines.length && lines[storyStart].strip().isEmpty()) storyStart++;
                            String body = String.join("\n",
                                    java.util.Arrays.copyOfRange(lines, storyStart, lines.length));
                            saved.setTitle(title);
                            saved.setContent(body);
                            repository.save(saved);
                            extractionWorker.enqueue(saved.getId(), child.getId(), user.getId());
                            return saved;
                        }).subscribeOn(Schedulers.boundedElastic())));
    }
}
