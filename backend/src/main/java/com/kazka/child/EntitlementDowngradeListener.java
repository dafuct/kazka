package com.kazka.child;

import com.kazka.billing.EntitlementDowngradedEvent;
import com.kazka.billing.EntitlementResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Component
public class EntitlementDowngradeListener {

    private static final Logger log = LoggerFactory.getLogger(EntitlementDowngradeListener.class);

    private final ChildProfileRepository profiles;
    private final EntitlementResolver entitlements;
    private final com.kazka.story.StoryRepository stories;

    public EntitlementDowngradeListener(ChildProfileRepository profiles,
                                        EntitlementResolver entitlements,
                                        com.kazka.story.StoryRepository stories) {
        this.profiles = profiles;
        this.entitlements = entitlements;
        this.stories = stories;
    }

    @EventListener
    @Transactional
    public void on(EntitlementDowngradedEvent ev) {
        if (entitlements.isPro(ev.userId())) return; // race-safe re-check
        List<ChildProfile> active = profiles.findByUserIdAndArchivedAtIsNullOrderByCreatedAtAsc(ev.userId());
        if (active.size() <= 1) return;

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

    private Instant mostRecentStoryAt(String childProfileId) {
        return stories.findFirstByChildProfileIdOrderByCreatedAtDesc(childProfileId)
                .map(com.kazka.story.Story::getCreatedAt)
                .orElse(Instant.EPOCH);
    }
}
