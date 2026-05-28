package com.kazka.child;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "characters")
@Getter
@Setter
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
    @Setter(AccessLevel.NONE)
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
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

    public void setTraits(List<String> traits) { this.traits = traits == null ? List.of() : traits; }
}
