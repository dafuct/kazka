package com.kazka.child;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoryCharacterRepository extends JpaRepository<StoryCharacter, StoryCharacter.Id> {
    List<StoryCharacter> findById_StoryId(String storyId);
    List<StoryCharacter> findById_CharacterId(String characterId);
    void deleteById_StoryId(String storyId);
}
