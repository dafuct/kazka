package com.kazka.child;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "characters")
public class Character {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "child_profile_id", nullable = false, length = 36)
    private String childProfileId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 20)
    private String kind;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Convert(converter = TraitsConverter.class)
    @Column(columnDefinition = "JSON", nullable = false)
    private List<String> traits = List.of();

    @Column(name = "first_story_id", length = 36)
    private String firstStoryId;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "usage_count", nullable = false)
    private int usageCount = 0;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getChildProfileId() { return childProfileId; }
    public void setChildProfileId(String childProfileId) { this.childProfileId = childProfileId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getTraits() { return traits; }
    public void setTraits(List<String> traits) { this.traits = traits == null ? List.of() : traits; }
    public String getFirstStoryId() { return firstStoryId; }
    public void setFirstStoryId(String firstStoryId) { this.firstStoryId = firstStoryId; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }
    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
