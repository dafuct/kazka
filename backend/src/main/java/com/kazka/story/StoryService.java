package com.kazka.story;

import com.kazka.auth.CurrentUserResolver.CurrentUser;
import com.kazka.auth.exception.EmailNotVerifiedException;
import com.kazka.ai.AiClient;
import com.kazka.illustration.ImageUrlResolver;
import com.kazka.moderation.ModerationCategory;
import com.kazka.moderation.ModerationPipeline;
import com.kazka.moderation.ModerationProperties;
import com.kazka.moderation.ModerationResult;
import com.kazka.moderation.ModerationService;
import com.kazka.moderation.SuspensionService;
import com.kazka.story.dto.*;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class StoryService {

    private final StoryRepository repository;
    private final UserRepository users;
    private final AiClient aiClient;
    private final PromptBuilder promptBuilder;
    private final ModerationService moderationService;
    private final SuspensionService suspensionService;
    private final ModerationProperties moderationProperties;
    private final com.kazka.billing.FreeTierGate freeTier;
    private final com.kazka.child.ChildProfileService childProfiles;
    private final com.kazka.child.CharacterRepository characters;
    private final com.kazka.child.StoryCharacterRepository storyCharacters;
    private final com.kazka.child.ChildEntitlementResolver childTier;
    private final com.kazka.child.CharacterExtractionWorker extractionWorker;
    private final ImageUrlResolver images;
    private final com.kazka.comics.ComicsBuilder comicsBuilder;
    private final com.kazka.comics.StoryPanelRepository panelRepository;

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
                                        refused.category() == ModerationCategory.JUDGE_UNAVAILABLE
                                                ? SseEvent.errorCode("JUDGE_UNAVAILABLE")
                                                : SseEvent.errorCode("BLOCKED_INPUT", refused.category().name())));
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
                    Flux<SseEvent> tokens = aiClient.streamText(storySystem, storyUser)
                            .doOnNext(rawBuffer::append)
                            .map(SseEvent::token)
                            .concatWith(Mono.defer(() ->
                                aiClient.streamEdit(editorSystem, rawBuffer.toString())
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
                                            triggerComics(saved.getId());
                                            return SseEvent.done(id, title);
                                        }).subscribeOn(Schedulers.boundedElastic()))
                            ))
                            .onErrorResume(e -> deleteIfStillEmpty(id)
                                    .thenMany(Flux.just(SseEvent.error(e.getMessage()))));
                    return meta.concatWith(tokens);
                });
    }

    /**
     * If generation fails before title/content land in DB, drop the placeholder row so it
     * doesn't haunt "Архів казок" as an empty card. Only deletes when the row is still empty —
     * a downstream side-effect failing after the story was populated must NOT delete real content.
     */
    private Mono<Void> deleteIfStillEmpty(String storyId) {
        return Mono.fromRunnable(() -> repository.findById(storyId).ifPresent(s -> {
            if ((s.getTitle() == null || s.getTitle().isBlank())
                    && (s.getContent() == null || s.getContent().isBlank())) {
                repository.deleteById(storyId);
            }
        })).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private static boolean looksLikeTitle(String line) {
        if (line.length() > 60) return false;
        if (line.contains(". ") || line.contains("! ") || line.contains("? ")) return false;
        if (line.endsWith(".") || line.endsWith("!") || line.endsWith("?")) return false;
        return line.split("\\s+").length <= 6;
    }

    /** Kick off the comic build for a freshly written story. Fire-and-forget;
     *  ComicsBuilder.build no-ops unless the story is PENDING and flips the status itself. */
    private void triggerComics(String storyId) {
        comicsBuilder.build(storyId).subscribe();
    }

    public Mono<Void> illustrate(String id, CurrentUser currentUser) {
        // Fire-and-forget manual backfill: the single-page comic pipeline takes ~15 s; the
        // caller (controller) shouldn't block on it. Build runs on its own scheduler and
        // no-ops unless the story is PENDING; errors surface via the `illustration_status =
        // FAILED` flip, which the frontend's progress widget polls via GET /{id}/status.
        // NOTE: the normal flow is triggered server-side after generation; this endpoint is
        // for manual recovery only and is no longer auto-called by the client.
        return ensureVerified(currentUser)
                .then(Mono.fromCallable(() -> findOwned(id, currentUser))
                        .subscribeOn(Schedulers.boundedElastic()))
                .doOnSuccess(__ -> comicsBuilder.build(id).subscribe())
                .then();
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
            // Defense in depth: hide rows that failed mid-generation (empty content) — the
            // on-error cleanup handles the live SSE path, but a crashed worker or stale row
            // could still leave a placeholder behind. Branching tales always have content,
            // so this filter is safe for them.
            java.util.List<Story> filtered = p.getContent().stream()
                    .filter(s -> s.getContent() != null && !s.getContent().isBlank())
                    .toList();
            java.util.Map<String, java.util.List<com.kazka.comics.StoryPanel>> panelsByStory =
                    fetchPanelsByStory(filtered);
            var items = filtered.stream()
                    .map(s -> StoryDto.from(s, panelsByStory.getOrDefault(s.getId(), java.util.List.of()), images))
                    .toList();
            return new PageResponse<>(items, p.getNumber(), p.getSize(), p.getTotalElements());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<StoryDto> featured(CurrentUser currentUser) {
        return Mono.fromCallable(() ->
                repository.findByCursor(currentUser.userId(), null, null,
                                PageRequest.of(0, 1))
                        .stream().findFirst()
                        .map(s -> StoryDto.from(s, loadPanels(s.getId()), images))
                        .orElse(null))
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
            java.util.Map<String, java.util.List<com.kazka.comics.StoryPanel>> panelsByStory =
                    fetchPanelsByStory(page);
            java.util.List<StoryDto> items = page.stream()
                    .map(s -> StoryDto.from(s, panelsByStory.getOrDefault(s.getId(), java.util.List.of()), images))
                    .toList();
            String next = hasMore
                    ? new StoryCursor(
                            page.get(page.size() - 1).getCreatedAt(),
                            page.get(page.size() - 1).getId()).encode()
                    : null;
            return new CursorPageResponse<>(items, next);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<StoryDto> findById(String id, CurrentUser currentUser) {
        return Mono.fromCallable(() -> {
            Story story = findOwned(id, currentUser);
            return StoryDto.from(story, loadPanels(story.getId()), images);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<StoryDto> update(String id, UpdateStoryRequest req, CurrentUser currentUser) {
        return Mono.fromCallable(() -> {
            Story story = findOwned(id, currentUser);
            if (story.isBranching() && !"complete".equals(story.getBranchingState())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "story_in_progress");
            }
            story.setTitle(req.title());
            story.setContent(req.content());
            if (req.childProfileId() != null && !req.childProfileId().isBlank()) {
                // verify new profile is owned by this user before rebinding
                childProfiles.requireOwned(req.childProfileId(), currentUser.userId());
                story.setChildProfileId(req.childProfileId());
            }
            return repository.save(story);
        }).subscribeOn(Schedulers.boundedElastic())
          .map(s -> StoryDto.from(s, loadPanels(s.getId()), images));
    }

    public Mono<Void> delete(String id, CurrentUser currentUser) {
        return Mono.fromCallable(() -> findOwned(id, currentUser))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(story -> Mono.fromRunnable(() -> {
                    // deletePanels removes both the panel rows AND their stored image files / R2 objects.
                    // The FK cascade on story_panels.story_id would handle row removal on its own, but
                    // it can't delete blobs in object storage, so we call this explicitly first.
                    comicsBuilder.deletePanels(id);
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

    public Mono<StoryStatusDto> getStatus(String id, CurrentUser currentUser) {
        return Mono.fromCallable(() -> {
            Story story = findOwned(id, currentUser);
            long panelsReady = panelRepository.countByStoryId(id);
            StoryStatusDto.Phase phase = derivePhase(story, panelsReady);
            return new StoryStatusDto(phase, (int) panelsReady,
                    story.getTitle() == null ? "" : story.getTitle());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> retry(String id, CurrentUser currentUser) {
        return ensureVerified(currentUser)
                .then(Mono.fromCallable(() -> {
                    Story story = findOwned(id, currentUser);
                    if (story.getIllustrationStatus() == IllustrationStatus.READY) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "story already has a comic");
                    }
                    return story;
                }).subscribeOn(Schedulers.boundedElastic()))
                .doOnSuccess(story -> comicsBuilder.retry(story.getId()).subscribe())
                .then();
    }

    private static StoryStatusDto.Phase derivePhase(Story story, long panelsReady) {
        return switch (story.getIllustrationStatus()) {
            case PENDING -> {
                if (panelsReady == 0) {
                    yield (story.getContent() == null || story.getContent().isBlank())
                            ? StoryStatusDto.Phase.WRITING
                            : StoryStatusDto.Phase.EXTRACTING_ACTS;
                }
                yield StoryStatusDto.Phase.DRAWING;
            }
            case READY -> StoryStatusDto.Phase.READY;
            case FAILED -> StoryStatusDto.Phase.FAILED;
        };
    }

    private java.util.List<com.kazka.comics.StoryPanel> loadPanels(String storyId) {
        return panelRepository.findByStoryIdOrderByPanelIndexAsc(storyId);
    }

    private java.util.Map<String, java.util.List<com.kazka.comics.StoryPanel>> fetchPanelsByStory(
            java.util.List<Story> stories) {
        if (stories.isEmpty()) return java.util.Map.of();
        java.util.List<String> ids = stories.stream().map(Story::getId).toList();
        java.util.List<com.kazka.comics.StoryPanel> all =
                panelRepository.findByStoryIdInOrderByStoryIdAscPanelIndexAsc(ids);
        java.util.Map<String, java.util.List<com.kazka.comics.StoryPanel>> grouped = new java.util.LinkedHashMap<>();
        for (com.kazka.comics.StoryPanel p : all) {
            grouped.computeIfAbsent(p.getStoryId(), k -> new java.util.ArrayList<>()).add(p);
        }
        return grouped;
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
                .flatMap(saved -> aiClient.streamText(storySystem, storyUser)
                        .reduce("", String::concat)
                        .flatMap(raw -> aiClient.streamEdit(editorSystem, raw)
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
                            triggerComics(saved.getId());
                            return saved;
                        }).subscribeOn(Schedulers.boundedElastic())))
                .onErrorResume(e -> deleteIfStillEmpty(story.getId()).then(Mono.error(e)));
    }
}
