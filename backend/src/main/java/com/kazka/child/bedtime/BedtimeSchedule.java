package com.kazka.child.bedtime;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "bedtime_schedules")
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
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public String getChildProfileId() { return childProfileId; }
    public void setChildProfileId(String childProfileId) { this.childProfileId = childProfileId; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getLocalTime() { return localTime; }
    public void setLocalTime(String localTime) { this.localTime = localTime; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public List<String> getThemes() { return themes; }
    public void setThemes(List<String> themes) { this.themes = themes == null ? List.of() : themes; }
    public Instant getNextRunAt() { return nextRunAt; }
    public void setNextRunAt(Instant nextRunAt) { this.nextRunAt = nextRunAt; }
    public Instant getLastSentAt() { return lastSentAt; }
    public void setLastSentAt(Instant lastSentAt) { this.lastSentAt = lastSentAt; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public Instant getFailedAt() { return failedAt; }
    public void setFailedAt(Instant failedAt) { this.failedAt = failedAt; }
    public boolean isHolidayThemesEnabled() { return holidayThemesEnabled; }
    public void setHolidayThemesEnabled(boolean holidayThemesEnabled) { this.holidayThemesEnabled = holidayThemesEnabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
