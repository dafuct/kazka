package com.kazka.child;

import com.kazka.billing.EntitlementDowngradedEvent;
import com.kazka.billing.EntitlementResolver;
import com.kazka.child.bedtime.BedtimeScheduleRepository;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class EntitlementDowngradeListener {

    private final ChildProfileRepository profiles;
    private final EntitlementResolver entitlements;
    private final StoryRepository stories;
    private final BedtimeScheduleRepository bedtimeRepo;

    @EventListener
    @Transactional
    public void on(EntitlementDowngradedEvent ev) {
        if (entitlements.isPro(ev.userId())) return; // race-safe re-check

        // Archive over-quota child profiles
        List<ChildProfile> active = profiles.findByUserIdAndArchivedAtIsNullOrderByCreatedAtAsc(ev.userId());
        if (active.size() > 1) {
            ChildProfile keep = active.stream()
                    .max(Comparator.comparing(p -> mostRecentStoryAt(p.getId())))
                    .orElse(active.get(0));

            Instant now = Instant.now();
            for (ChildProfile p : active) {
                if (!p.getId().equals(keep.getId())) {
                    p.setArchivedAt(now);
                    profiles.save(p);
                }
            }
            log.info("Entitlement downgrade: kept profile {} for user {}, archived {} others",
                    keep.getId(), ev.userId(), active.size() - 1);
        }

        // Disable all bedtime schedules
        int disabled = bedtimeRepo.disableAllForUser(ev.userId());
        if (disabled > 0) {
            log.info("Entitlement downgrade: disabled {} bedtime schedule(s) for user {}",
                    disabled, ev.userId());
        }
    }

    private Instant mostRecentStoryAt(String childProfileId) {
        return stories.findFirstByChildProfileIdOrderByCreatedAtDesc(childProfileId)
                .map(Story::getCreatedAt)
                .orElse(Instant.EPOCH);
    }
}
