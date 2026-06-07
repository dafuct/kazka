package com.kazka.child;

import com.kazka.child.dto.ExtractedCandidateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@RequiredArgsConstructor
@Service
public class CharacterService {

    private final CharacterRepository repo;
    private final ChildProfileService profiles;
    private final StoryCharacterRepository joinRepo;

    @Transactional(readOnly = true)
    public List<Character> listForProfile(String childProfileId, String userId) {
        profiles.requireOwned(childProfileId, userId);
        return repo.findByChildProfileIdAndArchivedAtIsNullOrderByLastUsedAtDescCreatedAtAsc(childProfileId);
    }

    @Transactional
    public List<Character> upsertConfirmed(String childProfileId, String userId,
                                           String storyId, List<ExtractedCandidateDto> candidates) {
        profiles.requireOwned(childProfileId, userId);
        List<Character> result = new ArrayList<>(candidates.size());
        Instant now = Instant.now();
        for (ExtractedCandidateDto cand : candidates) {
            Character character = repo.findByChildProfileIdAndName(childProfileId, cand.name())
                    .orElseGet(() -> newCharacter(childProfileId, cand, storyId));
            if (character.getDescription() == null || character.getDescription().isBlank()) {
                character.setDescription(safeDesc(cand.description()));
            }
            character.setTraits(unionTraits(character.getTraits(), cand.traits()));
            character.setUsageCount(character.getUsageCount() + 1);
            character.setLastUsedAt(now);
            result.add(repo.save(character));
            joinRepo.save(new StoryCharacter(storyId, character.getId(),
                    cand.role() == null ? "companion" : cand.role()));
        }
        return result;
    }

    @Transactional
    public void archive(String characterId, String userId) {
        Character character = repo.findById(characterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        profiles.requireOwned(character.getChildProfileId(), userId);
        character.setArchivedAt(Instant.now());
        repo.save(character);
    }

    @Transactional(readOnly = true)
    public Character requireOwned(String characterId, String userId) {
        Character character = repo.findById(characterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        profiles.requireOwned(character.getChildProfileId(), userId);
        return character;
    }

    @Transactional
    public Character updateOwned(String characterId, String userId,
                                                  String name, String kind,
                                                  String description, List<String> traits) {
        Character character = repo.findById(characterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        profiles.requireOwned(character.getChildProfileId(), userId);
        character.setName(name.trim());
        if (kind != null) character.setKind(kind);
        character.setDescription(description);
        if (traits != null) character.setTraits(traits);
        character.setLastUsedAt(Instant.now());
        return repo.save(character);
    }

    private Character newCharacter(String childProfileId, ExtractedCandidateDto cand, String storyId) {
        Character character = new Character();
        character.setId(UUID.randomUUID().toString());
        character.setChildProfileId(childProfileId);
        character.setName(cand.name().trim());
        character.setKind(cand.kind() == null ? "object" : cand.kind());
        character.setDescription(safeDesc(cand.description()));
        character.setTraits(cand.traits() == null ? List.of() : cand.traits());
        character.setFirstStoryId(storyId);
        return character;
    }

    private String safeDesc(String description) {
        if (description == null) return "";
        return description.length() > 280 ? description.substring(0, 280) : description;
    }

    private List<String> unionTraits(List<String> existing, List<String> incoming) {
        if (incoming == null || incoming.isEmpty()) return existing == null ? List.of() : existing;
        LinkedHashSet<String> merged = new LinkedHashSet<>(existing == null ? List.of() : existing);
        for (String t : incoming) {
            if (merged.size() >= 8) break;
            if (t != null && !t.isBlank()) merged.add(t);
        }
        return new ArrayList<>(merged);
    }
}
