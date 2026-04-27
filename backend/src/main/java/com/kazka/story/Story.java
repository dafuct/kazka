package com.kazka.story;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "stories")
public class Story {

    @Id
    @Column(length = 36)
    private String id;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String theme;

    @Convert(converter = CharactersConverter.class)
    @Column(columnDefinition = "JSON")
    private List<String> characters;

    @Column(name = "age_group", length = 10)
    private String ageGroup;

    @Column(length = 10)
    private String length;

    @Column(length = 5)
    private String language = "uk";

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "illustration_path", length = 500)
    private String illustrationPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "illustration_status", length = 20)
    private IllustrationStatus illustrationStatus = IllustrationStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public List<String> getCharacters() { return characters; }
    public void setCharacters(List<String> characters) { this.characters = characters; }

    public String getAgeGroup() { return ageGroup; }
    public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }

    public String getLength() { return length; }
    public void setLength(String length) { this.length = length; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getIllustrationPath() { return illustrationPath; }
    public void setIllustrationPath(String illustrationPath) { this.illustrationPath = illustrationPath; }

    public IllustrationStatus getIllustrationStatus() { return illustrationStatus; }
    public void setIllustrationStatus(IllustrationStatus illustrationStatus) { this.illustrationStatus = illustrationStatus; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
