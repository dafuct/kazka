package com.kazka.child;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "story_characters")
@Getter
@Setter
public class StoryCharacter {

    @EmbeddedId
    private Id id;

    @Column(nullable = false, length = 20)
    private String role;

    public StoryCharacter() {}

    public StoryCharacter(String storyId, String characterId, String role) {
        this.id = new Id(storyId, characterId);
        this.role = role;
    }

    @Embeddable
    @Getter
    @Setter
    public static class Id implements Serializable {
        @Column(name = "story_id", length = 36, nullable = false)
        private String storyId;
        @Column(name = "character_id", length = 36, nullable = false)
        private String characterId;

        public Id() {}
        public Id(String storyId, String characterId) {
            this.storyId = storyId;
            this.characterId = characterId;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Id otherId)) return false;
            return Objects.equals(storyId, otherId.storyId) && Objects.equals(characterId, otherId.characterId);
        }
        @Override public int hashCode() { return Objects.hash(storyId, characterId); }
    }
}
