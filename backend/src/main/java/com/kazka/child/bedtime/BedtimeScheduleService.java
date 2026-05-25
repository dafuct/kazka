package com.kazka.child.bedtime;

import com.kazka.billing.EntitlementResolver;
import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileService;
import com.kazka.child.bedtime.dto.BedtimeUpdateRequest;
import com.kazka.story.exception.PaywallRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class BedtimeScheduleService {

    private final BedtimeScheduleRepository repo;
    private final ChildProfileService profiles;
    private final EntitlementResolver entitlements;
    private final NextRunCalculator nextRun;

    public BedtimeScheduleService(BedtimeScheduleRepository repo,
                                  ChildProfileService profiles,
                                  EntitlementResolver entitlements,
                                  NextRunCalculator nextRun) {
        this.repo = repo;
        this.profiles = profiles;
        this.entitlements = entitlements;
        this.nextRun = nextRun;
    }

    @Transactional(readOnly = true)
    public BedtimeSchedule getOrEmpty(String childProfileId, String userId) {
        ChildProfile p = profiles.requireOwned(childProfileId, userId);
        return repo.findByChildProfileId(p.getId()).orElseGet(() -> {
            BedtimeSchedule blank = new BedtimeSchedule();
            blank.setChildProfileId(p.getId());
            return blank;
        });
    }

    @Transactional
    public BedtimeSchedule upsert(String childProfileId, String userId, BedtimeUpdateRequest req) {
        ChildProfile p = profiles.requireOwned(childProfileId, userId);

        if (req.enabled() && !entitlements.isPro(userId)) {
            throw new PaywallRequiredException("Bedtime ritual requires a paid plan");
        }

        ZoneId tz;
        try { tz = ZoneId.of(req.timezone()); }
        catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_timezone");
        }
        LocalTime localTime = LocalTime.parse(req.localTime());

        BedtimeSchedule s = repo.findByChildProfileId(p.getId())
                .orElseGet(() -> {
                    BedtimeSchedule fresh = new BedtimeSchedule();
                    fresh.setChildProfileId(p.getId());
                    return fresh;
                });

        s.setEnabled(req.enabled());
        s.setLocalTime(req.localTime());
        s.setTimezone(req.timezone());
        s.setThemes(req.themes() == null ? List.of() : req.themes());
        s.setHolidayThemesEnabled(req.holidayThemesEnabled());
        s.setFailedAt(null);
        s.setRetryCount(0);

        if (req.enabled()) {
            s.setNextRunAt(nextRun.nextRun(localTime, tz, Instant.now()));
        } else {
            s.setNextRunAt(null);
        }
        return repo.save(s);
    }
}
