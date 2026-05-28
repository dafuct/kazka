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
    public static BedtimeScheduleDto from(BedtimeSchedule bedtimeSchedule) {
        return new BedtimeScheduleDto(
                bedtimeSchedule.getChildProfileId(), bedtimeSchedule.isEnabled(), bedtimeSchedule.getLocalTime(), bedtimeSchedule.getTimezone(),
                bedtimeSchedule.getThemes(), bedtimeSchedule.isHolidayThemesEnabled(),
                bedtimeSchedule.getNextRunAt(), bedtimeSchedule.getLastSentAt(), bedtimeSchedule.getFailedAt());
    }
}
