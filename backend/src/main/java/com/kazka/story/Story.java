package com.kazka.story;

import com.kazka.child.ExtractionStatus;
import com.kazka.child.ExtractionStatusConverter;
import com.kazka.story.branching.BranchingChoicesConverter;
import com.kazka.story.branching.dto.BranchingChoice;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "stories")
@Getter
@Setter
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

    @Enumerated(EnumType.STRING)
    @Column(name = "illustration_status", length = 20)
    private IllustrationStatus illustrationStatus = IllustrationStatus.PENDING;

    @Column(name = "child_profile_id", length = 36)
    private String childProfileId;

    @Convert(converter = ExtractionStatusConverter.class)
    @Column(name = "extraction_status", length = 20, nullable = false)
    private ExtractionStatus extractionStatus = ExtractionStatus.PENDING;

    @Column(name = "is_branching", nullable = false)
    private boolean isBranching = false;

    @Column(name = "branching_state", nullable = false, length = 20)
    private String branchingState = "complete";

    @Convert(converter = BranchingChoicesConverter.class)
    @Column(name = "pending_choices", columnDefinition = "JSON")
    private List<BranchingChoice> pendingChoices;

    @Column(name = "translated_content", columnDefinition = "TEXT")
    private String translatedContent;

    @Column(name = "translated_language", length = 2)
    private String translatedLanguage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

}
