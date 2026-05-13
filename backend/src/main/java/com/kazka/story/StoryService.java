package com.kazka.story;

import com.kazka.auth.CurrentUserResolver.CurrentUser;
import com.kazka.auth.exception.EmailNotVerifiedException;
import com.kazka.device.PushNotifier;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(StoryService.class);

    private final StoryRepository repository;
    private final UserRepository users;
    private final HuggingFaceClient hfClient;
    private final PromptBuilder promptBuilder;
    private final IllustrationService illustrationService;
    private final ModerationService moderationService;
    private final SuspensionService suspensionService;
    private final ModerationProperties moderationProperties;
    private final PushNotifier pushNotifier;

    public StoryService(StoryRepository repository, UserRepository users,
                        HuggingFaceClient hfClient, PromptBuilder promptBuilder,
                        IllustrationService illustrationService,
                        ModerationService moderationService,
                        SuspensionService suspensionService,
                        ModerationProperties moderationProperties,
                        PushNotifier pushNotifier) {
        this.repository = repository;
        this.users = users;
        this.hfClient = hfClient;
        this.promptBuilder = promptBuilder;
        this.illustrationService = illustrationService;
        this.moderationService = moderationService;
        this.suspensionService = suspensionService;
        this.moderationProperties = moderationProperties;
        this.pushNotifier = pushNotifier;
    }

    public Flux<SseEvent> generate(GenerationRequest req, CurrentUser currentUser) {
        String userId = currentUser.userId();
        return ensureVerified(currentUser)
                .then(loadUser(currentUser))
                .doOnNext(suspensionService::assertNotSuspended)
                .thenMany(Flux.defer(() -> moderateThenGenerate(req, userId)))
                .doOnNext(event -> {
                    if ("done".equals(event.type())) {
                        try {
                            // SseEvent.done builds a Map of {id, title}; cast and pull both fields.
                            if (event.data() instanceof java.util.Map<?, ?> m) {
                                Object idObj = m.get("id");
                                Object titleObj = m.get("title");
                                if (idObj != null) {
                                    pushNotifier.notifyStoryReady(
                                            userId,
                                            idObj.toString(),
                                            titleObj == null ? "" : titleObj.toString());
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Push hook failed for user={}: {}", userId, e.getMessage());
                        }
                    }
                });
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
        String id = UUID.randomUUID().toString();
        String storySystem = promptBuilder.buildStorySystem();
        String storyUser = promptBuilder.buildStoryUserMessage(req);
        String editorSystem = promptBuilder.buildEditorSystem(req.language());

        Story story = new Story();
        story.setId(id);
        story.setUserId(userId);
        story.setTitle("");
        story.setTheme(req.theme());
        story.setCharacters(req.characters());
        story.setAgeGroup(req.ageGroup());
        story.setLength(req.length());
        story.setLanguage(req.language());
        story.setContent("");
        story.setIllustrationStatus(IllustrationStatus.PENDING);

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

    public Mono<PageResponse<StoryDto>> list(int page, int size, CurrentUser currentUser) {
        return Mono.fromCallable(() -> {
            Page<Story> p = currentUser.isAdmin()
                    ? repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                    : repository.findAllByUserIdOrderByCreatedAtDesc(currentUser.userId(), PageRequest.of(page, size));
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
}
