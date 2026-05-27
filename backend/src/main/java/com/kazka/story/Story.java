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

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

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

    @Column(name = "illustration_path_light", length = 500)
    private String illustrationPathLight;

    @Column(name = "illustration_path_dark", length = 500)
    private String illustrationPathDark;

    @Enumerated(EnumType.STRING)
    @Column(name = "illustration_status", length = 20)
    private IllustrationStatus illustrationStatus = IllustrationStatus.PENDING;

    @Column(name = "child_profile_id", length = 36)
    private String childProfileId;

    @Convert(converter = com.kazka.child.ExtractionStatusConverter.class)
    @Column(name = "extraction_status", length = 20, nullable = false)
    private com.kazka.child.ExtractionStatus extractionStatus = com.kazka.child.ExtractionStatus.PENDING;

    @Column(name = "is_branching", nullable = false)
    private boolean isBranching = false;

    @Column(name = "branching_state", nullable = false, length = 20)
    private String branchingState = "complete";

    @Convert(converter = com.kazka.story.branching.BranchingChoicesConverter.class)
    @Column(name = "pending_choices", columnDefinition = "JSON")
    private java.util.List<com.kazka.story.branching.dto.BranchingChoice> pendingChoices;

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

    public String getIllustrationPathLight() { return illustrationPathLight; }
    public void setIllustrationPathLight(String illustrationPathLight) { this.illustrationPathLight = illustrationPathLight; }

    public String getIllustrationPathDark() { return illustrationPathDark; }
    public void setIllustrationPathDark(String illustrationPathDark) { this.illustrationPathDark = illustrationPathDark; }

    public IllustrationStatus getIllustrationStatus() { return illustrationStatus; }
    public void setIllustrationStatus(IllustrationStatus illustrationStatus) { this.illustrationStatus = illustrationStatus; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public String getChildProfileId() { return childProfileId; }
    public void setChildProfileId(String childProfileId) { this.childProfileId = childProfileId; }

    public com.kazka.child.ExtractionStatus getExtractionStatus() { return extractionStatus; }
    public void setExtractionStatus(com.kazka.child.ExtractionStatus extractionStatus) { this.extractionStatus = extractionStatus; }

    public boolean isBranching() { return isBranching; }
    public void setBranching(boolean branching) { this.isBranching = branching; }

    public String getBranchingState() { return branchingState; }
    public void setBranchingState(String branchingState) { this.branchingState = branchingState; }

    public java.util.List<com.kazka.story.branching.dto.BranchingChoice> getPendingChoices() { return pendingChoices; }
    public void setPendingChoices(java.util.List<com.kazka.story.branching.dto.BranchingChoice> pendingChoices) { this.pendingChoices = pendingChoices; }
}
