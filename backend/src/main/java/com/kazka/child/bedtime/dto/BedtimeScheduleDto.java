package com.kazka.child.bedtime.dto;

import com.kazka.child.bedtime.BedtimeSchedule;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record BedtimeScheduleDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String childProfileId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String localTime,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String timezone,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> themes,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean holidayThemesEnabled,
        @Schema(nullable = true) Instant nextRunAt,
        @Schema(nullable = true) Instant lastSentAt,
        @Schema(nullable = true) Instant failedAt
) {
    public static BedtimeScheduleDto from(BedtimeSchedule s) {
        return new BedtimeScheduleDto(
                s.getChildProfileId(), s.isEnabled(), s.getLocalTime(), s.getTimezone(),
                s.getThemes(), s.isHolidayThemesEnabled(),
                s.getNextRunAt(), s.getLastSentAt(), s.getFailedAt());
    }
}
