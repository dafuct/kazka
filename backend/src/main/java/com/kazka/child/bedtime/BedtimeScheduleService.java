package com.kazka.child.bedtime;

import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileService;
import com.kazka.child.bedtime.dto.BedtimeUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@RequiredArgsConstructor
@Service
public class BedtimeScheduleService {

    private final BedtimeScheduleRepository repo;
    private final ChildProfileService profiles;
    private final NextRunCalculator nextRun;

    @Transactional(readOnly = true)
    public BedtimeSchedule getOrEmpty(String childProfileId, String userId) {
        ChildProfile profile = profiles.requireOwned(childProfileId, userId);
        return repo.findByChildProfileId(profile.getId()).orElseGet(() -> {
            BedtimeSchedule blank = new BedtimeSchedule();
            blank.setChildProfileId(profile.getId());
            return blank;
        });
    }

    @Transactional
    public BedtimeSchedule upsert(String childProfileId, String userId, BedtimeUpdateRequest req) {
        ChildProfile profile = profiles.requireOwned(childProfileId, userId);

        ZoneId tz;
        try { tz = ZoneId.of(req.timezone()); }
        catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_timezone");
        }
        LocalTime localTime = LocalTime.parse(req.localTime());

        BedtimeSchedule bedtimeSchedule = repo.findByChildProfileId(profile.getId())
                .orElseGet(() -> {
                    BedtimeSchedule fresh = new BedtimeSchedule();
                    fresh.setChildProfileId(profile.getId());
                    return fresh;
                });

        bedtimeSchedule.setEnabled(req.enabled());
        bedtimeSchedule.setLocalTime(req.localTime());
        bedtimeSchedule.setTimezone(req.timezone());
        bedtimeSchedule.setThemes(req.themes() == null ? List.of() : req.themes());
        bedtimeSchedule.setHolidayThemesEnabled(req.holidayThemesEnabled());
        bedtimeSchedule.setFailedAt(null);
        bedtimeSchedule.setRetryCount(0);

        if (req.enabled()) {
            bedtimeSchedule.setNextRunAt(nextRun.nextRun(localTime, tz, Instant.now()));
        } else {
            bedtimeSchedule.setNextRunAt(null);
        }
        return repo.save(bedtimeSchedule);
    }
}
