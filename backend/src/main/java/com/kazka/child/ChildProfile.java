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
@Table(name = "child_profiles")
@Getter
@Setter
public class ChildProfile {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "birth_year")
    private Short birthYear;

    @Column(length = 10)
    private String gender;

    @Column(name = "preferred_language", nullable = false, length = 10)
    private String preferredLanguage = "uk";

    @Convert(converter = InterestsConverter.class)
    @Column(columnDefinition = "JSON", nullable = false)
    @Setter(AccessLevel.NONE)
    private List<String> interests = List.of();

    @Column(name = "avatar_seed", nullable = false, length = 40)
    private String avatarSeed;

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

    public void setInterests(List<String> interests) { this.interests = interests == null ? List.of() : interests; }
}
