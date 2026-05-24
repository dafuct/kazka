package com.kazka.child;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "child_profiles")
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
    private List<String> interests = List.of();

    @Column(name = "avatar_seed", nullable = false, length = 40)
    private String avatarSeed;

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
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Short getBirthYear() { return birthYear; }
    public void setBirthYear(Short birthYear) { this.birthYear = birthYear; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }
    public List<String> getInterests() { return interests; }
    public void setInterests(List<String> interests) { this.interests = interests == null ? List.of() : interests; }
    public String getAvatarSeed() { return avatarSeed; }
    public void setAvatarSeed(String avatarSeed) { this.avatarSeed = avatarSeed; }
    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
