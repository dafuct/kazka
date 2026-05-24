package com.kazka.moderation;

import com.kazka.AbstractIT;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FlaggedAttemptRepositoryTest extends AbstractIT {

    @Autowired FlaggedAttemptRepository repo;
    @Autowired UserRepository users;
    @Autowired com.kazka.billing.UserEntitlementRepository entitlementRepo;

    private String userId;

    @BeforeEach
    void setUp() {
        repo.deleteAll();
        entitlementRepo.deleteAll();
        users.deleteAll();
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail("flag-test@example.com");
        u.setDisplayName("Flag Tester");
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        users.save(u);
        userId = u.getId();
    }

    @Test
    void should_persistAndReadBack_when_savingAFlaggedAttempt() {
        FlaggedAttempt fa = new FlaggedAttempt();
        fa.setId(UUID.randomUUID().toString());
        fa.setUserId(userId);
        fa.setPipeline(ModerationPipeline.TEXT_INPUT);
        fa.setCategory(ModerationCategory.SEXUAL);
        fa.setLanguage("uk");
        fa.setPromptText("оголена принцеса");
        fa.setConfidence(new BigDecimal("0.987"));
        fa.setJudgeModel("Qwen/Qwen3-32B");
        repo.save(fa);

        FlaggedAttempt loaded = repo.findById(fa.getId()).orElseThrow();
        assertThat(loaded.getUserId()).isEqualTo(userId);
        assertThat(loaded.getPipeline()).isEqualTo(ModerationPipeline.TEXT_INPUT);
        assertThat(loaded.getCategory()).isEqualTo(ModerationCategory.SEXUAL);
        assertThat(loaded.getLanguage()).isEqualTo("uk");
        assertThat(loaded.getPromptText()).isEqualTo("оголена принцеса");
        assertThat(loaded.getConfidence()).isEqualByComparingTo("0.987");
        assertThat(loaded.getJudgeModel()).isEqualTo("Qwen/Qwen3-32B");
        assertThat(loaded.getCreatedAt()).isNotNull();
    }

    @Test
    void should_countFlagsInWindow_when_userHasMultipleAttempts() {
        Instant now = Instant.now();
        save(userId, ModerationPipeline.TEXT_INPUT, ModerationCategory.SEXUAL, now.minusSeconds(60));
        save(userId, ModerationPipeline.TEXT_INPUT, ModerationCategory.VIOLENCE, now.minusSeconds(120));
        save(userId, ModerationPipeline.TEXT_INPUT, ModerationCategory.JUDGE_UNAVAILABLE, now.minusSeconds(180));   // excluded by category
        save(userId, ModerationPipeline.IMAGE_SCENE, ModerationCategory.VIOLENCE, now.minusSeconds(90));             // excluded by pipeline
        save(userId, ModerationPipeline.TEXT_INPUT, ModerationCategory.WAR, now.minusSeconds(60 * 60 * 25));         // excluded by window

        Instant since = now.minusSeconds(60 * 60 * 24);
        long counted = repo.countCountableInWindow(userId, since);
        assertThat(counted).isEqualTo(2);
    }

    @Test
    void should_findByPipelineAndUser_when_listingForAdminFilter() {
        save(userId, ModerationCategory.SEXUAL, Instant.now());
        save(userId, ModerationCategory.VIOLENCE, Instant.now().minusSeconds(10));

        List<FlaggedAttempt> found = repo.findByUserIdOrderByCreatedAtDesc(userId);
        assertThat(found).hasSize(2);
        assertThat(found.get(0).getCategory()).isEqualTo(ModerationCategory.SEXUAL);
    }

    private void save(String uid, ModerationCategory cat, Instant when) {
        save(uid, ModerationPipeline.TEXT_INPUT, cat, when);
    }

    private void save(String uid, ModerationPipeline pipeline, ModerationCategory cat, Instant when) {
        FlaggedAttempt fa = new FlaggedAttempt();
        fa.setId(UUID.randomUUID().toString());
        fa.setUserId(uid);
        fa.setPipeline(pipeline);
        fa.setCategory(cat);
        fa.setLanguage("uk");
        fa.setPromptText("test");
        fa.setCreatedAt(when);
        repo.save(fa);
    }
}
