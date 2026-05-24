package com.kazka.child;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "story_characters")
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

    public Id getId() { return id; }
    public void setId(Id id) { this.id = id; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @Embeddable
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
        public String getStoryId() { return storyId; }
        public void setStoryId(String storyId) { this.storyId = storyId; }
        public String getCharacterId() { return characterId; }
        public void setCharacterId(String characterId) { this.characterId = characterId; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Id i)) return false;
            return Objects.equals(storyId, i.storyId) && Objects.equals(characterId, i.characterId);
        }
        @Override public int hashCode() { return Objects.hash(storyId, characterId); }
    }
}
