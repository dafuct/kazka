package com.kazka.moderation;

import com.kazka.auth.AuthProperties;
import com.kazka.auth.MailService;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class SuspensionService {

    private static final Logger log = LoggerFactory.getLogger(SuspensionService.class);

    private final UserRepository users;
    private final FlaggedAttemptRepository flags;
    private final MailService mailService;
    private final AuthProperties authProps;
    private final ModerationProperties modProps;

    public SuspensionService(UserRepository users,
                             FlaggedAttemptRepository flags,
                             MailService mailService,
                             AuthProperties authProps,
                             ModerationProperties modProps) {
        this.users = users;
        this.flags = flags;
        this.mailService = mailService;
        this.authProps = authProps;
        this.modProps = modProps;
    }

    public void assertNotSuspended(User user) {
        if (user != null && user.isSuspended()) throw new AccountSuspendedException();
    }

    @Transactional
    public void recordAndMaybeSuspend(String userId,
                                      ModerationPipeline pipeline,
                                      ModerationCategory category,
                                      String language,
                                      String promptText,
                                      BigDecimal confidence,
                                      String judgeModel) {
        FlaggedAttempt fa = new FlaggedAttempt();
        fa.setId(UUID.randomUUID().toString());
        fa.setUserId(userId);
        fa.setPipeline(pipeline);
        fa.setCategory(category);
        fa.setLanguage(language);
        fa.setPromptText(promptText == null ? "" : promptText);
        fa.setConfidence(confidence);
        fa.setJudgeModel(judgeModel);
        flags.save(fa);

        // JUDGE_UNAVAILABLE never counts toward suspension
        if (category == ModerationCategory.JUDGE_UNAVAILABLE) return;
        // Image-scene refusals never count toward suspension (the user did not author the scene)
        if (pipeline == ModerationPipeline.IMAGE_SCENE) return;

        User locked = users.lockById(userId).orElse(null);
        if (locked == null || locked.isSuspended()) return;

        Instant since = Instant.now().minus(modProps.getSuspensionWindow());
        long count = flags.countCountableInWindow(userId, since);
        if (count < modProps.getSuspensionThreshold()) return;

        locked.setSuspendedAt(Instant.now());
        locked.setSuspendedReason("CONTENT_POLICY");
        locked.setSuspendedBy(null);
        users.save(locked);

        try {
            mailService.sendAccountSuspendedEmail(locked.getEmail(), locked.getDisplayName());
        } catch (Exception e) {
            log.warn("Failed to email suspended user={}: {}", locked.getId(), e.getMessage());
        }
        String adminEmail = authProps.admin() == null ? null : authProps.admin().email();
        if (adminEmail != null && !adminEmail.isBlank()) {
            try {
                mailService.sendAdminSuspensionNotice(adminEmail, locked.getEmail());
            } catch (Exception e) {
                log.warn("Failed to send admin notice for user={}: {}", locked.getId(), e.getMessage());
            }
        }
    }
}
