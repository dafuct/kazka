package com.kazka.child.bedtime;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "bedtime_schedules")
@Getter
@Setter
public class BedtimeSchedule {

    @Id
    @Column(name = "child_profile_id", length = 36)
    private String childProfileId;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "local_time", nullable = false, length = 5)
    private String localTime = "20:30";

    @Column(nullable = false, length = 50)
    private String timezone = "Europe/Kyiv";

    @Convert(converter = ThemesConverter.class)
    @Column(columnDefinition = "JSON", nullable = false)
    @Setter(AccessLevel.NONE)
    private List<String> themes = List.of();

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Column(name = "last_sent_at")
    private Instant lastSentAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "holiday_themes_enabled", nullable = false)
    private boolean holidayThemesEnabled = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

    public void setThemes(List<String> themes) { this.themes = themes == null ? List.of() : themes; }
}
