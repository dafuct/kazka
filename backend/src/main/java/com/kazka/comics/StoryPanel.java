package com.kazka.comics;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.List;

/**
 * One stored image of a story. Today there is a single row per story —
 * the full comic PAGE ({@code aspect = PAGE}, {@code panel_index = 1}). The
 * schema does not assume that count — {@code panel_index} is just an ordering
 * column with a unique key on (story_id, panel_index).
 *
 * <p>Dialog is stored as a JSON array of {@link Dialog} records via
 * {@link DialogConverter}, matching the project's existing pattern
 * for JSON columns ({@code CharactersConverter} on {@code Story.characters}).
 */
@Entity
@Table(name = "story_panels")
@Getter
@Setter
@NoArgsConstructor
public class StoryPanel {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "story_id", length = 36, nullable = false)
    private String storyId;

    @Column(name = "panel_index", nullable = false)
    private int panelIndex;

    @Column(name = "image_path", length = 512, nullable = false)
    private String imagePath;

    @Column(name = "scene_prompt", columnDefinition = "TEXT", nullable = false)
    private String scenePrompt;

    @Column(name = "narration", columnDefinition = "TEXT", nullable = false)
    private String narration;

    @Convert(converter = DialogConverter.class)
    @Column(name = "dialog_json", columnDefinition = "JSON")
    private List<Dialog> dialog = List.of();

    @Enumerated(EnumType.STRING)
    @Column(name = "aspect", length = 16, nullable = false)
    private PanelAspect aspect;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    /** One line of dialog: who speaks ({@code speaker}) and what they say ({@code line}). */
    public record Dialog(String speaker, String line) {}
}
